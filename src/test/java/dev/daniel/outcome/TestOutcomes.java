package dev.daniel.outcome;

import dev.daniel.outcome.error.Error;
import dev.daniel.outcome.error.ErrorType;
import dev.daniel.outcome.error.Errors;

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
