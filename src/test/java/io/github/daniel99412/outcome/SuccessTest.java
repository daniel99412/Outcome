package io.github.daniel99412.outcome;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuccessTest {

    @Test
    void mapTransformsValue() {
        Outcome<Integer> result = new Success<>(2).map(v -> v * 3);

        assertTrue(result.isSuccess());
        assertEquals(new Success<>(6), result);
    }

    @Test
    void flatMapChains() {
        Outcome<String> result = new Success<>(2)
                .flatMap(v -> new Success<>("value=" + v));

        assertTrue(result.isSuccess());
        assertEquals(new Success<>("value=2"), result);
    }

    @Test
    void flatMapRejectsNullResult() {
        assertThrows(NullPointerException.class,
                () -> new Success<>(1).flatMap(v -> null));
    }

    @Test
    void mapRejectsNullMapper() {
        assertThrows(NullPointerException.class, () -> new Success<>(1).map(null));
    }

    @Test
    void mapProblemDoesNotExecuteMapper() {
        AtomicInteger invocations = new AtomicInteger(0);
        Outcome<Integer> outcome = new Success<>(1);

        Outcome<Integer> result = outcome.mapProblem(e -> {
            invocations.incrementAndGet();
            return e;
        });

        assertEquals(0, invocations.get());
        assertSame(outcome, result);
    }

    @Test
    void foldInvokesSuccessBranch() {
        AtomicInteger failureInvocations = new AtomicInteger(0);

        String result = new Success<>(5).fold(
                v -> "success:" + v,
                problems -> {
                    failureInvocations.incrementAndGet();
                    return "failure";
                });

        assertEquals("success:5", result);
        assertEquals(0, failureInvocations.get());
    }

    @Test
    void peekExecutesAndReturnsSame() {
        AtomicInteger executed = new AtomicInteger(0);
        Outcome<Integer> outcome = new Success<>(1);

        Outcome<Integer> result = outcome.peek(v -> executed.incrementAndGet());

        assertEquals(1, executed.get());
        assertSame(outcome, result);
    }

    @Test
    void peekProblemDoesNotExecute() {
        AtomicInteger executed = new AtomicInteger(0);
        Outcome<Integer> outcome = new Success<>(1);

        Outcome<Integer> result = outcome.peekProblem(problems -> executed.incrementAndGet());

        assertEquals(0, executed.get());
        assertSame(outcome, result);
    }

    @Test
    void recoverReturnsSameSuccess() {
        AtomicInteger invoked = new AtomicInteger(0);
        Outcome<String> outcome = new Success<>("ok");

        Outcome<String> result = outcome.recover(problems -> {
            invoked.incrementAndGet();
            return "recovered";
        });

        assertEquals(0, invoked.get());
        assertSame(outcome, result);
    }

    @Test
    void recoverWithReturnsSameSuccess() {
        AtomicInteger invoked = new AtomicInteger(0);
        Outcome<String> outcome = new Success<>("ok");

        Outcome<String> result = outcome.recoverWith(problems -> {
            invoked.incrementAndGet();
            return new Success<>("recovered");
        });

        assertEquals(0, invoked.get());
        assertSame(outcome, result);
    }

    @Test
    void isSuccessAndIsFailure() {
        Outcome<String> success = new Success<>("v");

        assertTrue(success.isSuccess());
        assertFalse(success.isFailure());
    }

    @Test
    void nullValueRejected() {
        assertThrows(NullPointerException.class, () -> new Success<>(null));
    }

    @Test
    void nullArgumentsRejected() {
        Outcome<String> success = new Success<>("v");

        assertThrows(NullPointerException.class, () -> success.flatMap(null));
        assertThrows(NullPointerException.class, () -> success.fold(null, e -> "x"));
        assertThrows(NullPointerException.class, () -> success.fold(v -> "x", null));
        assertThrows(NullPointerException.class, () -> success.peek(null));
        assertThrows(NullPointerException.class, () -> success.peekProblem(null));
        assertThrows(NullPointerException.class, () -> success.recover(null));
        assertThrows(NullPointerException.class, () -> success.recoverWith(null));
    }

    @Test
    void peekPropagatesException() {
        Outcome<Integer> success = new Success<>(1);

        assertThrows(IllegalStateException.class,
                () -> success.peek(v -> {
                    throw new IllegalStateException("boom");
                }));
    }

    @Test
    void foldDoesNotTouchFailureBranchFunction() {
        AtomicBoolean failureBranch = new AtomicBoolean(false);
        new Success<>("v").fold(v -> "ok", problems -> {
            failureBranch.set(true);
            return "bad";
        });

        assertFalse(failureBranch.get());
    }
}
