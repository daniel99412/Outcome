package io.github.daniel99412.outcome;

import io.github.daniel99412.outcome.error.Error;
import io.github.daniel99412.outcome.error.ErrorType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErrorTest {

    @Test
    void equalityIgnoresCause() {
        RuntimeException cause = new RuntimeException("diagnostic");
        Error first = new Error("CODE", "desc", ErrorType.INTERNAL, Map.of("k", "v"), cause);
        Error second = new Error("CODE", "desc", ErrorType.INTERNAL, Map.of("k", "v"), new RuntimeException("other"));

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void equalityConsidersCode() {
        Error first = TestOutcomes.error("A");
        Error second = TestOutcomes.error("B");

        assertNotEquals(first, second);
    }

    @Test
    void equalityConsidersDescription() {
        Error first = new Error("CODE", "first", ErrorType.INTERNAL, Map.of(), null);
        Error second = new Error("CODE", "second", ErrorType.INTERNAL, Map.of(), null);

        assertNotEquals(first, second);
    }

    @Test
    void equalityConsidersType() {
        Error first = new Error("CODE", "desc", ErrorType.INTERNAL, Map.of(), null);
        Error second = new Error("CODE", "desc", ErrorType.NOT_FOUND, Map.of(), null);

        assertNotEquals(first, second);
    }

    @Test
    void equalityConsidersMetadata() {
        Error first = new Error("CODE", "desc", ErrorType.INTERNAL, Map.of("k", "1"), null);
        Error second = new Error("CODE", "desc", ErrorType.INTERNAL, Map.of("k", "2"), null);

        assertNotEquals(first, second);
    }

    @Test
    void hashCodeMatchesEquality() {
        Error first = new Error("CODE", "desc", ErrorType.INTERNAL, Map.of("k", "v"), new Exception());
        Error second = new Error("CODE", "desc", ErrorType.INTERNAL, Map.of("k", "v"), null);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void nullCodeRejected() {
        assertThrows(NullPointerException.class,
                () -> new Error(null, "desc", ErrorType.INTERNAL, Map.of(), null));
    }

    @Test
    void blankCodeRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Error("   ", "desc", ErrorType.INTERNAL, Map.of(), null));
    }

    @Test
    void nullDescriptionRejected() {
        assertThrows(NullPointerException.class,
                () -> new Error("CODE", null, ErrorType.INTERNAL, Map.of(), null));
    }

    @Test
    void blankDescriptionRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Error("CODE", "  ", ErrorType.INTERNAL, Map.of(), null));
    }

    @Test
    void nullTypeRejected() {
        assertThrows(NullPointerException.class,
                () -> new Error("CODE", "desc", null, Map.of(), null));
    }

    @Test
    void nullMetadataNormalizedToEmptyMap() {
        Error error = new Error("CODE", "desc", ErrorType.INTERNAL, null, null);

        assertTrue(error.metadata().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> error.metadata().put("x", "y"));
    }

    @Test
    void metadataIsCopiedAndImmutable() {
        Map<String, Object> source = new HashMap<>();
        source.put("k", "v");

        Error error = new Error("CODE", "desc", ErrorType.INTERNAL, source, null);

        source.put("k2", "v2");
        assertEquals(Map.of("k", "v"), error.metadata());

        assertThrows(UnsupportedOperationException.class, () -> error.metadata().put("x", "y"));
    }

    @Test
    void causeIsOptional() {
        Error withCause = new Error("CODE", "desc", ErrorType.INTERNAL, null, new RuntimeException());
        Error withoutCause = new Error("CODE", "desc", ErrorType.INTERNAL, Map.of(), null);

        assertEquals("CODE", withCause.code());
        assertTrue(withoutCause.cause() == null);
        assertEquals("CODE", withoutCause.code());
    }
}
