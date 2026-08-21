package io.github.daniel99412.outcome;

import io.github.daniel99412.outcome.problem.Problem;
import io.github.daniel99412.outcome.problem.Problems;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.github.daniel99412.outcome.TestOutcomes.failure;
import static io.github.daniel99412.outcome.TestOutcomes.problem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutcomeFactoriesTest {

    @Test
    void successCarriesValue() {
        Outcome<String> outcome = Outcome.success("v");

        assertTrue(outcome instanceof Success<String>);
        assertEquals(new Success<>("v"), outcome);
    }

    @Test
    void successRejectsNull() {
        assertThrows(NullPointerException.class, () -> Outcome.success(null));
    }

    @Test
    void failureWithSingleProblem() {
        Problem problem = problem("E1");

        Outcome<String> outcome = Outcome.failure(problem);

        assertTrue(outcome instanceof Failure<String>);
        Failure<String> f = (Failure<String>) outcome;
        assertEquals(1, f.problems().size());
        assertEquals(problem, f.problems().first());
    }

    @Test
    void failureRejectsNullProblem() {
        assertThrows(NullPointerException.class, () -> Outcome.failure((Problem) null));
    }

    @Test
    void failureWithProblemsCarriesSameInstance() {
        Problems problems = new Problems(List.of(problem("E1"), problem("E2")));

        Outcome<String> outcome = Outcome.failure(problems);

        assertSame(problems, ((Failure<String>) outcome).problems());
    }

    @Test
    void failureRejectsNullProblems() {
        assertThrows(NullPointerException.class, () -> Outcome.failure((Problems) null));
    }

    @Test
    void failureWithVarargsPreservesOrder() {
        Problem first = problem("E1");
        Problem second = problem("E2");

        Outcome<String> outcome = Outcome.failure(first, second);

        assertEquals(List.of(first, second), ((Failure<String>) outcome).problems().all());
    }

    @Test
    void failureWithSingleVarargMatchesSingleProblemOverload() {
        Problem problem = problem("E1");

        Outcome<String> outcome = Outcome.failure(problem);

        assertEquals(Outcome.failure(problem), outcome);
    }

    @Test
    void failureVarargsRejectsNullArray() {
        assertThrows(NullPointerException.class,
                () -> Outcome.failure((Problem[]) null));
    }

    @Test
    void failureVarargsRejectsNullElement() {
        assertThrows(NullPointerException.class,
                () -> Outcome.failure(problem("E1"), (Problem) null));
    }

    @Test
    void failureVarargsRejectsEmpty() {
        assertThrows(IllegalArgumentException.class, Outcome::failure);
    }

    @Test
    void factoriesComposeLikeDirectConstruction() {
        assertEquals(
                List.of("a", "b"),
                Outcome.sequence(Outcome.success("a"), Outcome.success("b")).orElseThrow(p -> new AssertionError()));

        assertEquals(failure("E1", "E2"), Outcome.failure(problem("E1"), problem("E2")));
    }
}
