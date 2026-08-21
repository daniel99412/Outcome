package io.github.daniel99412.outcome;

import io.github.daniel99412.outcome.error.Error;
import io.github.daniel99412.outcome.error.ErrorType;
import io.github.daniel99412.outcome.error.Errors;
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
    void mapErrorTransformsEveryError() {
        Failure<String> failure = TestOutcomes.failure("E1", "E2");

        Outcome<String> result = failure.mapError(e ->
                TestOutcomes.error(e.code() + "-mapped", e.description(), e.type()));

        assertTrue(result.isFailure());
        Failure<String> mapped = (Failure<String>) result;
        assertEquals(2, mapped.errors().size());
        assertEquals("E1-mapped", mapped.errors().first().code());
        assertEquals("E2-mapped", mapped.errors().all().get(1).code());
    }

    @Test
    void mapErrorDoesNotMutateOriginal() {
        Failure<String> failure = TestOutcomes.failure("E1");

        failure.mapError(e -> TestOutcomes.error(e.code() + "-mapped", e.description(), e.type()));

        assertEquals("E1", failure.errors().first().code());
        assertEquals(1, failure.errors().size());
    }

    @Test
    void mapErrorProducesNewFailure() {
        Failure<String> failure = TestOutcomes.failure("E1");

        Outcome<String> result = failure.mapError(e -> e);

        assertNotSame(failure, result);
    }

    @Test
    void mapErrorRejectsNullResult() {
        Failure<String> failure = TestOutcomes.failure("E1");

        assertThrows(NullPointerException.class, () -> failure.mapError(e -> null));
    }

    @Test
    void foldInvokesFailureBranch() {
        AtomicInteger successInvocations = new AtomicInteger(0);

        String result = TestOutcomes.failure("E1").fold(
                v -> {
                    successInvocations.incrementAndGet();
                    return "success";
                },
                errors -> "failure:" + errors.size());

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
    void peekErrorExecutesAndReturnsSame() {
        AtomicInteger executed = new AtomicInteger(0);
        Failure<String> failure = TestOutcomes.failure("E1");

        Outcome<String> result = failure.peekError(errors -> executed.incrementAndGet());

        assertEquals(1, executed.get());
        assertSame(failure, result);
    }

    @Test
    void recoverTransformsFailureIntoSuccess() {
        Outcome<String> result = TestOutcomes.failure("E1").recover(errors -> "default");

        assertTrue(result.isSuccess());
        assertEquals(new Success<>("default"), result);
    }

    @Test
    void recoverRejectsNullValue() {
        assertThrows(NullPointerException.class,
                () -> TestOutcomes.failure("E1").recover(errors -> null));
    }

    @Test
    void recoverWithTransformsFailureIntoOutcome() {
        Outcome<String> result = TestOutcomes.failure("E1")
                .recoverWith(errors -> new Success<>("fallback"));

        assertTrue(result.isSuccess());
        assertEquals(new Success<>("fallback"), result);
    }

    @Test
    void recoverWithCanReturnAnotherFailure() {
        Outcome<String> result = TestOutcomes.failure("E1")
                .recoverWith(errors -> new Failure<>(errors));

        assertTrue(result.isFailure());
    }

    @Test
    void recoverWithRejectsNullResult() {
        assertThrows(NullPointerException.class,
                () -> TestOutcomes.failure("E1").recoverWith(errors -> null));
    }

    @Test
    void nullErrorsRejected() {
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
        assertThrows(NullPointerException.class, () -> failure.peekError(null));
        assertThrows(NullPointerException.class, () -> failure.recover(null));
        assertThrows(NullPointerException.class, () -> failure.recoverWith(null));
    }

    @Test
    void errorTypePreservedThroughMapError() {
        Failure<String> failure = TestOutcomes.failure("E1");

        Outcome<String> result = failure.mapError(e ->
                TestOutcomes.error("new", e.description(), ErrorType.NOT_FOUND));

        Failure<String> mapped = (Failure<String>) result;
        assertEquals(ErrorType.NOT_FOUND, mapped.errors().first().type());
    }

    @Test
    void errorsExposedAreNotNull() {
        Errors errors = TestOutcomes.failure("E1").errors();

        assertTrue(errors.size() > 0);
        assertFalse(errors.all().isEmpty());
    }
}
