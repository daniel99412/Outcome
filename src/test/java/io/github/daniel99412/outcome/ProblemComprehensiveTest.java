package io.github.daniel99412.outcome;

import io.github.daniel99412.outcome.problem.Problem;
import io.github.daniel99412.outcome.problem.ProblemType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProblemComprehensiveTest {

    @Nested
    @DisplayName("Construction and validation")
    class Construction {

        @Test
        void validConstructionWithAllFields() {
            Throwable cause = new IllegalStateException("root");
            Map<String, Object> meta = Map.of("userId", 42, "retry", true);
            Problem e = new Problem("USER_NOT_FOUND", "User 42 not found", ProblemType.NOT_FOUND, meta, cause);
            assertEquals("USER_NOT_FOUND", e.code());
            assertEquals("User 42 not found", e.description());
            assertEquals(ProblemType.NOT_FOUND, e.type());
            assertEquals(meta, e.metadata());
            assertSame(cause, e.cause());
        }

        @Test
        void minimalConstructionWithNullMetadataAndNullCause() {
            Problem e = new Problem("CODE", "description", ProblemType.INTERNAL, null, null);
            assertTrue(e.metadata().isEmpty());
            assertNull(e.cause());
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "   ", "\t", "\n"})
        void blankCodeRejected(String blank) {
            assertThrows(IllegalArgumentException.class,
                    () -> new Problem(blank, "desc", ProblemType.INTERNAL, Map.of(), null));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "   ", "\t", "\n"})
        void blankDescriptionRejected(String blank) {
            assertThrows(IllegalArgumentException.class,
                    () -> new Problem("CODE", blank, ProblemType.INTERNAL, Map.of(), null));
        }

        @Test
        void nullCodeThrowsNPE() {
            assertThrows(NullPointerException.class, () -> new Problem(null, "desc", ProblemType.INTERNAL, Map.of(), null));
        }

        @Test
        void nullDescriptionThrowsNPE() {
            assertThrows(NullPointerException.class, () -> new Problem("CODE", null, ProblemType.INTERNAL, Map.of(), null));
        }

        @Test
        void nullTypeThrowsNPE() {
            assertThrows(NullPointerException.class, () -> new Problem("CODE", "desc", null, Map.of(), null));
        }

        @Test
        void codeWithLeadingTrailingSpacesIsAllowedIfNotBlank() {
            // isBlank only rejects fully blank; surrounding spaces are allowed per current impl
            Problem e = new Problem(" CODE ", " desc ", ProblemType.VALIDATION, null, null);
            assertEquals(" CODE ", e.code());
            assertEquals(" desc ", e.description());
        }

        @Test
        void metadataNullNormalizedToEmptyImmutableMap() {
            Problem e = new Problem("CODE", "desc", ProblemType.INTERNAL, null, null);
            assertEquals(Map.of(), e.metadata());
            assertThrows(UnsupportedOperationException.class, () -> e.metadata().put("k", "v"));
        }

        @Test
        void metadataIsCopiedDefensively() {
            Map<String, Object> mutable = new HashMap<>();
            mutable.put("a", "1");
            Problem e = new Problem("CODE", "desc", ProblemType.INTERNAL, mutable, null);
            mutable.put("b", "2");
            assertEquals(1, e.metadata().size());
            assertFalse(e.metadata().containsKey("b"));
        }

        @Test
        void metadataIsImmutableEvenIfCallerPassesMutableMap() {
            Map<String, Object> mutable = new HashMap<>();
            mutable.put("k", "v");
            Problem e = new Problem("CODE", "desc", ProblemType.INTERNAL, mutable, null);
            assertThrows(UnsupportedOperationException.class, () -> e.metadata().put("x", "y"));
            assertThrows(UnsupportedOperationException.class, () -> e.metadata().remove("k"));
        }

        @Test
        void emptyMapMetadataIsImmutable() {
            Problem e = new Problem("CODE", "desc", ProblemType.INTERNAL, Map.of(), null);
            assertThrows(UnsupportedOperationException.class, () -> e.metadata().put("k", "v"));
        }

        @Test
        void causeCanBeAnyThrowableAndIsPreserved() {
            Exception ex1 = new RuntimeException("r");
            Problem e1 = new Problem("CODE", "desc", ProblemType.INTERNAL, null, ex1);
            assertSame(ex1, e1.cause());

            Problem e2 = new Problem("CODE", "desc", ProblemType.INTERNAL, Map.of(), null);
            assertNull(e2.cause());

            Exception ex3 = new IllegalArgumentException("inner");
            Problem e3 = new Problem("CODE", "desc", ProblemType.DEPENDENCY, null, ex3);
            assertSame(ex3, e3.cause());
            assertEquals("inner", e3.cause().getMessage());
        }

        @ParameterizedTest
        @EnumSource(ProblemType.class)
        void allProblemTypesCanBeUsed(ProblemType type) {
            Problem e = new Problem("CODE", "desc", type, null, null);
            assertEquals(type, e.type());
        }

        @Test
        void allProblemTypesCountIsNine() {
            assertEquals(9, ProblemType.values().length);
        }
    }

    @Nested
    @DisplayName("Equality and hashCode — semantic identity ignores cause")
    class Equality {

        @Test
        void equalsIgnoresCauseDifferences() {
            Problem a = new Problem("CODE", "desc", ProblemType.INTERNAL, Map.of("k", "v"), new RuntimeException("a"));
            Problem b = new Problem("CODE", "desc", ProblemType.INTERNAL, Map.of("k", "v"), new RuntimeException("b"));
            Problem c = new Problem("CODE", "desc", ProblemType.INTERNAL, Map.of("k", "v"), null);
            assertEquals(a, b);
            assertEquals(a, c);
            assertEquals(b, c);
            assertEquals(a.hashCode(), b.hashCode());
            assertEquals(a.hashCode(), c.hashCode());
        }

        @Test
        void equalsConsidersCode() {
            Problem a = new Problem("CODE_A", "desc", ProblemType.INTERNAL, Map.of(), null);
            Problem b = new Problem("CODE_B", "desc", ProblemType.INTERNAL, Map.of(), null);
            assertNotEquals(a, b);
        }

        @Test
        void equalsConsidersDescription() {
            Problem a = new Problem("CODE", "desc A", ProblemType.INTERNAL, Map.of(), null);
            Problem b = new Problem("CODE", "desc B", ProblemType.INTERNAL, Map.of(), null);
            assertNotEquals(a, b);
        }

        @Test
        void equalsConsidersType() {
            Problem a = new Problem("CODE", "desc", ProblemType.NOT_FOUND, Map.of(), null);
            Problem b = new Problem("CODE", "desc", ProblemType.CONFLICT, Map.of(), null);
            assertNotEquals(a, b);
        }

        @Test
        void equalsConsidersMetadata() {
            Problem a = new Problem("CODE", "desc", ProblemType.INTERNAL, Map.of("k", "1"), null);
            Problem b = new Problem("CODE", "desc", ProblemType.INTERNAL, Map.of("k", "2"), null);
            Problem c = new Problem("CODE", "desc", ProblemType.INTERNAL, Map.of("other", "1"), null);
            assertNotEquals(a, b);
            assertNotEquals(a, c);
        }

        @Test
        void equalsAndHashCodeConsistentForEqualObjects() {
            Problem a = new Problem("CODE", "desc", ProblemType.VALIDATION, Map.of("x", 1), new Exception());
            Problem b = new Problem("CODE", "desc", ProblemType.VALIDATION, Map.of("x", 1), null);
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
            // multiple calls consistent
            assertEquals(a.hashCode(), a.hashCode());
        }

        @Test
        void equalsReflexiveSymmetricTransitive() {
            Problem a = new Problem("CODE", "desc", ProblemType.INTERNAL, Map.of("k", "v"), null);
            Problem b = new Problem("CODE", "desc", ProblemType.INTERNAL, Map.of("k", "v"), new RuntimeException());
            Problem c = new Problem("CODE", "desc", ProblemType.INTERNAL, Map.of("k", "v"), null);
            // reflexive
            assertEquals(a, a);
            // symmetric
            assertEquals(a, b);
            assertEquals(b, a);
            // transitive
            assertEquals(a, b);
            assertEquals(b, c);
            assertEquals(a, c);
        }

        @Test
        void notEqualsNullAndDifferentClass() {
            Problem a = new Problem("CODE", "desc", ProblemType.INTERNAL, Map.of(), null);
            assertNotEquals(null, a);
            assertNotEquals("string", a);
            assertNotEquals(a, 123);
        }

        @Test
        void notEqualsWhenMetadataValueDiffersButKeySame() {
            Problem a = new Problem("CODE", "desc", ProblemType.INTERNAL, Map.of("k", "a"), null);
            Problem b = new Problem("CODE", "desc", ProblemType.INTERNAL, Map.of("k", "b"), null);
            assertNotEquals(a, b);
        }

        @Test
        void hashCodeDifferentForNonEqualProblemsIsLikelyButNotRequired() {
            Problem a = new Problem("CODE1", "desc", ProblemType.INTERNAL, Map.of(), null);
            Problem b = new Problem("CODE2", "desc", ProblemType.INTERNAL, Map.of(), null);
            // not strictly required, but should usually differ; we just ensure not throwing
            assertNotEquals(a, b);
            // we don't assert hashCode inequality strictly, just that they are computed
            assertNotNull(a.hashCode());
        }
    }

    @Nested
    @DisplayName("toString")
    class ToString {

        @Test
        void toStringContainsSemanticFields() {
            Problem e = new Problem("MY_CODE", "my description", ProblemType.TIMEOUT, Map.of("k", "v"), new RuntimeException("cause"));
            String s = e.toString();
            assertTrue(s.contains("MY_CODE"));
            assertTrue(s.contains("my description"));
            assertTrue(s.contains("TIMEOUT"));
            assertTrue(s.contains("k"));
            // cause should NOT be in toString per spec
            assertFalse(s.contains("cause"));
            assertFalse(s.contains("RuntimeException"));
        }

        @Test
        void toStringWithEmptyMetadata() {
            Problem e = new Problem("CODE", "desc", ProblemType.INTERNAL, Map.of(), null);
            String s = e.toString();
            assertTrue(s.startsWith("Problem{"));
            assertTrue(s.contains("code='CODE'"));
            assertTrue(s.contains("description='desc'"));
        }

        @Test
        void toStringDoesNotThrowForAllProblemTypes() {
            for (ProblemType t : ProblemType.values()) {
                Problem e = new Problem("CODE", "desc", t, null, null);
                assertNotNull(e.toString());
            }
        }
    }

    @Nested
    @DisplayName("Immutability and record semantics")
    class Immutability {

        @Test
        void recordAccessorsReturnCorrectValues() {
            Map<String, Object> meta = Map.of("k", "v");
            Throwable cause = new Exception("c");
            Problem e = new Problem("CODE", "desc", ProblemType.FORBIDDEN, meta, cause);
            assertEquals("CODE", e.code());
            assertEquals("desc", e.description());
            assertEquals(ProblemType.FORBIDDEN, e.type());
            assertEquals(meta, e.metadata());
            assertSame(cause, e.cause());
        }

        @Test
        void metadataReturnedIsUnmodifiableViewEachTime() {
            Problem e = new Problem("CODE", "desc", ProblemType.INTERNAL, Map.of("k", "v"), null);
            Map<String, Object> m1 = e.metadata();
            Map<String, Object> m2 = e.metadata();
            // both views should be equal and unmodifiable
            assertEquals(m1, m2);
            assertThrows(UnsupportedOperationException.class, () -> m1.put("new", "val"));
        }
    }
}
