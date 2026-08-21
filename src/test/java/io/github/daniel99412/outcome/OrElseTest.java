package io.github.daniel99412.outcome;

import io.github.daniel99412.outcome.problem.Problems;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static io.github.daniel99412.outcome.TestOutcomes.failure;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrElseTest {

    @Test
    void orElseReturnsValueOnSuccess() {
        assertEquals("v", Outcome.<String>success("v").orElse("other"));
    }

    @Test
    void orElseReturnsOtherOnFailure() {
        assertEquals("fallback", failure("E1").orElse("fallback"));
    }

    @Test
    void orElseRejectsNullOtherEvenOnSuccess() {
        assertThrows(NullPointerException.class, () -> Outcome.success("v").orElse(null));
        assertThrows(NullPointerException.class, () -> failure("E1").orElse(null));
    }

    @Test
    void orElseGetDoesNotExecuteSupplierOnSuccess() {
        AtomicBoolean executed = new AtomicBoolean(false);

        String result = Outcome.<String>success("v")
                .orElseGet(() -> {
                    executed.set(true);
                    return "supplied";
                });

        assertEquals("v", result);
        assertFalse(executed.get());
    }

    @Test
    void orElseGetExecutesSupplierOnFailure() {
        assertEquals("supplied", failure("E1").orElseGet(() -> "supplied"));
    }

    @Test
    void orElseRejectsNullSupplier() {
        assertThrows(NullPointerException.class,
                () -> Outcome.success("v").orElseGet(null));
        assertThrows(NullPointerException.class,
                () -> failure("E1").orElseGet(null));
    }

    @Test
    void orElseGetRejectsNullSuppliedValue() {
        assertThrows(NullPointerException.class,
                () -> failure("E1").orElseGet(() -> null));
    }

    @Test
    void orElseThrowReturnsValueOnSuccessWithoutExecutingMapper() {
        AtomicInteger invocations = new AtomicInteger(0);

        String result = Outcome.<String>success("v").orElseThrow(problems -> {
            invocations.incrementAndGet();
            return new IllegalStateException();
        });

        assertEquals("v", result);
        assertEquals(0, invocations.get());
    }

    @Test
    void orElseThrowThrowsMappedExceptionOnFailure() {
        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> failure("E1", "E2").orElseThrow(problems ->
                        new IllegalStateException("code=" + problems.first().code())));

        assertEquals("code=E1", thrown.getMessage());
    }

    @Test
    void orElseThrowMapsFullProblemsInstance() {
        MappedException thrown = assertThrows(MappedException.class,
                () -> failure("E1", "E2").orElseThrow(MappedException::new));

        assertEquals(2, thrown.problems.size());
        assertEquals("E1", thrown.problems.first().code());
    }

    @Test
    void orElseThrowPropagatesCheckedExceptions() {
        assertThrows(CheckedException.class,
                () -> failure("E1").orElseThrow(p -> new CheckedException()));
    }

    @Test
    void orElseThrowRejectsNullMapper() {
        assertThrows(NullPointerException.class,
                () -> Outcome.success("v").orElseThrow(null));
        assertThrows(NullPointerException.class,
                () -> failure("E1").orElseThrow(null));
    }

    @Test
    void orElseThrowRejectsNullException() {
        assertThrows(NullPointerException.class,
                () -> failure("E1").orElseThrow(p -> null));
    }

    @Test
    void orElseFamilyNeverMutatesOriginalOutcome() {
        Outcome<String> outcome = failure("E1");
        Problems before = ((Failure<String>) outcome).problems();

        outcome.orElse("x");
        outcome.orElseGet(() -> "y");
        assertThrows(IllegalStateException.class,
                () -> outcome.orElseThrow(p -> new IllegalStateException()));

        assertTrue(outcome.isFailure());
        assertEquals(before, ((Failure<String>) outcome).problems());
    }

    private static final class MappedException extends RuntimeException {
        final Problems problems;

        MappedException(Problems problems) {
            super("mapped");
            this.problems = problems;
        }
    }

    private static final class CheckedException extends Exception {
    }
}
