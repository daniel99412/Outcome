package dev.daniel.outcome;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SequenceTest {

    @Test
    void emptyInputProducesEmptySuccess() {
        Outcome<List<String>> result = Outcome.sequence(List.<Outcome<String>>of());

        assertTrue(result.isSuccess());
        assertEquals(new Success<>(List.of()), result);
    }

    @Test
    void allSuccessesProduceSuccessWithValuesInOrder() {
        Outcome<List<String>> result = Outcome.sequence(List.of(
                new Success<>("A"),
                new Success<>("B"),
                new Success<>("C")));

        assertTrue(result.isSuccess());
        assertEquals(List.of("A", "B", "C"), ((Success<List<String>>) result).value());
    }

    @Test
    void singleFailureProducesFailure() {
        Outcome<List<String>> result = Outcome.sequence(List.of(
                new Success<>("A"),
                TestOutcomes.<String>failure("E1"),
                new Success<>("C")));

        assertTrue(result.isFailure());
        Failure<List<String>> failure = (Failure<List<String>>) result;
        assertEquals(List.of("E1"), failure.errors().all().stream().map(e -> e.code()).toList());
    }

    @Test
    void multipleFailuresAccumulateAllErrorsInOrder() {
        Outcome<List<String>> result = Outcome.sequence(List.of(
                new Success<>("A"),
                TestOutcomes.<String>failure("E1", "E2"),
                TestOutcomes.<String>failure("E3"),
                new Success<>("D")));

        assertTrue(result.isFailure());
        Failure<List<String>> failure = (Failure<List<String>>) result;
        assertEquals(List.of("E1", "E2", "E3"),
                failure.errors().all().stream().map(e -> e.code()).toList());
    }

    @Test
    void orderOfErrorsMatchesEncounterOrder() {
        Outcome<List<String>> result = Outcome.sequence(List.of(
                TestOutcomes.<String>failure("A", "B"),
                new Success<>("s"),
                TestOutcomes.<String>failure("C")));

        Failure<List<String>> failure = (Failure<List<String>>) result;
        assertEquals(List.of("A", "B", "C"),
                failure.errors().all().stream().map(e -> e.code()).toList());
    }

    @Test
    void doesNotShortCircuitOnFirstFailure() {
        Outcome<List<String>> result = Outcome.sequence(List.of(
                TestOutcomes.<String>failure("E1"),
                TestOutcomes.<String>failure("E2")));

        Failure<List<String>> failure = (Failure<List<String>>) result;
        assertEquals(2, failure.errors().size());
        assertEquals("E2", failure.errors().all().get(1).code());
    }

    @Test
    void nullIterableRejected() {
        assertThrows(NullPointerException.class, () -> Outcome.sequence(null));
    }

    @Test
    void nullElementRejected() {
        List<Outcome<String>> list = java.util.Arrays.asList(new Success<>("A"), null);

        assertThrows(NullPointerException.class, () -> Outcome.sequence(list));
    }
}
