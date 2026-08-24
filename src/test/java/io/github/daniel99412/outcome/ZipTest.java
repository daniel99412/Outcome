package io.github.daniel99412.outcome;

import io.github.daniel99412.outcome.Outcome.TriFunction;
import io.github.daniel99412.outcome.problem.Problem;
import io.github.daniel99412.outcome.problem.Problems;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZipTest {

    private final AtomicBoolean combinerRan = new AtomicBoolean(false);

    private <A, B, R> BiFunction<A, B, R> neverRun() {
        return (a, b) -> {
            combinerRan.set(true);
            throw new AssertionError("combiner must not run");
        };
    }

    @Nested
    @DisplayName("zip of two")
    class TwoSources {

        @Test
        void combinesBothSuccessValues() {
            Outcome<String> result = Outcome.zip(
                    new Success<>(1),
                    new Success<>("one"),
                    (number, name) -> name + ":" + number);

            assertTrue(result.isSuccess());
            assertEquals("one:1", result.orElseThrow(problems -> new AssertionError()));
        }

        @Test
        void firstFailureSkipsCombinerAndCarriesItsProblems() {
            var failure = TestOutcomes.failure("A1", "A2");

            Outcome<String> result = Outcome.zip(failure, new Success<>("x"), neverRun());

            assertTrue(result.isFailure());
            assertFalse(combinerRan.get());
            Problems problems = problemsOf(result);
            assertEquals(List.of("A1", "A2"), codes(problems));
        }

        @Test
        void secondFailureSkipsCombinerAndCarriesItsProblems() {
            var failure = TestOutcomes.failure("B1");

            Outcome<String> result = Outcome.zip(new Success<>(7), failure, neverRun());

            assertTrue(result.isFailure());
            assertFalse(combinerRan.get());
            assertEquals(List.of("B1"), codes(problemsOf(result)));
        }

        @Test
        void bothFailuresAccumulateAllProblemsInOrder() {
            var first = TestOutcomes.failure("A1", "A2");
            var second = TestOutcomes.failure("B1");

            Outcome<String> result = Outcome.zip(first, second, neverRun());

            assertTrue(result.isFailure());
            assertFalse(combinerRan.get());
            assertEquals(List.of("A1", "A2", "B1"), codes(problemsOf(result)));
        }

        @Test
        void combinerReturningNullFailsFastWithExactMessage() {
            NullPointerException thrown = assertThrows(NullPointerException.class,
                    () -> Outcome.zip(new Success<>(1), new Success<>(2), (a, b) -> null));
            assertEquals("zip combiner cannot return null", thrown.getMessage());
        }

        @Test
        void nullFirstIsRejected() {
            assertThrows(NullPointerException.class,
                    () -> Outcome.zip(null, new Success<>("x"), (a, b) -> a));
        }

        @Test
        void nullSecondIsRejected() {
            assertThrows(NullPointerException.class,
                    () -> Outcome.zip(new Success<>(1), null, (a, b) -> a));
        }

        @Test
        void nullCombinerIsRejected() {
            assertThrows(NullPointerException.class,
                    () -> Outcome.zip(new Success<>(1), new Success<>("x"), null));
        }
    }

    @Nested
    @DisplayName("zip of three")
    class ThreeSources {

        @Test
        void combinesAllThreeSuccessValuesPreservingTypes() {
            record Account(String user, String profile, Integer settings) {
            }

            Outcome<Account> result = Outcome.zip(
                    new Success<>("ada"),
                    new Success<>("admin"),
                    new Success<>(3),
                    Account::new);

            Account account = result.orElseThrow(problems -> new AssertionError());
            assertEquals("ada", account.user());
            assertEquals("admin", account.profile());
            assertEquals(3, account.settings());
        }

        @Test
        void allFailuresAccumulateInOrder() {
            var first = TestOutcomes.failure("A1");
            var second = TestOutcomes.failure("B1", "B2");
            var third = TestOutcomes.failure("C1");

            Outcome<String> result = Outcome.zip(first, second, third, neverThreeWay());

            assertTrue(result.isFailure());
            assertEquals(List.of("A1", "B1", "B2", "C1"), codes(problemsOf(result)));
        }

        @Test
        void mixedSuccessesAndFailuresAccumulateOnlyFailures() {
            Outcome<String> first = new Success<>("ok");
            var second = TestOutcomes.failure("B1");
            Outcome<Integer> third = new Success<>(42);

            Outcome<String> result = Outcome.zip(first, second, third, (a, b, c) -> {
                combinerRan.set(true);
                throw new AssertionError("combiner must not run");
            });

            assertTrue(result.isFailure());
            assertEquals(List.of("B1"), codes(problemsOf(result)));
        }

        @Test
        void anyNullArgumentIsRejected() {
            TriFunction<String, String, String, String> combiner = (a, b, c) -> a;
            assertThrows(NullPointerException.class,
                    () -> Outcome.zip(null, new Success<>("b"), new Success<>("c"), combiner));
            assertThrows(NullPointerException.class,
                    () -> Outcome.zip(new Success<>("a"), null, new Success<>("c"), combiner));
            assertThrows(NullPointerException.class,
                    () -> Outcome.zip(new Success<>("a"), new Success<>("b"), null, combiner));
            assertThrows(NullPointerException.class,
                    () -> Outcome.zip(new Success<>("a"), new Success<>("b"), new Success<>("c"), null));
        }

        @Test
        void threeWayCombinerReturningNullFailsFast() {
            assertThrows(NullPointerException.class,
                    () -> Outcome.zip(
                            new Success<>("a"), new Success<>("b"), new Success<>("c"),
                            (a, b, c) -> null));
        }
    }

    private TriFunction<String, String, String, String> neverThreeWay() {
        return (a, b, c) -> {
            combinerRan.set(true);
            throw new AssertionError("combiner must not run");
        };
    }

    private static Problems problemsOf(Outcome<?> outcome) {
        return outcome.fold(value -> {
            throw new AssertionError("expected failure but got: " + value);
        }, identity -> identity);
    }

    private static List<String> codes(Problems problems) {
        return problems.all().stream().map(Problem::code).toList();
    }
}
