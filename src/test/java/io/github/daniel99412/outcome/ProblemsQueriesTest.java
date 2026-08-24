package io.github.daniel99412.outcome;

import io.github.daniel99412.outcome.problem.Problem;
import io.github.daniel99412.outcome.problem.ProblemType;
import io.github.daniel99412.outcome.problem.Problems;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProblemsQueriesTest {

    private static final Problem A = Problem.validation("DUP", "first dup");
    private static final Problem B = Problem.notFound("UNIQUE", "single");
    private static final Problem C = Problem.validation("DUP", "second dup");
    private static final Problem D = Problem.internal("OTHER", "internal");

    private static Problems sample() {
        return new Problems(List.of(A, B, C, D));
    }

    @Nested
    class HasCode {

        @Test
        void trueWhenAnyProblemCarriesTheCode() {
            assertTrue(sample().hasCode("UNIQUE"));
            assertTrue(sample().hasCode("DUP"));
        }

        @Test
        void falseWhenNoProblemCarriesTheCode() {
            assertFalse(sample().hasCode("MISSING"));
        }

        @Test
        void nullCodeIsRejected() {
            assertThrows(NullPointerException.class, () -> sample().hasCode(null));
        }
    }

    @Nested
    class ByCode {

        @Test
        void returnsEveryMatchInOrder() {
            List<Problem> matches = sample().byCode("DUP");

            assertEquals(2, matches.size());
            assertEquals(List.of("first dup", "second dup"),
                    matches.stream().map(Problem::description).toList());
        }

        @Test
        void returnsEmptyListWhenNothingMatches() {
            assertTrue(sample().byCode("MISSING").isEmpty());
        }

        @Test
        void returnedListIsImmutable() {
            List<Problem> matches = sample().byCode("DUP");

            assertThrows(UnsupportedOperationException.class,
                    () -> matches.add(Problem.conflict("X", "y")));
        }

        @Test
        void nullCodeIsRejected() {
            assertThrows(NullPointerException.class, () -> sample().byCode(null));
        }
    }

    @Nested
    class ByType {

        @Test
        void returnsOnlyProblemsOfTheType() {
            List<Problem> validations = sample().byType(ProblemType.VALIDATION);

            assertEquals(2, validations.size());
            assertTrue(validations.stream().allMatch(p -> p.type() == ProblemType.VALIDATION));
        }

        @Test
        void returnsEmptyListWhenNoTypeMatches() {
            assertTrue(sample().byType(ProblemType.UNAUTHORIZED).isEmpty());
        }

        @Test
        void returnedListIsImmutable() {
            List<Problem> internals = sample().byType(ProblemType.INTERNAL);

            assertThrows(UnsupportedOperationException.class, internals::clear);
        }

        @Test
        void nullTypeIsRejected() {
            assertThrows(NullPointerException.class, () -> sample().byType(null));
        }
    }
}
