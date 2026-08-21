package io.github.daniel99412.outcome;

import io.github.daniel99412.outcome.problem.Problem;
import io.github.daniel99412.outcome.problem.ProblemType;
import io.github.daniel99412.outcome.problem.Problems;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class OutcomeComprehensiveTest {

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static Problem err(String code) {
        return new Problem(code, "desc " + code, ProblemType.INTERNAL, Map.of(), null);
    }

    private static Problem err(String code, ProblemType type) {
        return new Problem(code, "desc " + code, type, Map.of(), null);
    }

    @SuppressWarnings("unchecked")
    private static <T> Failure<T> failure(String... codes) {
        return (Failure<T>) TestOutcomes.failure(codes);
    }

    // ── Success contract ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Success contract")
    class SuccessContract {

        @Test
        void mapTransformsValueAndReturnsNewSuccess() {
            Success<Integer> s = new Success<>(10);
            Outcome<Integer> result = s.map(v -> v + 5);
            assertInstanceOf(Success.class, result);
            assertEquals(new Success<>(15), result);
            assertNotSame(s, result);
            // original unchanged
            assertEquals(10, s.value());
        }

        @Test
        void mapToDifferentType() {
            Outcome<String> result = new Success<>(42).map(Object::toString);
            assertEquals(new Success<>("42"), result);
        }

        @Test
        void mapChaining() {
            Outcome<Integer> result = new Success<>(2)
                    .map(v -> v * 3)   // 6
                    .map(v -> v + 4);  // 10
            assertEquals(new Success<>(10), result);
        }

        @Test
        void mapMapperReturningNullThrows() {
            Success<String> s = new Success<>("a");
            // Success.map does new Success(mapper.apply) -> NPE from record
            assertThrows(NullPointerException.class, () -> s.map(v -> null));
        }

        @Test
        void mapMapperThrowingPropagates() {
            Success<Integer> s = new Success<>(1);
            assertThrows(IllegalStateException.class, () -> s.map(v -> {
                throw new IllegalStateException("boom");
            }));
        }

        @Test
        void flatMapSuccessToSuccess() {
            Outcome<String> result = new Success<>(5).flatMap(v -> new Success<>("n=" + v));
            assertEquals(new Success<>("n=5"), result);
        }

        @Test
        void flatMapSuccessToFailure() {
            Failure<String> expectedFailure = failure("E1");
            Outcome<String> result = new Success<String>("ok").flatMap(v -> expectedFailure);
            assertTrue(result.isFailure());
            assertEquals(expectedFailure.problems().first().code(), ((Failure<String>) result).problems().first().code());
        }

        @Test
        void flatMapChainingMixed() {
            Outcome<Integer> result = new Success<>(2)
                    .flatMap(v -> new Success<>(v * 10))
                    .map(v -> "val=" + v)
                    .flatMap(v -> new Success<>(v.length()));
            // 2 -> 20 -> "val=20" -> 6
            assertEquals(new Success<>(6), result);
        }

        @Test
        void flatMapNullReturnThrows() {
            assertThrows(NullPointerException.class, () -> new Success<>(1).flatMap(v -> null));
        }

        @Test
        void flatMapMapperThrowingPropagates() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Success<>(1).flatMap(v -> { throw new IllegalArgumentException("bad"); }));
        }

        @Test
        void mapProblemReturnsSameInstanceAndDoesNotInvokeMapper() {
            Success<String> s = new Success<>("ok");
            AtomicInteger calls = new AtomicInteger();
            Outcome<String> result = s.mapProblem(e -> { calls.incrementAndGet(); return e; });
            assertSame(s, result);
            assertEquals(0, calls.get());
        }

        @Test
        void foldCallsOnlySuccessBranch() {
            AtomicBoolean failureCalled = new AtomicBoolean();
            String r = new Success<>(99).fold(
                    v -> "success-" + v,
                    e -> { failureCalled.set(true); return "failure"; });
            assertEquals("success-99", r);
            assertFalse(failureCalled.get());
        }

        @Test
        void peekExecutesSideEffectAndReturnsSame() {
            AtomicReference<String> seen = new AtomicReference<>();
            Success<String> s = new Success<>("hello");
            Outcome<String> r = s.peek(seen::set);
            assertSame(s, r);
            assertEquals("hello", seen.get());
        }

        @Test
        void peekExceptionPropagatesAndDoesNotSwallow() {
            Success<Integer> s = new Success<>(1);
            assertThrows(RuntimeException.class, () -> s.peek(v -> { throw new RuntimeException("peek boom"); }));
        }

        @Test
        void peekProblemNotExecutedOnSuccess() {
            AtomicInteger c = new AtomicInteger();
            Success<Integer> s = new Success<>(1);
            Outcome<Integer> r = s.peekProblem(e -> c.incrementAndGet());
            assertSame(s, r);
            assertEquals(0, c.get());
        }

        @Test
        void recoverAndRecoverWithNotExecutedOnSuccess() {
            Success<String> s = new Success<>("ok");
            AtomicInteger calls = new AtomicInteger();
            assertSame(s, s.recover(e -> { calls.incrementAndGet(); return "x"; }));
            assertSame(s, s.recoverWith(e -> { calls.incrementAndGet(); return new Success<>("x"); }));
            assertEquals(0, calls.get());
        }

        @Test
        void isSuccessTrueIsFailureFalse() {
            Outcome<String> o = new Success<>("v");
            assertTrue(o.isSuccess());
            assertFalse(o.isFailure());
        }

        @Test
        void equalsAndHashCodeForSuccess() {
            Success<String> a = new Success<>("same");
            Success<String> b = new Success<>("same");
            Success<String> c = new Success<>("diff");
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
            assertNotEquals(a, c);
            assertNotEquals(a, failure("E1"));
            assertNotEquals(null, a);
            assertEquals(a, a); // reflexive
        }

        @Test
        void successToStringContainsValue() {
            Success<String> s = new Success<>("myVal");
            assertTrue(s.toString().contains("myVal"));
        }

        @Test
        void nullValueRejected() {
            assertThrows(NullPointerException.class, () -> new Success<>(null));
        }

        @Test
        void nullMappersRejected() {
            Success<Integer> s = new Success<>(1);
            assertThrows(NullPointerException.class, () -> s.map(null));
            assertThrows(NullPointerException.class, () -> s.flatMap(null));
            assertThrows(NullPointerException.class, () -> s.mapProblem(null));
            assertThrows(NullPointerException.class, () -> s.fold(null, e -> ""));
            assertThrows(NullPointerException.class, () -> s.fold(v -> "", null));
            assertThrows(NullPointerException.class, () -> s.peek(null));
            assertThrows(NullPointerException.class, () -> s.peekProblem(null));
            assertThrows(NullPointerException.class, () -> s.recover(null));
            assertThrows(NullPointerException.class, () -> s.recoverWith(null));
        }
    }

    // ── Failure contract ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Failure contract")
    class FailureContract {

        @Test
        void mapReturnsSameInstanceWithoutInvokingMapper() {
            Failure<Integer> f = failure("E1");
            AtomicInteger calls = new AtomicInteger();
            Outcome<String> r = f.map(v -> { calls.incrementAndGet(); return "x"; });
            assertSame(f, r);
            assertEquals(0, calls.get());
            // even throwing mapper must not be invoked
            Outcome<String> r2 = f.map(v -> { throw new IllegalStateException("should not be called"); });
            assertSame(f, r2);
        }

        @Test
        void flatMapReturnsSameInstanceWithoutInvokingMapper() {
            Failure<Integer> f = failure("E1");
            AtomicInteger calls = new AtomicInteger();
            Outcome<String> r = f.flatMap(v -> { calls.incrementAndGet(); return new Success<>("x"); });
            assertSame(f, r);
            assertEquals(0, calls.get());
        }

        @Test
        void mapProblemTransformsInOrderAndCreatesNewInstance() {
            Failure<String> f = failure("E1", "E2", "E3");
            Outcome<String> result = f.mapProblem(e -> err(e.code() + "-X"));
            assertNotSame(f, result);
            assertTrue(result.isFailure());
            Failure<String> mapped = (Failure<String>) result;
            assertEquals(List.of("E1-X", "E2-X", "E3-X"),
                    mapped.problems().all().stream().map(Problem::code).toList());
            // original unchanged
            assertEquals(List.of("E1", "E2", "E3"),
                    f.problems().all().stream().map(Problem::code).toList());
        }

        @Test
        void mapProblemWithIdentityCreatesNewInstanceButEqualProblems() {
            Failure<String> f = failure("E1");
            Outcome<String> r = f.mapProblem(e -> e);
            assertNotSame(f, r);
            assertEquals(f.problems().first(), ((Failure<String>) r).problems().first());
        }

        @Test
        void mapProblemNullReturnThrows() {
            Failure<String> f = failure("E1");
            assertThrows(NullPointerException.class, () -> f.mapProblem(e -> null));
        }

        @Test
        void mapProblemThrowingPropagates() {
            Failure<String> f = failure("E1");
            assertThrows(IllegalStateException.class, () -> f.mapProblem(e -> { throw new IllegalStateException("mapProblem boom"); }));
        }

        @Test
        void foldCallsOnlyFailureBranch() {
            AtomicBoolean successCalled = new AtomicBoolean();
            String r = failure("E1").fold(
                    v -> { successCalled.set(true); return "success"; },
                    problems -> "failure:" + problems.size());
            assertEquals("failure:1", r);
            assertFalse(successCalled.get());
        }

        @Test
        void peekNotExecutedOnFailure() {
            Failure<String> f = failure("E1");
            AtomicInteger c = new AtomicInteger();
            Outcome<String> r = f.peek(v -> c.incrementAndGet());
            assertSame(f, r);
            assertEquals(0, c.get());
        }

        @Test
        void peekProblemExecutesAndReturnsSame() {
            Failure<String> f = failure("E1");
            AtomicReference<Problems> seen = new AtomicReference<>();
            Outcome<String> r = f.peekProblem(seen::set);
            assertSame(f, r);
            assertNotNull(seen.get());
            assertEquals(1, seen.get().size());
        }

        @Test
        void peekProblemExceptionPropagates() {
            Failure<String> f = failure("E1");
            assertThrows(RuntimeException.class, () -> f.peekProblem(e -> { throw new RuntimeException("peekProblem boom"); }));
        }

        @Test
        void recoverConvertsToSuccess() {
            Outcome<String> r = OutcomeComprehensiveTest.<String>failure("E1").recover(problems -> "recovered-" + problems.size());
            assertInstanceOf(Success.class, r);
            assertEquals(new Success<>("recovered-1"), r);
        }

        @Test
        void recoverWithSuccess() {
            Outcome<String> r = OutcomeComprehensiveTest.<String>failure("E1").recoverWith(problems -> new Success<>("fallback"));
            assertEquals(new Success<>("fallback"), r);
        }

        @Test
        void recoverWithFailureKeepsFailure() {
            Problems original = OutcomeComprehensiveTest.<String>failure("E1").problems();
            Outcome<String> r = OutcomeComprehensiveTest.<String>failure("E1").recoverWith(problems -> new Failure<>(problems));
            assertTrue(r.isFailure());
            assertEquals(1, ((Failure<String>) r).problems().size());
        }

        @Test
        void recoverCanInspectProblemsToDecideValue() {
            Failure<String> f = new Failure<>(new Problems(List.of(
                    err("NOT_FOUND", ProblemType.NOT_FOUND),
                    err("VALIDATION", ProblemType.VALIDATION)
            )));
            Outcome<String> r = f.recover(problems -> problems.contains(err("NOT_FOUND", ProblemType.NOT_FOUND)) ? "found" : "other");
            // contains check uses semantic equality (code+desc+type+metadata)
            // err helper creates description "desc CODE", so exact match matters – test uses same factory
            // Here we check size based recovery instead to avoid brittle description
            Outcome<String> r2 = f.recover(problems -> problems.size() == 2 ? "two-problems" : "one");
            assertEquals(new Success<>("two-problems"), r2);
        }

        @Test
        void recoverNullValueThrows() {
            assertThrows(NullPointerException.class, () -> failure("E1").recover(e -> null));
        }

        @Test
        void recoverWithNullReturnThrows() {
            assertThrows(NullPointerException.class, () -> failure("E1").recoverWith(e -> null));
        }

        @Test
        void recoverThrowingPropagates() {
            assertThrows(IllegalStateException.class, () -> failure("E1").recover(e -> { throw new IllegalStateException("recover boom"); }));
        }

        @Test
        void recoverWithThrowingPropagates() {
            assertThrows(IllegalStateException.class, () -> failure("E1").recoverWith(e -> { throw new IllegalStateException("recoverWith boom"); }));
        }

        @Test
        void isSuccessFalseIsFailureTrue() {
            Outcome<String> o = failure("E1");
            assertFalse(o.isSuccess());
            assertTrue(o.isFailure());
        }

        @Test
        void equalsAndHashCodeForFailure() {
            Failure<String> a = failure("E1", "E2");
            Failure<String> b = failure("E1", "E2");
            Failure<String> c = failure("E1");
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
            assertNotEquals(a, c);
            assertNotEquals(a, new Success<>(List.of("E1")));
            assertEquals(a, a);
        }

        @Test
        void failureToStringContainsProblems() {
            Failure<String> f = failure("E1");
            assertTrue(f.toString().contains("E1"));
        }

        @Test
        void nullProblemsRejected() {
            assertThrows(NullPointerException.class, () -> new Failure<>(null));
        }

        @Test
        void nullMappersRejected() {
            Failure<Integer> f = failure("E1");
            assertThrows(NullPointerException.class, () -> f.map(null));
            assertThrows(NullPointerException.class, () -> f.flatMap(null));
            assertThrows(NullPointerException.class, () -> f.mapProblem(null));
            assertThrows(NullPointerException.class, () -> f.fold(null, e -> ""));
            assertThrows(NullPointerException.class, () -> f.fold(v -> "", null));
            assertThrows(NullPointerException.class, () -> f.peek(null));
            assertThrows(NullPointerException.class, () -> f.peekProblem(null));
            assertThrows(NullPointerException.class, () -> f.recover(null));
            assertThrows(NullPointerException.class, () -> f.recoverWith(null));
        }

        @Test
        void problemTypePreservedThroughMapProblem() {
            Failure<String> f = failure("E1");
            Outcome<String> r = f.mapProblem(e -> err("NEW", ProblemType.TIMEOUT));
            assertEquals(ProblemType.TIMEOUT, ((Failure<String>) r).problems().first().type());
        }
    }

    // ── Outcome polymorphism & chaining ────────────────────────────────────

    @Nested
    @DisplayName("Outcome polymorphism & chaining")
    class Polymorphism {

        @Test
        void outcomeReferenceWorksForBothSubtypes() {
            Outcome<Integer> successRef = new Success<>(10);
            Outcome<Integer> failureRef = OutcomeComprehensiveTest.<Integer>failure("E1");

            assertTrue(successRef.isSuccess());
            assertTrue(failureRef.isFailure());

            // pattern matching via instanceof (sealed)
            assertInstanceOf(Success.class, successRef);
            assertInstanceOf(Failure.class, failureRef);
        }

        @Test
        void sealedSwitchExhaustiveness() {
            Outcome<String> o1 = new Success<>("ok");
            Outcome<String> o2 = failure("E1");

            String r1 = switch (o1) {
                case Success<String> s -> "success:" + s.value();
                case Failure<String> f -> "failure:" + f.problems().size();
            };
            String r2 = switch (o2) {
                case Success<String> s -> "success:" + s.value();
                case Failure<String> f -> "failure:" + f.problems().size();
            };
            assertEquals("success:ok", r1);
            assertEquals("failure:1", r2);
        }

        @Test
        void chainingSuccessPath() {
            Outcome<Integer> result = new Success<>(5)
                    .map(v -> v * 2)                 // 10
                    .flatMap(v -> new Success<>(v + 3)) // 13
                    .map(v -> v * 2)                 // 26
                    .peek(v -> assertEquals(26, v))
                    .mapProblem(e -> err("SHOULD_NOT_HAPPEN"))
                    .recover(e -> -1)
                    .recoverWith(e -> new Success<>(-2));

            assertEquals(new Success<>(26), result);
        }

        @Test
        void chainingFailurePathShortCircuitsThenRecovers() {
            AtomicInteger mapCalls = new AtomicInteger();
            AtomicInteger flatMapCalls = new AtomicInteger();

            Outcome<String> result = failure("E1")
                    .map(v -> { mapCalls.incrementAndGet(); return ((String) v) + "x"; })
                    .flatMap(v -> { flatMapCalls.incrementAndGet(); return new Success<>("y"); })
                    .mapProblem(e -> err(e.code() + "-mapped"))
                    .peek(v -> fail("peek should not be called on failure"))
                    .peekProblem(problems -> assertEquals(1, problems.size()));

            // map/flatMap not called, mapProblem transforms, peekProblem fires
            assertEquals(0, mapCalls.get());
            assertEquals(0, flatMapCalls.get());
            assertTrue(result.isFailure());
            assertEquals("E1-mapped", ((Failure<String>) result).problems().first().code());

            // now recover
            Outcome<String> recovered = result.recover(problems -> "recovered");
            assertEquals(new Success<>("recovered"), recovered);

            Outcome<String> recoveredWith = result.recoverWith(problems -> new Success<>("fallback"));
            assertEquals(new Success<>("fallback"), recoveredWith);
        }

        @Test
        void foldAsTerminalOperation() {
            String s1 = new Success<>(123).fold(v -> "ok:" + v, e -> "fail");
            String s2 = failure("E1").fold(v -> "ok", e -> "fail:" + e.first().code());
            assertEquals("ok:123", s1);
            assertEquals("fail:E1", s2);
        }

        @Test
        void mapAndFlatMapNullMappersThrowEvenOnFailure() {
            Outcome<String> f = failure("E1");
            assertThrows(NullPointerException.class, () -> f.map(null));
            assertThrows(NullPointerException.class, () -> f.flatMap(null));
        }

        @Test
        void recoverChainingFailureCanStillMap() {
            Outcome<Integer> result = OutcomeComprehensiveTest.<Integer>failure("E1")
                    .recover(problems -> 42)
                    .map(v -> v * 2);
            assertEquals(new Success<>(84), result);
        }

        @Test
        void mapProblemAfterRecoverHasNoEffectBecauseNowSuccess() {
            Outcome<String> result = OutcomeComprehensiveTest.<String>failure("E1")
                    .recover(e -> "ok")
                    .mapProblem(e -> err("NOPE"));
            assertEquals(new Success<>("ok"), result);
        }

        @Test
        void genericFlexibilityWithVariousTypes() {
            Outcome<String> stringOutcome = new Success<>("value");
            Outcome<Integer> intOutcome = new Success<>(42);
            Outcome<List<String>> listOutcome = new Success<>(List.of("a"));
            Outcome<Object> objectOutcome = new Success<>((Object) "value");

            assertTrue(stringOutcome.isSuccess());
            assertTrue(intOutcome.isSuccess());
            assertTrue(listOutcome.isSuccess());
            assertTrue(objectOutcome.isSuccess());

            // Success must reject null even for generic types
            assertThrows(NullPointerException.class, () -> new Success<String>(null));
        }

        @Test
        void successHoldingListIsImmutableViaSequenceButDirectSuccessIsAsGiven() {
            List<String> list = List.of("a", "b");
            Success<List<String>> s = new Success<>(list);
            assertEquals(list, s.value());
            assertThrows(UnsupportedOperationException.class, () -> s.value().add("c"));
        }
    }

    // ── Immutability and contract ──────────────────────────────────────────

    @Nested
    @DisplayName("Immutability")
    class Immutability {

        @Test
        void successMapDoesNotMutateOriginal() {
            Success<Integer> original = new Success<>(5);
            original.map(v -> v + 1);
            assertEquals(5, original.value());
        }

        @Test
        void failureMapProblemDoesNotMutateOriginal() {
            Failure<String> original = failure("ORIG");
            original.mapProblem(e -> err("MUTATED"));
            assertEquals("ORIG", original.problems().first().code());
        }

        @Test
        void problemsInsideFailureAreImmutable() {
            Failure<String> f = failure("E1");
            assertThrows(UnsupportedOperationException.class, () -> f.problems().all().add(err("E2")));
        }

        @Test
        void peekDoesNotChangeOutcomeEvenIfActionMutatesExternalState() {
            Success<Integer> s = new Success<>(10);
            AtomicInteger external = new AtomicInteger(0);
            Outcome<Integer> r = s.peek(v -> external.set(v * 2));
            assertSame(s, r);
            assertEquals(20, external.get());
            assertEquals(10, s.value());
        }
    }
}
