package io.github.daniel99412.outcome;

import io.github.daniel99412.outcome.problem.Problem;
import io.github.daniel99412.outcome.problem.Problems;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OtherwiseTest {

    @Test
    void successReturnsSameInstanceWithoutEvaluatingFallback() {
        Outcome<String> success = new Success<>("cached");

        Outcome<String> result = success.otherwise(() -> {
            throw new AssertionError("fallback must not run");
        });

        assertSame(success, result);
    }

    @Test
    void failureReturnsFallbackSuccessValue() {
        Outcome<String> failure = TestOutcomes.failure("CACHE_MISS");

        Outcome<String> result = failure.otherwise(() -> new Success<>("from-db"));

        assertTrue(result.isSuccess());
        assertEquals("from-db", result.orElseThrow(problems -> new AssertionError()));
    }

    @Test
    void fallbackFailureReplacesOriginalProblems() {
        Outcome<String> failure = TestOutcomes.failure("CACHE_MISS");

        Outcome<String> result = failure.otherwise(() -> TestOutcomes.failure("DB_DOWN", "DB_TIMEOUT"));

        assertTrue(result.isFailure());
        Problems problems = result.fold(
                value -> {
                    throw new AssertionError("expected failure");
                },
                identity -> identity);
        assertEquals(2, problems.size());
        assertEquals("DB_DOWN", problems.first().code());
        assertTrue(problems.hasCode("DB_TIMEOUT"));
    }

    @Test
    void fallbackIsEvaluatedLazilyExactlyOnceOnFailure() {
        Outcome<String> failure = TestOutcomes.failure("E1");
        AtomicInteger evaluations = new AtomicInteger();

        Outcome<String> result = failure.otherwise(() -> {
            evaluations.incrementAndGet();
            return new Success<>("recovered");
        });

        assertEquals(1, evaluations.get());
        assertTrue(result.isSuccess());
    }

    @Test
    void nullFallbackIsRejectedEvenOnSuccess() {
        assertThrows(NullPointerException.class,
                () -> new Success<>("x").otherwise(null));
    }

    @Test
    void fallbackReturningNullFailsFastWithExactMessage() {
        NullPointerException thrown = assertThrows(NullPointerException.class,
                () -> TestOutcomes.failure("E1").otherwise(() -> null));
        assertEquals("otherwise fallback cannot return null", thrown.getMessage());
    }

    @Test
    void cascadeStopsAtFirstWorkingStrategy() {
        AtomicInteger apiCalls = new AtomicInteger();

        Outcome<String> result = TestOutcomes.failure("CACHE_MISS")
                .otherwise(() -> TestOutcomes.failure("DB_DOWN"))
                .otherwise(() -> {
                    apiCalls.incrementAndGet();
                    return new Success<>("from-api");
                })
                .otherwise(() -> {
                    throw new AssertionError("must never run");
                });

        assertEquals(1, apiCalls.get());
        assertEquals("from-api", result.orElseThrow(problems -> new AssertionError()));
    }

    @Test
    void worksWithProblemFactoriesInFallback() {
        Outcome<String> result = TestOutcomes.failure("E1")
                .otherwise(() -> Outcome.failure(
                        Problem.dependency("DOWNSTREAM_500", "upstream exploded")));

        assertTrue(result.isFailure());
        assertEquals("DOWNSTREAM_500", result.fold(
                value -> {
                    throw new AssertionError("expected failure");
                },
                problems -> problems.first().code()));
    }
}
