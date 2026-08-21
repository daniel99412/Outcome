package io.github.daniel99412.outcome;

import io.github.daniel99412.outcome.problem.Problem;
import io.github.daniel99412.outcome.problem.ProblemType;
import io.github.daniel99412.outcome.problem.Problems;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SequenceComprehensiveTest {

    private static Problem err(String code) {
        return new Problem(code, "desc " + code, ProblemType.INTERNAL, Map.of(), null);
    }

    private static <T> Failure<T> failure(String... codes) {
        List<Problem> errs = Arrays.stream(codes).map(SequenceComprehensiveTest::err).toList();
        return new Failure<>(new Problems(errs));
    }

    @Nested
    @DisplayName("Basic sequence behavior")
    class Basic {

        @Test
        void emptyIterableReturnsSuccessWithEmptyList() {
            Outcome<List<String>> r = Outcome.sequence(List.of());
            assertInstanceOf(Success.class, r);
            assertEquals(List.of(), ((Success<List<String>>) r).value());
            assertTrue(((Success<List<String>>) r).value().isEmpty());
        }

        @Test
        void emptyIterableReturnsImmutableEmptyList() {
            Outcome<List<String>> r = Outcome.sequence(Collections.emptyList());
            List<String> v = ((Success<List<String>>) r).value();
            assertThrows(UnsupportedOperationException.class, () -> v.add("x"));
        }

        @Test
        void singleSuccessReturnsSuccessWithSingleValue() {
            Outcome<List<Integer>> r = Outcome.sequence(List.of(new Success<>(42)));
            assertEquals(List.of(42), ((Success<List<Integer>>) r).value());
        }

        @Test
        void singleFailureReturnsFailureWithItsProblems() {
            Outcome<List<String>> r = Outcome.sequence(List.of(failure("E1")));
            assertInstanceOf(Failure.class, r);
            assertEquals(List.of("E1"), ((Failure<List<String>>) r).problems().all().stream().map(Problem::code).toList());
        }

        @Test
        void allSuccessesReturnsSuccessInEncounterOrder() {
            Outcome<List<String>> r = Outcome.sequence(List.of(
                    new Success<>("C"), new Success<>("A"), new Success<>("B")));
            assertEquals(List.of("C", "A", "B"), ((Success<List<String>>) r).value());
        }

        @Test
        void allFailuresAccumulatesAllProblemsInEncounterOrder() {
            Outcome<List<String>> r = Outcome.sequence(List.of(
                    failure("E1"), failure("E2"), failure("E3")));
            assertTrue(r.isFailure());
            assertEquals(List.of("E1", "E2", "E3"),
                    ((Failure<List<String>>) r).problems().all().stream().map(Problem::code).toList());
        }

        @Test
        void mixedSuccessesAndFailuresAccumulatesOnlyFailuresInOrder() {
            Outcome<List<String>> r = Outcome.sequence(List.of(
                    new Success<>("ok1"),
                    failure("E1", "E2"),
                    new Success<>("ok2"),
                    failure("E3"),
                    new Success<>("ok3")));
            assertTrue(r.isFailure());
            assertEquals(List.of("E1", "E2", "E3"),
                    ((Failure<List<String>>) r).problems().all().stream().map(Problem::code).toList());
        }

        @Test
        void orderMatchesEncounterOrderWithInterleavedFailures() {
            Outcome<List<Integer>> r = Outcome.sequence(List.of(
                    failure("A"),
                    new Success<>(1),
                    failure("B", "C"),
                    new Success<>(2),
                    failure("D")));
            assertEquals(List.of("A", "B", "C", "D"),
                    ((Failure<List<Integer>>) r).problems().all().stream().map(Problem::code).toList());
        }

        @Test
        void doesNotShortCircuitCollectsAllFailures() {
            Outcome<List<String>> r = Outcome.sequence(List.of(
                    failure("E1"), failure("E2"), failure("E3"), failure("E4")));
            assertEquals(4, ((Failure<List<String>>) r).problems().size());
        }

        @Test
        void successValuesAreCopiedIntoImmutableList() {
            List<Outcome<String>> input = new ArrayList<>(List.of(new Success<>("a"), new Success<>("b")));
            Outcome<List<String>> r = Outcome.sequence(input);
            List<String> values = ((Success<List<String>>) r).value();
            assertThrows(UnsupportedOperationException.class, () -> values.add("c"));
            // mutating input after call does not affect result
            input.add(new Success<>("c"));
            assertEquals(2, values.size());
        }

        @Test
        void worksWithSetIterable() {
            // Use LinkedHashSet to have deterministic order for test, but sequence should work with any Iterable
            Set<Outcome<String>> set = Set.of(new Success<>("a"), new Success<>("b"), new Success<>("c"));
            Outcome<List<String>> r = Outcome.sequence(set);
            assertTrue(r.isSuccess());
            // Set order is not guaranteed for HashSet; we just check size and containment
            List<String> vals = ((Success<List<String>>) r).value();
            assertEquals(3, vals.size());
            assertTrue(vals.containsAll(List.of("a", "b", "c")));
        }

        @Test
        void worksWithCustomIterable() {
            Iterable<Outcome<Integer>> iterable = () -> Arrays.<Outcome<Integer>>asList(new Success<>(1), new Success<>(2), SequenceComprehensiveTest.<Integer>failure("E1")).iterator();
            Outcome<List<Integer>> r = Outcome.sequence(iterable);
            assertTrue(r.isFailure());
            assertEquals(1, ((Failure<List<Integer>>) r).problems().size());
        }

        @Test
        void failureWithMultipleProblemsPerFailurePreservesInnerOrder() {
            Failure<String> f1 = new Failure<>(new Problems(List.of(err("A1"), err("A2"))));
            Failure<String> f2 = new Failure<>(new Problems(List.of(err("B1"))));
            Outcome<List<String>> r = Outcome.sequence(List.of(
                    new Success<>("s"), f1, f2));
            assertEquals(List.of("A1", "A2", "B1"),
                    ((Failure<List<String>>) r).problems().all().stream().map(Problem::code).toList());
        }

        @Test
        void largeSequence100ElementsAllSuccess() {
            List<Outcome<Integer>> many = new ArrayList<>();
            for (int i = 0; i < 100; i++) many.add(new Success<>(i));
            Outcome<List<Integer>> r = Outcome.sequence(many);
            assertTrue(r.isSuccess());
            List<Integer> vals = ((Success<List<Integer>>) r).value();
            assertEquals(100, vals.size());
            for (int i = 0; i < 100; i++) assertEquals(i, vals.get(i));
        }

        @Test
        void largeSequence100WithEverySecondFailureAccumulates50() {
            List<Outcome<Integer>> many = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                if (i % 2 == 0) many.add(new Success<>(i));
                else many.add(failure("E" + i));
            }
            Outcome<List<Integer>> r = Outcome.sequence(many);
            assertTrue(r.isFailure());
            // 50 failures each with one problem => 50 problems
            assertEquals(50, ((Failure<List<Integer>>) r).problems().size());
        }
    }

    @Nested
    @DisplayName("Null handling")
    class NullHandling {

        @Test
        void nullIterableThrowsNPE() {
            assertThrows(NullPointerException.class,
                    () -> Outcome.sequence((Iterable<Outcome<String>>) null));
            assertThrows(NullPointerException.class,
                    () -> Outcome.sequence((Outcome<String>[]) null));
        }

        @Test
        void nullElementThrowsNPE() {
            List<Outcome<String>> list = new ArrayList<>();
            list.add(new Success<>("a"));
            list.add(null);
            list.add(new Success<>("b"));
            assertThrows(NullPointerException.class, () -> Outcome.sequence(list));
        }

        @Test
        void nullFirstElementThrowsNPE() {
            List<Outcome<String>> list = Arrays.asList(null, new Success<>("a"));
            assertThrows(NullPointerException.class, () -> Outcome.sequence(list));
        }

        @Test
        void allNullElementsThrows() {
            List<Outcome<String>> list = Arrays.asList(null, null);
            assertThrows(NullPointerException.class, () -> Outcome.sequence(list));
        }
    }

    @Nested
    @DisplayName("Integration: sequence + map + recover")
    class Integration {

        @Test
        void sequenceThenMapTransformsSuccessValues() {
            Outcome<List<Integer>> seq = Outcome.sequence(List.of(new Success<>(1), new Success<>(2), new Success<>(3)));
            Outcome<List<Integer>> mapped = seq.map(list -> list.stream().map(v -> v * 10).toList());
            assertEquals(List.of(10, 20, 30), ((Success<List<Integer>>) mapped).value());
        }

        @Test
        void sequenceFailureThenMapIsShortCircuited() {
            Outcome<List<String>> seq = Outcome.sequence(List.of(new Success<>("a"), failure("E1")));
            Outcome<List<String>> mapped = seq.map(list -> List.of("should not be called"));
            assertTrue(mapped.isFailure());
            assertEquals("E1", ((Failure<List<String>>) mapped).problems().first().code());
        }

        @Test
        void sequenceFailureThenRecoverProducesSuccess() {
            Outcome<List<String>> seq = Outcome.sequence(List.of(failure("E1"), failure("E2")));
            Outcome<List<String>> recovered = seq.recover(problems -> List.of("fallback"));
            assertEquals(List.of("fallback"), ((Success<List<String>>) recovered).value());
        }

        @Test
        void sequenceSuccessThenRecoverDoesNotChange() {
            Outcome<List<String>> seq = Outcome.sequence(List.of(new Success<>("a"), new Success<>("b")));
            Outcome<List<String>> recovered = seq.recover(problems -> List.of("fallback"));
            assertEquals(List.of("a", "b"), ((Success<List<String>>) recovered).value());
        }

        @Test
        void sequenceFailureThenMapProblemTransformsProblems() {
            Outcome<List<String>> seq = Outcome.sequence(List.of(failure("E1"), new Success<>("a"), failure("E2")));
            Outcome<List<String>> mapped = seq.mapProblem(e -> new Problem(e.code().toLowerCase(), e.description(), e.type(), e.metadata(), e.cause()));
            assertEquals(List.of("e1", "e2"), ((Failure<List<String>>) mapped).problems().all().stream().map(Problem::code).toList());
        }

        @Test
        void sequenceThenFlatMapChains() {
            Outcome<List<Integer>> seq = Outcome.sequence(List.of(new Success<>(1), new Success<>(2)));
            Outcome<Integer> flatMapped = seq.flatMap(list -> new Success<>(list.stream().mapToInt(Integer::intValue).sum()));
            assertEquals(new Success<>(3), flatMapped);
        }

        @Test
        void sequenceFailureThenFlatMapShortCircuits() {
            Outcome<List<Integer>> seq = Outcome.sequence(List.of(failure("E1")));
            Outcome<Integer> flatMapped = seq.flatMap(list -> new Success<>(999));
            assertTrue(flatMapped.isFailure());
        }

        @Test
        void sequenceThenFold() {
            Outcome<List<String>> successSeq = Outcome.sequence(List.of(new Success<>("a"), new Success<>("b")));
            String foldedSuccess = successSeq.fold(
                    list -> String.join(",", list),
                    problems -> "failed:" + problems.size());
            assertEquals("a,b", foldedSuccess);

            Outcome<List<String>> failSeq = Outcome.sequence(List.of(failure("E1")));
            String foldedFail = failSeq.fold(
                    list -> String.join(",", list),
                    problems -> "failed:" + problems.first().code());
            assertEquals("failed:E1", foldedFail);
        }
    }
}
