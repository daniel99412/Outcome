package io.github.daniel99412.outcome;

import io.github.daniel99412.outcome.problem.Problem;
import io.github.daniel99412.outcome.problem.ProblemType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProblemTest {

    @Test
    void equalityIgnoresCause() {
        RuntimeException cause = new RuntimeException("diagnostic");
        Problem first = new Problem("CODE", "desc", ProblemType.INTERNAL, Map.of("k", "v"), cause);
        Problem second = new Problem("CODE", "desc", ProblemType.INTERNAL, Map.of("k", "v"), new RuntimeException("other"));

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void equalityConsidersCode() {
        Problem first = TestOutcomes.problem("A");
        Problem second = TestOutcomes.problem("B");

        assertNotEquals(first, second);
    }

    @Test
    void equalityConsidersDescription() {
        Problem first = new Problem("CODE", "first", ProblemType.INTERNAL, Map.of(), null);
        Problem second = new Problem("CODE", "second", ProblemType.INTERNAL, Map.of(), null);

        assertNotEquals(first, second);
    }

    @Test
    void equalityConsidersType() {
        Problem first = new Problem("CODE", "desc", ProblemType.INTERNAL, Map.of(), null);
        Problem second = new Problem("CODE", "desc", ProblemType.NOT_FOUND, Map.of(), null);

        assertNotEquals(first, second);
    }

    @Test
    void equalityConsidersMetadata() {
        Problem first = new Problem("CODE", "desc", ProblemType.INTERNAL, Map.of("k", "1"), null);
        Problem second = new Problem("CODE", "desc", ProblemType.INTERNAL, Map.of("k", "2"), null);

        assertNotEquals(first, second);
    }

    @Test
    void hashCodeMatchesEquality() {
        Problem first = new Problem("CODE", "desc", ProblemType.INTERNAL, Map.of("k", "v"), new Exception());
        Problem second = new Problem("CODE", "desc", ProblemType.INTERNAL, Map.of("k", "v"), null);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void nullCodeRejected() {
        assertThrows(NullPointerException.class,
                () -> new Problem(null, "desc", ProblemType.INTERNAL, Map.of(), null));
    }

    @Test
    void blankCodeRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Problem("   ", "desc", ProblemType.INTERNAL, Map.of(), null));
    }

    @Test
    void nullDescriptionRejected() {
        assertThrows(NullPointerException.class,
                () -> new Problem("CODE", null, ProblemType.INTERNAL, Map.of(), null));
    }

    @Test
    void blankDescriptionRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Problem("CODE", "  ", ProblemType.INTERNAL, Map.of(), null));
    }

    @Test
    void nullTypeRejected() {
        assertThrows(NullPointerException.class,
                () -> new Problem("CODE", "desc", null, Map.of(), null));
    }

    @Test
    void nullMetadataNormalizedToEmptyMap() {
        Problem problem = new Problem("CODE", "desc", ProblemType.INTERNAL, null, null);

        assertTrue(problem.metadata().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> problem.metadata().put("x", "y"));
    }

    @Test
    void metadataIsCopiedAndImmutable() {
        Map<String, Object> source = new HashMap<>();
        source.put("k", "v");

        Problem problem = new Problem("CODE", "desc", ProblemType.INTERNAL, source, null);

        source.put("k2", "v2");
        assertEquals(Map.of("k", "v"), problem.metadata());

        assertThrows(UnsupportedOperationException.class, () -> problem.metadata().put("x", "y"));
    }

    @Test
    void causeIsOptional() {
        Problem withCause = new Problem("CODE", "desc", ProblemType.INTERNAL, null, new RuntimeException());
        Problem withoutCause = new Problem("CODE", "desc", ProblemType.INTERNAL, Map.of(), null);

        assertEquals("CODE", withCause.code());
        assertTrue(withoutCause.cause() == null);
        assertEquals("CODE", withoutCause.code());
    }
}
