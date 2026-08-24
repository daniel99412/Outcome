package io.github.daniel99412.outcome;

import io.github.daniel99412.outcome.problem.Problem;
import io.github.daniel99412.outcome.problem.ProblemType;
import io.github.daniel99412.outcome.problem.Problems;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatchingTest {

    @Nested
    @DisplayName("catching(work)")
    class SingleArgument {

        @Test
        void returnsSuccessWithValue() {
            Outcome<String> result = Outcome.catching(() -> "ok");

            assertTrue(result.isSuccess());
            assertEquals("ok", result.orElseThrow(problems -> new AssertionError()));
        }

        @Test
        void capturesCheckedExceptions() {
            IOException thrown = new IOException("disk on fire");

            Outcome<String> result = Outcome.catching(() -> {
                throw thrown;
            });

            assertTrue(result.isFailure());
            Problem problem = problemsOf(result);
            assertSame(thrown, problem.cause());
        }

        @Test
        void mapsExceptionToInternalProblemWithDefaults() {
            IOException exception = new IOException("boom");

            Outcome<String> result = Outcome.catching(() -> {
                throw exception;
            });

            Problem problem = problemsOf(result);
            assertEquals("UNEXPECTED_FAILURE", problem.code());
            assertEquals("boom", problem.description());
            assertEquals(ProblemType.INTERNAL, problem.type());
            assertNotNull(problem.cause());
            assertSame(exception, problem.cause());
            assertTrue(problem.metadata().isEmpty());
        }

        @Test
        void blankMessageFallsBackToExceptionClassName() {
            RuntimeException exception = new RuntimeException();

            Outcome<String> result = Outcome.catching(() -> {
                throw exception;
            });

            Problem problem = problemsOf(result);
            assertEquals(RuntimeException.class.getName(), problem.description());
        }

        @Test
        void blankSpacedMessageFallsBackToExceptionClassName() {
            RuntimeException exception = new RuntimeException("   ");

            Outcome<String> result = Outcome.catching(() -> {
                throw exception;
            });

            Problem problem = problemsOf(result);
            assertEquals(RuntimeException.class.getName(), problem.description());
        }

        @Test
        void fatalErrorsAreNotCaught() {
            assertThrows(AssertionError.class, () ->
                    Outcome.catching(() -> {
                        throw new AssertionError("fatal");
                    }));
        }

        @Test
        void nullWorkIsRejected() {
            NullPointerException thrown = assertThrows(NullPointerException.class,
                    () -> Outcome.catching(null));
            assertEquals("work cannot be null", thrown.getMessage());
        }

        @Test
        void nullResultFromWorkFailsFast() {
            NullPointerException thrown = assertThrows(NullPointerException.class,
                    () -> Outcome.catching(() -> null));
            assertEquals("work cannot return null", thrown.getMessage());
        }
    }

    @Nested
    @DisplayName("catching(work, toProblem)")
    class TwoArguments {

        @Test
        void returnsSuccessWithoutTouchingMapper() {
            Outcome<String> result = Outcome.catching(
                    () -> "ok",
                    exception -> {
                        throw new AssertionError("mapper must not run");
                    });

            assertTrue(result.isSuccess());
            assertEquals("ok", result.orElseThrow(problems -> new AssertionError()));
        }

        @Test
        void usesMappedProblemOnFailure() {
            Problem mapped = Problem.validation("PARSE_FAILED", "bad input");

            Outcome<String> result = Outcome.catching(
                    () -> {
                        throw new IllegalStateException("nope");
                    },
                    exception -> mapped);

            Problem problem = problemsOf(result);
            assertEquals(mapped.code(), problem.code());
            assertEquals(ProblemType.VALIDATION, problem.type());
        }

        @Test
        void mapperReceivesOriginalException() {
            IllegalStateException exception = new IllegalStateException("nope");

            Outcome<String> result = Outcome.catching(
                    () -> {
                        throw exception;
                    },
                    received -> {
                        assertSame(exception, received);
                        return Problem.internal("ANY", "desc");
                    });

            assertTrue(result.isFailure());
        }

        @Test
        void nullMapperIsRejectedEvenWhenWorkSucceeds() {
            assertThrows(NullPointerException.class,
                    () -> Outcome.catching(() -> "ok", null));
        }

        @Test
        void mapperReturningNullFailsFastWithExactMessage() {
            NullPointerException thrown = assertThrows(NullPointerException.class,
                    () -> Outcome.catching(
                            () -> {
                                throw new IOException("x");
                            },
                            exception -> null));
            assertEquals("catching mapper cannot return null", thrown.getMessage());
        }

        @Test
        void fatalErrorsAreNotCaughtAndNeverReachMapper() {
            assertThrows(AssertionError.class, () ->
                    Outcome.catching(
                            () -> {
                                throw new AssertionError("fatal");
                            },
                            exception -> Problem.internal("NEVER", "never")));
        }

        @Test
        void customMapperCanAttachMetadataAndCause() {
            Map<String, Object> metadata = Map.of();

            Outcome<String> result = Outcome.catching(
                    () -> {
                        throw new IOException("disk");
                    },
                    exception -> new Problem(
                            "IO_FAILED",
                            "I/O error",
                            ProblemType.DEPENDENCY,
                            metadata,
                            exception));

            Problem problem = problemsOf(result);
            assertEquals("IO_FAILED", problem.code());
            assertEquals(ProblemType.DEPENDENCY, problem.type());
            assertNull(problem.metadata().get("anything"));
            assertNotNull(problem.cause());
        }
    }

    private static Problem problemsOf(Outcome<?> outcome) {
        return outcome.fold(
                value -> {
                    throw new AssertionError("expected failure but got: " + value);
                },
                Problems::first);
    }
}
