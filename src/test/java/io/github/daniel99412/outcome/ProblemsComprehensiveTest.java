package io.github.daniel99412.outcome;

import io.github.daniel99412.outcome.problem.Problem;
import io.github.daniel99412.outcome.problem.ProblemType;
import io.github.daniel99412.outcome.problem.Problems;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProblemsComprehensiveTest {

    private static Problem err(String code) {
        return new Problem(code, "desc " + code, ProblemType.INTERNAL, Map.of(), null);
    }

    private static Problem err(String code, ProblemType type) {
        return new Problem(code, "desc " + code, type, Map.of(), null);
    }

    @Nested
    @DisplayName("Construction invariants")
    class Construction {

        @Test
        void nullCollectionThrowsNPE() {
            assertThrows(NullPointerException.class, () -> new Problems(null));
        }

        @Test
        void emptyCollectionThrowsIAE() {
            assertThrows(IllegalArgumentException.class, () -> new Problems(List.of()));
            assertThrows(IllegalArgumentException.class, () -> new Problems(new ArrayList<>()));
        }

        @Test
        void nullElementThrowsNPE() {
            List<Problem> list = new ArrayList<>();
            list.add(err("A"));
            list.add(null);
            assertThrows(NullPointerException.class, () -> new Problems(list));
        }

        @Test
        void singleProblemIsValid() {
            Problems e = new Problems(List.of(err("A")));
            assertEquals(1, e.size());
            assertEquals("A", e.first().code());
        }

        @Test
        void manyProblemsPreservesOrderAndAllowsDuplicates() {
            // duplicates must NOT be deduplicated
            Problem a1 = err("A");
            Problem a2 = err("A"); // equal but distinct instance, same semantic
            Problems problems = new Problems(List.of(a1, err("B"), a1, a2));
            assertEquals(4, problems.size());
            assertEquals(List.of("A", "B", "A", "A"),
                    problems.all().stream().map(Problem::code).toList());
        }

        @Test
        void acceptsAnyIterableNotJustList() {
            Iterable<Problem> iterable = () -> Arrays.asList(err("A"), err("B")).iterator();
            Problems e = new Problems(iterable);
            assertEquals(2, e.size());
        }

        @Test
        void preservesInsertionOrderForLargeCollection() {
            List<Problem> src = new ArrayList<>();
            for (int i = 0; i < 50; i++) src.add(err("E" + i));
            Problems e = new Problems(src);
            for (int i = 0; i < 50; i++) assertEquals("E" + i, e.all().get(i).code());
        }
    }

    @Nested
    @DisplayName("Snapshot immutability")
    class Immutability {

        @Test
        void snapshotIsolatedFromSourceListMutation() {
            List<Problem> source = new ArrayList<>(List.of(err("A")));
            Problems problems = new Problems(source);
            source.add(err("B"));
            source.set(0, err("C"));
            source.clear();
            assertEquals(1, problems.size());
            assertEquals("A", problems.first().code());
        }

        @Test
        void allReturnsUnmodifiableList() {
            Problems problems = new Problems(List.of(err("A"), err("B")));
            List<Problem> all = problems.all();
            assertThrows(UnsupportedOperationException.class, () -> all.add(err("X")));
            assertThrows(UnsupportedOperationException.class, () -> all.remove(0));
            assertThrows(UnsupportedOperationException.class, () -> all.set(0, err("Y")));
            assertThrows(UnsupportedOperationException.class, () -> all.clear());
        }

        @Test
        void allReturnsSameImmutableViewConsistently() {
            Problems problems = new Problems(List.of(err("A")));
            List<Problem> a1 = problems.all();
            List<Problem> a2 = problems.all();
            // Both are unmodifiable and equal
            assertEquals(a1, a2);
            assertThrows(UnsupportedOperationException.class, () -> a1.add(err("X")));
        }

        @Test
        void firstReturnsFirstElementAndIsConsistent() {
            Problems problems = new Problems(List.of(err("FIRST"), err("SECOND"), err("THIRD")));
            assertEquals("FIRST", problems.first().code());
            assertEquals(problems.all().get(0), problems.first());
        }

        @Test
        void sizeIsConsistentWithAll() {
            Problems problems = new Problems(List.of(err("A"), err("B"), err("C")));
            assertEquals(problems.all().size(), problems.size());
        }
    }

    @Nested
    @DisplayName("contains — semantic equality")
    class Contains {

        @Test
        void containsFindsSemanticallyEqualProblemIgnoringCause() {
            Problem withCause = new Problem("CODE", "desc", ProblemType.INTERNAL, Map.of("k", "v"), new RuntimeException("a"));
            Problem sameButDifferentCause = new Problem("CODE", "desc", ProblemType.INTERNAL, Map.of("k", "v"), new RuntimeException("b"));
            Problems problems = new Problems(List.of(withCause));
            assertTrue(problems.contains(sameButDifferentCause));
            assertTrue(problems.contains(withCause));
        }

        @Test
        void containsReturnsFalseForNonExisting() {
            Problems problems = new Problems(List.of(err("A"), err("B")));
            assertFalse(problems.contains(err("Z")));
        }

        @Test
        void containsReturnsFalseWhenMetadataDiffers() {
            Problems problems = new Problems(List.of(new Problem("CODE", "desc", ProblemType.INTERNAL, Map.of("k", "1"), null)));
            assertFalse(problems.contains(new Problem("CODE", "desc", ProblemType.INTERNAL, Map.of("k", "2"), null)));
        }

        @Test
        void containsNullThrowsNPE() {
            Problems problems = new Problems(List.of(err("A")));
            assertThrows(NullPointerException.class, () -> problems.contains(null));
        }

        @Test
        void containsChecksAllElements() {
            Problems problems = new Problems(List.of(err("A"), err("B"), err("C")));
            assertTrue(problems.contains(err("C")));
            assertTrue(problems.contains(err("A")));
            assertTrue(problems.contains(err("B")));
        }
    }

    @Nested
    @DisplayName("map")
    class MapTests {

        @Test
        void mapTransformsEachProblemPreservingOrder() {
            Problems original = new Problems(List.of(err("A"), err("B"), err("C")));
            Problems mapped = original.map(e -> new Problem(e.code().toLowerCase(), e.description(), e.type(), e.metadata(), e.cause()));
            assertEquals(List.of("a", "b", "c"), mapped.all().stream().map(Problem::code).toList());
        }

        @Test
        void mapCreatesNewInstanceAndDoesNotMutateOriginal() {
            Problems original = new Problems(List.of(err("A")));
            Problems mapped = original.map(e -> err("B"));
            assertNotSame(original, mapped);
            assertEquals("A", original.first().code());
            assertEquals("B", mapped.first().code());
        }

        @Test
        void mapWithIdentityCreatesNewInstanceWithEqualContent() {
            Problems original = new Problems(List.of(err("A"), err("B")));
            Problems mapped = original.map(e -> e);
            assertNotSame(original, mapped);
            assertEquals(original.all(), mapped.all());
        }

        @Test
        void mapNullMapperThrowsNPE() {
            Problems original = new Problems(List.of(err("A")));
            assertThrows(NullPointerException.class, () -> original.map(null));
        }

        @Test
        void mapMapperReturningNullThrowsNPE() {
            Problems original = new Problems(List.of(err("A"), err("B")));
            assertThrows(NullPointerException.class, () -> original.map(e -> null));
        }

        @Test
        void mapMapperThrowingPropagates() {
            Problems original = new Problems(List.of(err("A")));
            assertThrows(IllegalStateException.class, () -> original.map(e -> { throw new IllegalStateException("boom"); }));
        }

        @Test
        void mapResultIsAlsoImmutable() {
            Problems original = new Problems(List.of(err("A")));
            Problems mapped = original.map(e -> err("B"));
            assertThrows(UnsupportedOperationException.class, () -> mapped.all().add(err("C")));
        }

        @Test
        void mapCanChangeProblemType() {
            Problems original = new Problems(List.of(err("A", ProblemType.VALIDATION)));
            Problems mapped = original.map(e -> new Problem(e.code(), e.description(), ProblemType.NOT_FOUND, e.metadata(), e.cause()));
            assertEquals(ProblemType.NOT_FOUND, mapped.first().type());
            assertEquals(ProblemType.VALIDATION, original.first().type());
        }

        @Test
        void mapOnSingleElement() {
            Problems original = new Problems(List.of(err("ONLY")));
            Problems mapped = original.map(e -> err("MAPPED"));
            assertEquals(1, mapped.size());
            assertEquals("MAPPED", mapped.first().code());
        }
    }

    @Nested
    @DisplayName("Iterable")
    class IterableTests {

        @Test
        void iteratorInOrder() {
            Problems problems = new Problems(List.of(err("A"), err("B"), err("C")));
            List<String> codes = new ArrayList<>();
            for (Problem e : problems) codes.add(e.code());
            assertEquals(List.of("A", "B", "C"), codes);
        }

        @Test
        void iteratorHasNextAndNextConsistent() {
            Problems problems = new Problems(List.of(err("A"), err("B")));
            Iterator<Problem> it = problems.iterator();
            assertTrue(it.hasNext());
            assertEquals("A", it.next().code());
            assertTrue(it.hasNext());
            assertEquals("B", it.next().code());
            assertFalse(it.hasNext());
        }

        @Test
        void iteratorRemoveThrowsUnsupportedOperation() {
            Problems problems = new Problems(List.of(err("A"), err("B")));
            Iterator<Problem> it = problems.iterator();
            it.next();
            assertThrows(UnsupportedOperationException.class, it::remove);
        }

        @Test
        void forEachWorks() {
            Problems problems = new Problems(List.of(err("A"), err("B")));
            List<String> collected = new ArrayList<>();
            problems.forEach(e -> collected.add(e.code()));
            assertEquals(List.of("A", "B"), collected);
        }

        @Test
        void spliteratorPreservesOrder() {
            Problems problems = new Problems(List.of(err("X"), err("Y")));
            List<String> codes = new ArrayList<>();
            problems.spliterator().forEachRemaining(e -> codes.add(e.code()));
            assertEquals(List.of("X", "Y"), codes);
        }
    }

    @Nested
    @DisplayName("toString and misc")
    class Misc {

        @Test
        void toStringContainsAllCodes() {
            Problems problems = new Problems(List.of(err("A"), err("B")));
            String s = problems.toString();
            assertTrue(s.contains("A"));
            assertTrue(s.contains("B"));
            assertTrue(s.startsWith("Problems"));
        }

        @Test
        void toStringForSingleProblem() {
            Problems problems = new Problems(List.of(err("ONLY")));
            assertTrue(problems.toString().contains("ONLY"));
        }
    }
}
