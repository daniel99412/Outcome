package io.github.daniel99412.outcome;

import io.github.daniel99412.outcome.problem.Problem;
import io.github.daniel99412.outcome.problem.ProblemType;
import io.github.daniel99412.outcome.problem.Problems;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FailureTest {

    @Test
    void mapDoesNotExecuteMapper() {
        AtomicInteger invocations = new AtomicInteger(0);
        Failure<String> failure = TestOutcomes.failure("E1");

        Outcome<String> result = failure.map(v -> {
            invocations.incrementAndGet();
            return v + "!";
        });

        assertEquals(0, invocations.get());
        assertSame(failure, result);
    }

    @Test
    void flatMapDoesNotExecuteMapper() {
        AtomicInteger invocations = new AtomicInteger(0);
        Failure<String> failure = TestOutcomes.failure("E1");

        Outcome<String> result = failure.flatMap(v -> {
            invocations.incrementAndGet();
            return new Success<>("x");
        });

        assertEquals(0, invocations.get());
        assertSame(failure, result);
    }

    @Test
    void mapProblemTransformsEveryProblem() {
        Failure<String> failure = TestOutcomes.failure("E1", "E2");

        Outcome<String> result = failure.mapProblem(e ->
                TestOutcomes.problem(e.code() + "-mapped", e.description(), e.type()));

        assertTrue(result.isFailure());
        Failure<String> mapped = (Failure<String>) result;
        assertEquals(2, mapped.problems().size());
        assertEquals("E1-mapped", mapped.problems().first().code());
        assertEquals("E2-mapped", mapped.problems().all().get(1).code());
    }

    @Test
    void mapProblemDoesNotMutateOriginal() {
        Failure<String> failure = TestOutcomes.failure("E1");

        failure.mapProblem(e -> TestOutcomes.problem(e.code() + "-mapped", e.description(), e.type()));

        assertEquals("E1", failure.problems().first().code());
        assertEquals(1, failure.problems().size());
    }

    @Test
    void mapProblemProducesNewFailure() {
        Failure<String> failure = TestOutcomes.failure("E1");

        Outcome<String> result = failure.mapProblem(e -> e);

        assertNotSame(failure, result);
    }

    @Test
    void mapProblemRejectsNullResult() {
        Failure<String> failure = TestOutcomes.failure("E1");

        assertThrows(NullPointerException.class, () -> failure.mapProblem(e -> null));
    }

    @Test
    void foldInvokesFailureBranch() {
        AtomicInteger successInvocations = new AtomicInteger(0);

        String result = TestOutcomes.failure("E1").fold(
                v -> {
                    successInvocations.incrementAndGet();
                    return "success";
                },
                problems -> "failure:" + problems.size());

        assertEquals("failure:1", result);
        assertEquals(0, successInvocations.get());
    }

    @Test
    void peekDoesNotExecute() {
        AtomicInteger executed = new AtomicInteger(0);
        Failure<String> failure = TestOutcomes.failure("E1");

        Outcome<String> result = failure.peek(v -> executed.incrementAndGet());

        assertEquals(0, executed.get());
        assertSame(failure, result);
    }

    @Test
    void peekProblemExecutesAndReturnsSame() {
        AtomicInteger executed = new AtomicInteger(0);
        Failure<String> failure = TestOutcomes.failure("E1");

        Outcome<String> result = failure.peekProblem(problems -> executed.incrementAndGet());

        assertEquals(1, executed.get());
        assertSame(failure, result);
    }

    @Test
    void recoverTransformsFailureIntoSuccess() {
        Outcome<String> result = TestOutcomes.failure("E1").recover(problems -> "default");

        assertTrue(result.isSuccess());
        assertEquals(new Success<>("default"), result);
    }

    @Test
    void recoverRejectsNullValue() {
        assertThrows(NullPointerException.class,
                () -> TestOutcomes.failure("E1").recover(problems -> null));
    }

    @Test
    void recoverWithTransformsFailureIntoOutcome() {
        Outcome<String> result = TestOutcomes.failure("E1")
                .recoverWith(problems -> new Success<>("fallback"));

        assertTrue(result.isSuccess());
        assertEquals(new Success<>("fallback"), result);
    }

    @Test
    void recoverWithCanReturnAnotherFailure() {
        Outcome<String> result = TestOutcomes.failure("E1")
                .recoverWith(problems -> new Failure<>(problems));

        assertTrue(result.isFailure());
    }

    @Test
    void recoverWithRejectsNullResult() {
        assertThrows(NullPointerException.class,
                () -> TestOutcomes.failure("E1").recoverWith(problems -> null));
    }

    @Test
    void nullProblemsRejected() {
        assertThrows(NullPointerException.class, () -> new Failure<>(null));
    }

    @Test
    void nullArgumentsRejected() {
        Failure<String> failure = TestOutcomes.failure("E1");

        assertThrows(NullPointerException.class, () -> failure.map(null));
        assertThrows(NullPointerException.class, () -> failure.flatMap(null));
        assertThrows(NullPointerException.class, () -> failure.fold(null, e -> "x"));
        assertThrows(NullPointerException.class, () -> failure.fold(v -> "x", null));
        assertThrows(NullPointerException.class, () -> failure.peek(null));
        assertThrows(NullPointerException.class, () -> failure.peekProblem(null));
        assertThrows(NullPointerException.class, () -> failure.recover(null));
        assertThrows(NullPointerException.class, () -> failure.recoverWith(null));
    }

    @Test
    void problemTypePreservedThroughMapProblem() {
        Failure<String> failure = TestOutcomes.failure("E1");

        Outcome<String> result = failure.mapProblem(e ->
                TestOutcomes.problem("new", e.description(), ProblemType.NOT_FOUND));

        Failure<String> mapped = (Failure<String>) result;
        assertEquals(ProblemType.NOT_FOUND, mapped.problems().first().type());
    }

    @Test
    void problemsExposedAreNotNull() {
        Problems problems = TestOutcomes.failure("E1").problems();

        assertTrue(problems.size() > 0);
        assertFalse(problems.all().isEmpty());
    }
}
