package dev.daniel.outcome;

import dev.daniel.outcome.error.Error;
import dev.daniel.outcome.error.Errors;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErrorsTest {

    @Test
    void nullCollectionRejected() {
        assertThrows(NullPointerException.class, () -> new Errors(null));
    }

    @Test
    void emptyCollectionRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Errors(List.of()));
    }

    @Test
    void preservesInsertionOrder() {
        List<Error> source = new ArrayList<>(List.of(
                TestOutcomes.error("A"),
                TestOutcomes.error("B"),
                TestOutcomes.error("C")));

        Errors errors = new Errors(source);

        assertEquals(List.of("A", "B", "C"),
                errors.all().stream().map(Error::code).toList());
    }

    @Test
    void snapshotIsImmutable() {
        List<Error> source = new ArrayList<>(List.of(TestOutcomes.error("A")));
        Errors errors = new Errors(source);

        source.add(TestOutcomes.error("B"));
        source.set(0, TestOutcomes.error("C"));

        assertEquals(1, errors.size());
        assertEquals("A", errors.first().code());
        assertThrows(UnsupportedOperationException.class, () -> errors.all().add(TestOutcomes.error("X")));
    }

    @Test
    void containsWorks() {
        Errors errors = new Errors(List.of(TestOutcomes.error("A"), TestOutcomes.error("B")));

        assertTrue(errors.contains(TestOutcomes.error("A")));
        assertTrue(errors.contains(TestOutcomes.error("B")));
        assertTrue(!errors.contains(TestOutcomes.error("Z")));
    }

    @Test
    void mapTransformsAllErrors() {
        Errors original = new Errors(List.of(TestOutcomes.error("A"), TestOutcomes.error("B")));

        Errors mapped = original.map(e -> TestOutcomes.error(e.code() + "!", e.description(), e.type()));

        assertEquals(List.of("A!", "B!"),
                mapped.all().stream().map(Error::code).toList());
    }

    @Test
    void mapPreservesOrder() {
        Errors original = new Errors(
                List.of(TestOutcomes.error("A"), TestOutcomes.error("B"), TestOutcomes.error("C")));

        Errors mapped = original.map(e ->
                TestOutcomes.error("x" + e.code(), e.description(), e.type()));

        assertEquals(List.of("xA", "xB", "xC"),
                mapped.all().stream().map(Error::code).toList());
    }

    @Test
    void mapDoesNotMutateOriginal() {
        Errors original = new Errors(List.of(TestOutcomes.error("A")));

        Errors mapped = original.map(e -> TestOutcomes.error("B", e.description(), e.type()));

        assertEquals("A", original.first().code());
        assertEquals("B", mapped.first().code());
        assertNotSame(original, mapped);
    }

    @Test
    void mapRejectsNullResult() {
        Errors original = new Errors(List.of(TestOutcomes.error("A")));

        assertThrows(NullPointerException.class, () -> original.map(e -> null));
    }

    @Test
    void iteratorIteratesInOrder() {
        Errors errors = new Errors(List.of(TestOutcomes.error("A"), TestOutcomes.error("B")));

        List<String> codes = new ArrayList<>();
        Iterator<Error> it = errors.iterator();
        while (it.hasNext()) {
            codes.add(it.next().code());
        }

        assertEquals(List.of("A", "B"), codes);
    }

    @Test
    void sizeAndFirst() {
        Errors errors = new Errors(List.of(TestOutcomes.error("A"), TestOutcomes.error("B")));

        assertEquals(2, errors.size());
        assertEquals("A", errors.first().code());
    }
}
