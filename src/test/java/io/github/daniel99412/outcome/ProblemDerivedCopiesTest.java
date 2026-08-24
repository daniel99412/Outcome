package io.github.daniel99412.outcome;

import io.github.daniel99412.outcome.problem.Problem;
import io.github.daniel99412.outcome.problem.ProblemType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProblemDerivedCopiesTest {

    private static final RuntimeException CAUSE = new RuntimeException("root");

    @Test
    void withCauseAttachesDiagnosticCause() {
        Problem base = Problem.timeout("DB_TIMEOUT", "slow");

        Problem enriched = base.withCause(CAUSE);

        assertSame(CAUSE, enriched.cause());
        assertEquals("DB_TIMEOUT", enriched.code());
        assertNull(base.cause());
    }

    @Test
    void withCauseReplacesPreviousCause() {
        Problem first = Problem.internal("X", "y").withCause(new IllegalStateException("a"));
        RuntimeException second = new RuntimeException("b");

        assertSame(second, first.withCause(second).cause());
    }

    @Test
    void withCauseDoesNotAffectSemanticEquality() {
        Problem without = Problem.internal("X", "y");
        Problem with = without.withCause(CAUSE);

        assertEquals(without, with);
        assertEquals(without.hashCode(), with.hashCode());
    }

    @Test
    void withCauseRejectsNullExplicitly() {
        NullPointerException thrown = assertThrows(NullPointerException.class,
                () -> Problem.internal("X", "y").withCause(null));
        assertEquals("cause cannot be null", thrown.getMessage());
    }

    @Test
    void singleEntryWithMetadataMergesPreservingExistingKeys() {
        Problem base = Problem.dependency("DOWN", "x").withMetadata("attempt", 1);

        Problem merged = base.withMetadata("queryId", "q-42");

        assertEquals(1, merged.metadata().get("attempt"));
        assertEquals("q-42", merged.metadata().get("queryId"));
        assertNotEquals(base, merged);
    }

    @Test
    void repeatedKeyIsReplacedByNewValue() {
        Problem base = Problem.internal("X", "y").withMetadata("k", "old");

        Problem replaced = base.withMetadata("k", "new");

        assertEquals("new", replaced.metadata().get("k"));
    }

    @Test
    void mapVariantOfWithMetadataMergesWithoutSilentReplacement() {
        Map<String, Object> existing = Map.of("keep", 1);
        Problem base = new Problem("X", "y", ProblemType.INTERNAL, existing, null);

        Problem merged = base.withMetadata(Map.of("extra", 2));

        assertEquals(1, merged.metadata().get("keep"));
        assertEquals(2, merged.metadata().get("extra"));
    }

    @Test
    void emptyExtraMapKeepsCurrentMetadata() {
        Problem base = Problem.conflict("C", "d").withMetadata("k", 1);

        Problem same = base.withMetadata(Map.of());

        assertEquals(base.metadata(), same.metadata());
        assertNotEquals(System.identityHashCode(base), System.identityHashCode(same));
    }

    @Test
    void nullArgumentsAreRejected() {
        Problem base = Problem.internal("X", "y");

        assertThrows(NullPointerException.class, () -> base.withMetadata((String) null, 1));
        assertThrows(NullPointerException.class, () -> base.withMetadata("k", null));
        assertThrows(NullPointerException.class, () -> base.withMetadata((Map<String, Object>) null));
    }

    @Test
    void mapWithNullKeyOrValueIsRejected() {
        Problem base = Problem.internal("X", "y");

        Map<String, Object> nullKey = new HashMap<>();
        nullKey.put(null, 1);
        assertThrows(NullPointerException.class, () -> base.withMetadata(nullKey));

        Map<String, Object> nullValue = new HashMap<>();
        nullValue.put("k", null);
        assertThrows(NullPointerException.class, () -> base.withMetadata(nullValue));
    }

    @Test
    void originalInstanceNeverMutatedByAnyCopy() {
        Problem base = Problem.validation("V", "desc");

        base.withCause(CAUSE);
        base.withMetadata("a", 1);
        base.withMetadata(Map.of("b", 2));

        assertEquals("V", base.code());
        assertEquals("desc", base.description());
        assertEquals(Map.of(), base.metadata());
        assertNull(base.cause());
        assertNotNull(base);
    }
}
