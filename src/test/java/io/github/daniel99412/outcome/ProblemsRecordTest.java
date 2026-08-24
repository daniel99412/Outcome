package io.github.daniel99412.outcome;

import io.github.daniel99412.outcome.problem.Problem;
import io.github.daniel99412.outcome.problem.Problems;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProblemsRecordTest {

    @Test
    void isARecordWithListComponent() {
        assertTrue(Problems.class.isRecord());

        var componentType = Problems.class.getRecordComponents()[0].getType();
        assertEquals(List.class, componentType);
    }

    @Test
    void recordAccessorExposesTheImmutableSnapshot() {
        Problem problem = TestOutcomes.problem("A");
        Problems problems = new Problems(new ArrayList<>(List.of(problem)));

        List<Problem> accessor = problems.problems();

        assertThrows(UnsupportedOperationException.class,
                () -> accessor.add(TestOutcomes.problem("B")));
        assertEquals(problems.all(), accessor);
    }

    @Test
    void constructionCopiesTheSourceListDefensively() {
        List<Problem> source = new ArrayList<>(List.of(TestOutcomes.problem("A")));
        Problems problems = new Problems(source);

        source.clear();

        assertEquals(1, problems.size());
    }

    @Test
    void rejectsNullElementsFromMutableSource() {
        List<Problem> source = new ArrayList<>();
        source.add(TestOutcomes.problem("A"));
        source.add(null);

        assertThrows(NullPointerException.class, () -> new Problems(source));
    }

    @Test
    void iterableContractStillWorksViaStreamSupport() {
        Problems problems = new Problems(List.of(
                TestOutcomes.problem("A"),
                TestOutcomes.problem("B")));

        List<String> codes = StreamSupport.stream(problems.spliterator(), false)
                .map(Problem::code)
                .toList();

        assertEquals(List.of("A", "B"), codes);
    }
}
