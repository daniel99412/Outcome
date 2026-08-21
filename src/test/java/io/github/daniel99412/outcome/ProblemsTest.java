package io.github.daniel99412.outcome;

import io.github.daniel99412.outcome.problem.Problem;
import io.github.daniel99412.outcome.problem.Problems;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProblemsTest {

    @Test
    void nullCollectionRejected() {
        assertThrows(NullPointerException.class, () -> new Problems(null));
    }

    @Test
    void emptyCollectionRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Problems(List.of()));
    }

    @Test
    void preservesInsertionOrder() {
        List<Problem> source = new ArrayList<>(List.of(
                TestOutcomes.problem("A"),
                TestOutcomes.problem("B"),
                TestOutcomes.problem("C")));

        Problems problems = new Problems(source);

        assertEquals(List.of("A", "B", "C"),
                problems.all().stream().map(Problem::code).toList());
    }

    @Test
    void snapshotIsImmutable() {
        List<Problem> source = new ArrayList<>(List.of(TestOutcomes.problem("A")));
        Problems problems = new Problems(source);

        source.add(TestOutcomes.problem("B"));
        source.set(0, TestOutcomes.problem("C"));

        assertEquals(1, problems.size());
        assertEquals("A", problems.first().code());
        assertThrows(UnsupportedOperationException.class, () -> problems.all().add(TestOutcomes.problem("X")));
    }

    @Test
    void containsWorks() {
        Problems problems = new Problems(List.of(TestOutcomes.problem("A"), TestOutcomes.problem("B")));

        assertTrue(problems.contains(TestOutcomes.problem("A")));
        assertTrue(problems.contains(TestOutcomes.problem("B")));
        assertTrue(!problems.contains(TestOutcomes.problem("Z")));
    }

    @Test
    void mapTransformsAllProblems() {
        Problems original = new Problems(List.of(TestOutcomes.problem("A"), TestOutcomes.problem("B")));

        Problems mapped = original.map(e -> TestOutcomes.problem(e.code() + "!", e.description(), e.type()));

        assertEquals(List.of("A!", "B!"),
                mapped.all().stream().map(Problem::code).toList());
    }

    @Test
    void mapPreservesOrder() {
        Problems original = new Problems(
                List.of(TestOutcomes.problem("A"), TestOutcomes.problem("B"), TestOutcomes.problem("C")));

        Problems mapped = original.map(e ->
                TestOutcomes.problem("x" + e.code(), e.description(), e.type()));

        assertEquals(List.of("xA", "xB", "xC"),
                mapped.all().stream().map(Problem::code).toList());
    }

    @Test
    void mapDoesNotMutateOriginal() {
        Problems original = new Problems(List.of(TestOutcomes.problem("A")));

        Problems mapped = original.map(e -> TestOutcomes.problem("B", e.description(), e.type()));

        assertEquals("A", original.first().code());
        assertEquals("B", mapped.first().code());
        assertNotSame(original, mapped);
    }

    @Test
    void mapRejectsNullResult() {
        Problems original = new Problems(List.of(TestOutcomes.problem("A")));

        assertThrows(NullPointerException.class, () -> original.map(e -> null));
    }

    @Test
    void iteratorIteratesInOrder() {
        Problems problems = new Problems(List.of(TestOutcomes.problem("A"), TestOutcomes.problem("B")));

        List<String> codes = new ArrayList<>();
        Iterator<Problem> it = problems.iterator();
        while (it.hasNext()) {
            codes.add(it.next().code());
        }

        assertEquals(List.of("A", "B"), codes);
    }

    @Test
    void sizeAndFirst() {
        Problems problems = new Problems(List.of(TestOutcomes.problem("A"), TestOutcomes.problem("B")));

        assertEquals(2, problems.size());
        assertEquals("A", problems.first().code());
    }
}
