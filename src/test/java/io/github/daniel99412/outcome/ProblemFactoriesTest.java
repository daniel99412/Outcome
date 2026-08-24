package io.github.daniel99412.outcome;

import io.github.daniel99412.outcome.problem.Problem;
import io.github.daniel99412.outcome.problem.ProblemType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProblemFactoriesTest {

    @Test
    void genericOfBuildsProblemWithEmptyMetadataAndNoCause() {
        Problem problem = Problem.of("CODE", "desc", ProblemType.CONFLICT);

        assertEquals("CODE", problem.code());
        assertEquals("desc", problem.description());
        assertEquals(ProblemType.CONFLICT, problem.type());
        assertEquals(Map.of(), problem.metadata());
        assertNull(problem.cause());
    }

    @Test
    void everyTypedFactoryMapsToItsProblemType() {
        assertEquals(ProblemType.VALIDATION,
                Problem.validation("C", "d").type());
        assertEquals(ProblemType.NOT_FOUND,
                Problem.notFound("C", "d").type());
        assertEquals(ProblemType.CONFLICT,
                Problem.conflict("C", "d").type());
        assertEquals(ProblemType.UNAUTHORIZED,
                Problem.unauthorized("C", "d").type());
        assertEquals(ProblemType.FORBIDDEN,
                Problem.forbidden("C", "d").type());
        assertEquals(ProblemType.DEPENDENCY,
                Problem.dependency("C", "d").type());
        assertEquals(ProblemType.TIMEOUT,
                Problem.timeout("C", "d").type());
        assertEquals(ProblemType.UNAVAILABLE,
                Problem.unavailable("C", "d").type());
        assertEquals(ProblemType.INTERNAL,
                Problem.internal("C", "d").type());
    }

    @Test
    void typedFactoriesCarryCodeAndDescription() {
        Problem problem = Problem.notFound("USER_NOT_FOUND", "no such user");

        assertEquals("USER_NOT_FOUND", problem.code());
        assertEquals("no such user", problem.description());
        assertEquals(Map.of(), problem.metadata());
        assertNull(problem.cause());
    }

    @Test
    void factoriesRejectNullArguments() {
        assertThrows(NullPointerException.class, () -> Problem.of(null, "d", ProblemType.INTERNAL));
        assertThrows(NullPointerException.class, () -> Problem.of("c", null, ProblemType.INTERNAL));
        assertThrows(NullPointerException.class, () -> Problem.of("c", "d", null));
        assertThrows(NullPointerException.class, () -> Problem.validation(null, "d"));
        assertThrows(NullPointerException.class, () -> Problem.notFound("c", null));
    }

    @Test
    void factoriesRejectBlankCodeAndDescription() {
        assertThrows(IllegalArgumentException.class, () -> Problem.validation("", "d"));
        assertThrows(IllegalArgumentException.class, () -> Problem.notFound("c", " "));
    }

    @Test
    void typedFactoryResultIsUsableWithMetadataAndCauseEnrichment() {
        RuntimeException cause = new RuntimeException();
        Problem base = Problem.timeout("DB_TIMEOUT", "slow");
        Problem enriched = base.withCause(cause).withMetadata("attempt", 2);

        assertEquals(ProblemType.TIMEOUT, enriched.type());
        assertEquals(cause, enriched.cause());
        assertEquals(2, enriched.metadata().get("attempt"));
    }
}
