package io.github.daniel99412.outcome;

import io.github.daniel99412.outcome.problem.Problem;
import io.github.daniel99412.outcome.problem.ProblemType;
import io.github.daniel99412.outcome.problem.Problems;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Test utilities for building outcomes and problems.
 */
final class TestOutcomes {

    private TestOutcomes() {
    }

    static Problem problem(String code) {
        return new Problem(code, "description of " + code, ProblemType.INTERNAL, Map.of(), null);
    }

    static Problem problem(String code, ProblemType type) {
        return new Problem(code, "description of " + code, type, Map.of(), null);
    }

    static Problem problem(String code, String description, ProblemType type) {
        return new Problem(code, description, type, Map.of(), null);
    }

    static Failure<String> failure(String... codes) {
        List<Problem> problems = Arrays.stream(codes)
                .map(TestOutcomes::problem)
                .toList();
        return new Failure<>(new Problems(problems));
    }
}
