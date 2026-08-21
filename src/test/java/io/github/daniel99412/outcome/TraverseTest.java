package io.github.daniel99412.outcome;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static io.github.daniel99412.outcome.TestOutcomes.failure;
import static io.github.daniel99412.outcome.TestOutcomes.problem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraverseTest {

    @Test
    void mapsEveryElementOnSuccess() {
        Outcome<List<Integer>> result = Outcome.traverse(
                List.of("a", "bb", "ccc"),
                s -> Outcome.success(s.length()));

        assertEquals(new Success<>(List.of(1, 2, 3)), result);
    }

    @Test
    void accumulatesProblemsFromAllFailures() {
        Outcome<List<String>> result = Outcome.traverse(
                List.of("ok", "bad", "also-bad"),
                s -> s.equals("ok") ? Outcome.success(s) : failure(s));

        assertTrue(result.isFailure());
        assertEquals(
                TestOutcomes.failure("bad", "also-bad"),
                result);
    }

    @Test
    void mapperRunsForEveryElementEvenAfterFailures() {
        AtomicInteger invocations = new AtomicInteger(0);

        Outcome.traverse(
                List.of("1", "2", "3", "4"),
                s -> {
                    invocations.incrementAndGet();
                    return failure("E" + s);
                });

        assertEquals(4, invocations.get());
    }

    @Test
    void emptySourceProducesEmptySuccess() {
        Outcome<List<String>> result = Outcome.traverse(List.<String>of(), Outcome::success);

        assertEquals(new Success<>(List.of()), result);
    }

    @Test
    void rejectsNullSource() {
        assertThrows(NullPointerException.class,
                () -> Outcome.traverse(null, (Function<String, Outcome<String>>) null));
    }

    @Test
    void rejectsNullMapper() {
        assertThrows(NullPointerException.class,
                () -> Outcome.traverse(List.of("a"), null));
    }

    @Test
    void rejectsNullMappedResult() {
        assertThrows(NullPointerException.class,
                () -> Outcome.traverse(List.of("a"), s -> null));
    }

    @Test
    void preservesOrderOfValuesAndProblems() {
        Outcome<List<Integer>> result = Outcome.traverse(
                List.of(1, 2, 3, 4),
                n -> {
                    if (n % 2 == 0) {
                        return Outcome.<Integer>failure(problem("E" + n));
                    }
                    return Outcome.success(n);
                });

        assertTrue(result.isFailure());
        assertEquals(
                List.of("E2", "E4"),
                ((Failure<List<Integer>>) result).problems().all().stream()
                        .map(p -> p.code())
                        .toList());
    }

    @Test
    void mixedSuccessAndFailureKeepsValuesAndProblemsSeparate() {
        Outcome<List<String>> ok = Outcome.traverse(
                List.of("x"),
                s -> s.equals("x") ? Outcome.success("X") : failure("EX"));

        assertEquals(new Success<>(List.of("X")), ok);

        Outcome<List<String>> bad = Outcome.traverse(
                List.of("1", "2"),
                s -> failure("E" + s));

        assertTrue(bad.isFailure());
        Failure<List<String>> f = (Failure<List<String>>) bad;
        assertEquals(2, f.problems().size());
        assertEquals("E1", f.problems().first().code());
        assertEquals("E2", f.problems().all().get(1).code());
    }
}
