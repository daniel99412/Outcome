package io.github.daniel99412.outcome;

import org.junit.jupiter.api.Test;

import java.util.List;

import static io.github.daniel99412.outcome.TestOutcomes.failure;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SequenceVarargsTest {

    @Test
    void emptyInvocationProducesEmptySuccess() {
        Outcome<List<String>> result = Outcome.sequence();

        assertEquals(new Success<>(List.of()), result);
    }

    @Test
    void allSuccessesCollectValuesInOrder() {
        Outcome<List<String>> result = Outcome.sequence(
                Outcome.success("A"),
                Outcome.success("B"),
                Outcome.success("C"));

        assertEquals(new Success<>(List.of("A", "B", "C")), result);
    }

    @Test
    void failuresAccumulateAllProblems() {
        Outcome<List<String>> result = Outcome.sequence(
                Outcome.success("A"),
                failure("E1", "E2"),
                failure("E3"));

        assertTrue(result.isFailure());
        assertEquals(
                TestOutcomes.failure("E1", "E2", "E3"),
                result);
    }

    @Test
    void delegatesToIterableSemantics() {
        Outcome<String> s1 = Outcome.success("A");
        Outcome<String> f1 = failure("E1");

        assertEquals(Outcome.sequence(List.of(s1, f1)), Outcome.sequence(s1, f1));
        assertEquals(Outcome.sequence(List.of(f1, s1)), Outcome.sequence(f1, s1));
    }

    @Test
    void rejectsNullArray() {
        assertThrows(NullPointerException.class,
                () -> Outcome.sequence((Outcome<String>[]) null));
    }

    @Test
    void rejectsNullElement() {
        assertThrows(NullPointerException.class,
                () -> Outcome.sequence(Outcome.success("A"), null));
    }
}
