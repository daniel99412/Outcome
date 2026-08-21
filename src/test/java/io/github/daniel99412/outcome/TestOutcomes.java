package io.github.daniel99412.outcome;

import io.github.daniel99412.outcome.error.Error;
import io.github.daniel99412.outcome.error.ErrorType;
import io.github.daniel99412.outcome.error.Errors;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Test utilities for building outcomes and errors.
 */
final class TestOutcomes {

    private TestOutcomes() {
    }

    static Error error(String code) {
        return new Error(code, "description of " + code, ErrorType.INTERNAL, Map.of(), null);
    }

    static Error error(String code, ErrorType type) {
        return new Error(code, "description of " + code, type, Map.of(), null);
    }

    static Error error(String code, String description, ErrorType type) {
        return new Error(code, description, type, Map.of(), null);
    }

    static Failure<String> failure(String... codes) {
        List<Error> errors = Arrays.stream(codes)
                .map(TestOutcomes::error)
                .toList();
        return new Failure<>(new Errors(errors));
    }
}
