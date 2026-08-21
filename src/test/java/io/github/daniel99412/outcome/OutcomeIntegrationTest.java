package io.github.daniel99412.outcome;

import io.github.daniel99412.outcome.problem.Problem;
import io.github.daniel99412.outcome.problem.ProblemType;
import io.github.daniel99412.outcome.problem.Problems;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration tests mimicking real usage of the Outcome library
 * – validation pipelines, problem accumulation, recovery and type transformations.
 */
class OutcomeIntegrationTest {

    private static Problem err(String code, ProblemType type) {
        return new Problem(code, "desc " + code, type, Map.of(), null);
    }

    private static Problem validation(String field) {
        return new Problem("VALIDATION_" + field.toUpperCase(), field + " is invalid", ProblemType.VALIDATION, Map.of("field", field), null);
    }

    // Simulated domain functions
    private Outcome<String> validateName(String name) {
        if (name == null || name.isBlank()) return new Failure<>(new Problems(List.of(validation("name"))));
        if (name.length() < 2) return new Failure<>(new Problems(List.of(validation("name"))));
        return new Success<>(name.trim());
    }

    private Outcome<Integer> validateAge(Integer age) {
        if (age == null) return new Failure<>(new Problems(List.of(validation("age"))));
        if (age < 0 || age > 150) return new Failure<>(new Problems(List.of(validation("age"))));
        return new Success<>(age);
    }

    private Outcome<String> validateEmail(String email) {
        if (email == null || !email.contains("@")) return new Failure<>(new Problems(List.of(validation("email"))));
        return new Success<>(email.toLowerCase());
    }

    record User(String name, int age, String email) {}

    @Test
    @DisplayName("Happy path – all validations succeed")
    void happyPathAllValidationsSucceed() {
        Outcome<String> name = validateName(" Ada ");
        Outcome<Integer> age = validateAge(30);
        Outcome<String> email = validateEmail("ADA@Example.COM");

        // combine manually via sequence for same-type? Here we flatMap chain to build User
        Outcome<User> user = name.flatMap(n ->
                age.flatMap(a ->
                        email.map(e -> new User(n, a, e))));

        assertTrue(user.isSuccess());
        User u = ((Success<User>) user).value();
        assertEquals("Ada", u.name());
        assertEquals(30, u.age());
        assertEquals("ada@example.com", u.email());
    }

    @Test
    @DisplayName("Failure short-circuits flatMap chain")
    void failureShortCircuits() {
        AtomicInteger ageCalled = new AtomicInteger();
        AtomicInteger emailCalled = new AtomicInteger();

        Outcome<String> name = validateName(""); // fails
        Outcome<User> user = name.flatMap(n -> {
            ageCalled.incrementAndGet();
            return validateAge(30).flatMap(a -> {
                emailCalled.incrementAndGet();
                return validateEmail("a@b.com").map(e -> new User(n, a, e));
            });
        });

        assertTrue(user.isFailure());
        assertEquals(0, ageCalled.get());
        assertEquals(0, emailCalled.get());
    }

    @Test
    @DisplayName("Problem accumulation via sequence – multiple fields invalid")
    void problemAccumulationViaSequence() {
        Outcome<String> name = validateName("");       // 1 problem
        Outcome<Integer> age = validateAge(-5);        // 1 problem
        Outcome<String> email = validateEmail("bad");  // 1 problem

        // For accumulation we wrap each as Outcome<Problem> style? Use sequence on a common type
        // Simulate: collect validation outcomes as Outcome<Void> and sequence
        // Instead directly test sequence with boxed outcomes of String and converting to void for accumulation demo
        Outcome<List<String>> nameSeq = Outcome.sequence(List.of(name, validateName(null)));
        assertTrue(nameSeq.isFailure());
        assertEquals(2, ((Failure<List<String>>) nameSeq).problems().size());

        // Full accumulation: age and email separately
        Outcome<List<Integer>> ageSeq = Outcome.sequence(List.of(age, validateAge(null)));
        assertEquals(2, ((Failure<List<Integer>>) ageSeq).problems().size());

        // Mix different types via map to unify: map all to String result then sequence
        List<Outcome<String>> allAsString = List.of(
                name.map(v -> "name:" + v),
                age.map(v -> "age:" + v),
                email.map(v -> "email:" + v)
        );
        Outcome<List<String>> combined = Outcome.sequence(allAsString);
        assertTrue(combined.isFailure());
        // 3 failures -> 3 problems accumulated
        assertEquals(3, ((Failure<List<String>>) combined).problems().size());
    }

    @Test
    @DisplayName("Recover from validation failure with default user")
    void recoverWithDefault() {
        Outcome<User> failing = validateName("").flatMap(n -> validateAge(30).map(a -> new User(n, a, "a@b.com")));
        assertTrue(failing.isFailure());

        Outcome<User> recovered = failing.recover(problems -> new User("Default", 0, "default@example.com"));
        assertTrue(recovered.isSuccess());
        assertEquals("Default", ((Success<User>) recovered).value().name());
    }

    @Test
    @DisplayName("RecoverWith can retry or convert to another failure")
    void recoverWithCanRetry() {
        // First attempt fails
        Outcome<String> attempt1 = validateName("");
        // recoverWith retries with valid input
        Outcome<String> attempt2 = attempt1.recoverWith(problems -> validateName("Bob"));
        assertEquals(new Success<>("Bob"), attempt2);

        // recoverWith to a different failure
        Outcome<String> attempt3 = attempt1.recoverWith(problems -> new Failure<>(new Problems(List.of(err("RETRY_FAIL", ProblemType.INTERNAL)))));
        assertTrue(attempt3.isFailure());
        assertEquals("RETRY_FAIL", ((Failure<String>) attempt3).problems().first().code());
    }

    @Test
    @DisplayName("mapProblem enriches problems with metadata")
    void mapProblemEnrichesMetadata() {
        Outcome<String> failing = validateName("");
        Outcome<String> enriched = failing.mapProblem(e ->
                new Problem(e.code(), e.description(), e.type(), Map.of("field", "name", "severity", "high"), e.cause()));

        assertTrue(enriched.isFailure());
        Problem first = ((Failure<String>) enriched).problems().first();
        assertEquals("high", first.metadata().get("severity"));
        assertEquals("name", first.metadata().get("field"));
    }

    @Test
    @DisplayName("fold as final rendering to HTTP-like response")
    void foldAsRendering() {
        Outcome<User> success = new Success<>(new User("Ada", 30, "ada@example.com"));
        String successResponse = success.fold(
                user -> "200 OK: " + user.name(),
                problems -> "400 Bad Request: " + problems.size() + " problems");
        assertEquals("200 OK: Ada", successResponse);

        Outcome<User> failure = new Failure<>(new Problems(List.of(validation("name"), validation("email"))));
        String failureResponse = failure.fold(
                user -> "200 OK: " + user.name(),
                problems -> "400 Bad Request: " + problems.size() + " problems");
        assertEquals("400 Bad Request: 2 problems", failureResponse);
    }

    @Test
    @DisplayName("peek for logging without altering outcome")
    void peekForLogging() {
        AtomicInteger successLog = new AtomicInteger();
        AtomicInteger failureLog = new AtomicInteger();

        Outcome<String> s = new Success<>("ok");
        Outcome<String> s2 = s.peek(v -> successLog.incrementAndGet())
                               .peekProblem(e -> failureLog.incrementAndGet());
        assertSame(s, s2);
        assertEquals(1, successLog.get());
        assertEquals(0, failureLog.get());

        Outcome<String> f = new Failure<>(new Problems(List.of(err("E1", ProblemType.INTERNAL))));
        Outcome<String> f2 = f.peek(v -> successLog.incrementAndGet())
                               .peekProblem(e -> failureLog.incrementAndGet());
        assertSame(f, f2);
        assertEquals(1, successLog.get()); // unchanged
        assertEquals(1, failureLog.get());
    }

    @Test
    @DisplayName("Chaining map -> flatMap -> mapProblem -> recover covers full pipeline")
    void fullPipeline() {
        // Start success, transform, then simulate failure in flatMap, then enrich problem, then recover
        Outcome<Integer> pipeline = new Success<>(" 42 ")
                .map(String::trim)                         // "42"
                .flatMap(s -> {
                    try {
                        return new Success<>(Integer.parseInt(s));
                    } catch (NumberFormatException ex) {
                        return new Failure<>(new Problems(List.of(
                                new Problem("PARSE_ERROR", "not a number", ProblemType.VALIDATION, Map.of(), ex))));
                    }
                })                                         // 42
                .map(v -> v * 2)                           // 84
                .mapProblem(e -> new Problem("ENRICHED_" + e.code(), e.description(), e.type(), Map.of("step", "parse"), e.cause()));

        assertEquals(new Success<>(84), pipeline);

        // Now a failing pipeline that recovers
        Outcome<Integer> failingPipeline = new Success<>(" not_a_number ")
                .map(String::trim)
                .flatMap(s -> {
                    try {
                        return new Success<>(Integer.parseInt(s));
                    } catch (NumberFormatException ex) {
                        return new Failure<>(new Problems(List.of(
                                new Problem("PARSE_ERROR", "not a number", ProblemType.VALIDATION, Map.of(), ex))));
                    }
                })
                .map(v -> v * 2)
                .recover(problems -> -1);

        assertEquals(new Success<>(-1), failingPipeline);
    }

    @Test
    @DisplayName("Success value immutability – sequence returns immutable list")
    void sequenceImmutabilityInIntegration() {
        Outcome<List<String>> seq = Outcome.sequence(List.of(new Success<>("a"), new Success<>("b")));
        List<String> list = ((Success<List<String>>) seq).value();
        assertThrows(UnsupportedOperationException.class, () -> list.add("c"));
        assertEquals(2, list.size());
    }

    @Test
    @DisplayName("Problems inside Failure are never empty and preserve order across operations")
    void problemsPreserveOrderAcrossOperations() {
        Failure<String> f = new Failure<>(new Problems(List.of(
                err("FIRST", ProblemType.VALIDATION),
                err("SECOND", ProblemType.NOT_FOUND),
                err("THIRD", ProblemType.CONFLICT)
        )));

        Failure<String> mapped = (Failure<String>) f.mapProblem(e ->
                new Problem(e.code() + "_M", e.description(), e.type(), e.metadata(), e.cause()));

        assertEquals(List.of("FIRST_M", "SECOND_M", "THIRD_M"),
                mapped.problems().all().stream().map(Problem::code).toList());
        assertEquals(ProblemType.VALIDATION, mapped.problems().all().get(0).type());
        assertEquals(ProblemType.NOT_FOUND, mapped.problems().all().get(1).type());
        assertEquals(ProblemType.CONFLICT, mapped.problems().all().get(2).type());
    }

    @Test
    @DisplayName("Multiple recover strategies can be chained")
    void multipleRecovers() {
        // First failure, first recover returns another failure, second recover succeeds
        Outcome<String> result = new Failure<String>(new Problems(List.of(err("E1", ProblemType.INTERNAL))))
                .recoverWith(problems -> new Failure<>(new Problems(List.of(err("E2", ProblemType.DEPENDENCY)))))
                .recover(problems -> "finally recovered");

        assertEquals(new Success<>("finally recovered"), result);
    }

    @Test
    @DisplayName("Outcome can be used in collections and streams")
    void outcomeInCollectionsAndStreams() {
        List<Outcome<Integer>> outcomes = List.of(
                new Success<>(1), new Failure<>(new Problems(List.of(err("E1", ProblemType.INTERNAL)))), new Success<>(3));

        long successes = outcomes.stream().filter(Outcome::isSuccess).count();
        long failures = outcomes.stream().filter(Outcome::isFailure).count();
        assertEquals(2, successes);
        assertEquals(1, failures);

        // partition
        int sum = outcomes.stream()
                .filter(Outcome::isSuccess)
                .map(o -> ((Success<Integer>) o).value())
                .mapToInt(Integer::intValue).sum();
        assertEquals(4, sum);
    }
}
