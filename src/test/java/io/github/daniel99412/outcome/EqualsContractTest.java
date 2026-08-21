package io.github.daniel99412.outcome;

import io.github.daniel99412.outcome.problem.Problem;
import io.github.daniel99412.outcome.problem.ProblemType;
import io.github.daniel99412.outcome.problem.Problems;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/**
 * Contract tests for equals/hashCode — guarantees library behaves correctly in Sets, Maps and records.
 */
class EqualsContractTest {

    private static Problem err(String code, String description, ProblemType type, Map<String, Object> meta, Throwable cause) {
        return new Problem(code, description, type, meta, cause);
    }

    @Test
    @DisplayName("Problem equals/hashCode ignores cause (semantic identity)")
    void problemEqualsIgnoresCause() {
        Problem red = new Problem("CODE_A", "desc A", ProblemType.INTERNAL, Map.of("k", "v"), new RuntimeException("red cause"));
        Problem black = new Problem("CODE_B", "desc B", ProblemType.NOT_FOUND, Map.of("k2", "v2"), new IllegalStateException("black cause"));

        EqualsVerifier.forClass(Problem.class)
                .withPrefabValues(Problem.class, red, black)
                .withIgnoredFields("cause")
                .suppress(Warning.ALL_FIELDS_SHOULD_BE_USED) // cause intentionally ignored per javadoc Problem.java:9
                .withNonnullFields("code", "description", "type", "metadata")
                .verify();
    }

    @Test
    @DisplayName("Problems equals/hashCode based on ordered list")
    void problemsEqualsHashCode() {
        Problem a1 = new Problem("A", "desc A", ProblemType.INTERNAL, Map.of(), null);
        Problem b1 = new Problem("B", "desc B", ProblemType.NOT_FOUND, Map.of("k", "v"), null);
        Problem a2 = new Problem("C", "desc C", ProblemType.VALIDATION, Map.of(), new RuntimeException());
        Problem b2 = new Problem("D", "desc D", ProblemType.CONFLICT, Map.of(), null);

        Problems red = new Problems(List.of(a1));
        Problems black = new Problems(List.of(b1, a2));

        // prefab for Problem already needed inside Problems
        EqualsVerifier.forClass(Problems.class)
                .withPrefabValues(Problem.class, a1, b1)
                .withPrefabValues(Problems.class, red, black)
                .withNonnullFields("problems")
                .suppress(Warning.SURROGATE_KEY)
                .verify();
    }

    @Test
    @DisplayName("Success record equals/hashCode")
    void successEqualsHashCode() {
        EqualsVerifier.forClass(Success.class)
                .withPrefabValues(Success.class, new Success<>("red"), new Success<>("black"))
                .withNonnullFields("value")
                .suppress(Warning.STRICT_HASHCODE)
                .verify();
    }

    @Test
    @DisplayName("Failure record equals/hashCode delegates to Problems")
    void failureEqualsHashCode() {
        Problem a = new Problem("A", "desc A", ProblemType.INTERNAL, Map.of(), null);
        Problem b = new Problem("B", "desc B", ProblemType.NOT_FOUND, Map.of(), null);

        Problems problemsRed = new Problems(List.of(a));
        Problems problemsBlack = new Problems(List.of(b));

        Failure<String> red = new Failure<>(problemsRed);
        Failure<String> black = new Failure<>(problemsBlack);

        EqualsVerifier.forClass(Failure.class)
                .withPrefabValues(Problem.class, a, b)
                .withPrefabValues(Problems.class, problemsRed, problemsBlack)
                .withPrefabValues(Failure.class, red, black)
                .withNonnullFields("problems")
                .suppress(Warning.STRICT_HASHCODE)
                .verify();
    }

    @Test
    @DisplayName("Failure equality is semantic via Problems, not reference")
    void failureSemanticEqualitySanity() {
        // Sanity check that our EqualsVerifier contract matches actual library fix in Problems.java:106
        Failure<String> f1 = new Failure<>(new Problems(List.of(new Problem("E1", "desc", ProblemType.INTERNAL, Map.of(), null))));
        Failure<String> f2 = new Failure<>(new Problems(List.of(new Problem("E1", "desc", ProblemType.INTERNAL, Map.of(), null))));
        // Should be equal now that Problems implements equals/hashCode
        org.junit.jupiter.api.Assertions.assertEquals(f1, f2);
        org.junit.jupiter.api.Assertions.assertEquals(f1.hashCode(), f2.hashCode());
        org.junit.jupiter.api.Assertions.assertEquals(f1.problems(), f2.problems());
    }
}
