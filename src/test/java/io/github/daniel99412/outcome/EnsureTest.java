package io.github.daniel99412.outcome;

import io.github.daniel99412.outcome.problem.Problem;
import io.github.daniel99412.outcome.problem.ProblemType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnsureTest {

    @Test
    void satisfyingPredicateReturnsSameInstance() {
        Outcome<String> success = new Success<>("ada");

        Outcome<String> result = success.ensure(
                value -> value.length() > 1,
                () -> {
                    throw new AssertionError("supplier must not run");
                });

        assertSame(success, result);
    }

    @Test
    void failingPredicateProducesFailureWithSuppliedProblem() {
        Problem problem = Problem.conflict("USER_INACTIVE", "suspended");

        Outcome<String> result = new Success<>("ada").ensure(
                String::isEmpty,
                () -> problem);

        assertTrue(result.isFailure());
        result.peekProblem(problems -> assertEquals(problem, problems.first()));
    }

    @Test
    void failureIsReturnedUntouchedWithoutExecutingCallbacks() {
        Outcome<String> failure = TestOutcomes.failure("E1");
        AtomicBoolean predicateRan = new AtomicBoolean(false);
        AtomicBoolean supplierRan = new AtomicBoolean(false);

        Outcome<String> result = failure.ensure(
                value -> predicateRan.getAndSet(true),
                () -> {
                    supplierRan.set(true);
                    throw new AssertionError("supplier must not run");
                });

        assertSame(failure, result);
        assertFalse(predicateRan.get());
        assertFalse(supplierRan.get());
    }

    @Test
    void nullPredicateIsRejectedEvenOnFailure() {
        Outcome<String> failure = TestOutcomes.failure("E1");

        NullPointerException thrown = assertThrows(NullPointerException.class,
                () -> failure.ensure(null, () -> Problem.internal("ANY", "d")));
        assertEquals("ensure predicate cannot be null", thrown.getMessage());
    }

    @Test
    void nullSupplierIsRejectedEvenOnSuccess() {
        NullPointerException thrown = assertThrows(NullPointerException.class,
                () -> new Success<>("x").ensure(value -> true, null));
        assertEquals("ensure problem supplier cannot be null", thrown.getMessage());
    }

    @Test
    void supplierReturningNullFailsFast() {
        NullPointerException thrown = assertThrows(NullPointerException.class,
                () -> new Success<>("x").ensure(String::isEmpty, () -> null));
        assertEquals("ensure problem supplier cannot return null", thrown.getMessage());
    }

    @Test
    void worksInsideTraverseAccumulatingDownstream() {
        Outcome<Integer> valid = Outcome.success(10);
        Outcome<Integer> invalid = Outcome.success(-1).ensure(
                n -> n > 0,
                () -> Problem.validation("NOT_POSITIVE", "must be positive"));

        var combined = Outcome.traverse(
                java.util.List.of(valid, invalid),
                outcome -> outcome);

        assertTrue(combined.isFailure());
        combined.peekProblem(problems -> {
            assertEquals(1, problems.size());
            assertEquals("NOT_POSITIVE", problems.first().code());
            assertEquals(ProblemType.VALIDATION, problems.first().type());
        });
    }
}
