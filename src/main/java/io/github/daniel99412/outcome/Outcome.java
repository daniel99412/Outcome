package io.github.daniel99412.outcome;

import io.github.daniel99412.outcome.problem.Problem;
import io.github.daniel99412.outcome.problem.Problems;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The result of a possibly failing operation.
 * <p>
 * An {@code Outcome} is a sealed, immutable, dependency-free representation of
 * success or failure. It is deliberately small: it is not a substitute for
 * {@link java.util.Optional} and does not aim to be a general functional
 * programming framework.
 *
 * @param <T> the type of the value carried by a successful outcome
 * @see Success
 * @see Failure
 */
public sealed interface Outcome<T>
        permits Success, Failure {

    /**
     * Creates a {@link Success} holding the given value.
     *
     * @param value the successful value; must not be null
     * @param <T>   the type of the value
     * @return a {@code Success} holding the value
     * @throws NullPointerException if the value is null
     */
    static <T> Outcome<T> success(T value) {
        return new Success<>(value);
    }

    /**
     * Creates a {@link Failure} carrying a single {@link Problem}.
     *
     * @param problem the problem to carry; must not be null
     * @param <T>     the type the failure would have carried on success
     * @return a {@code Failure} carrying the given problem
     * @throws NullPointerException if the problem is null
     */
    static <T> Outcome<T> failure(Problem problem) {
        Objects.requireNonNull(problem, "problem cannot be null");
        return new Failure<>(new Problems(List.of(problem)));
    }

    /**
     * Creates a {@link Failure} carrying the given problems.
     *
     * @param problems the problems to carry; must not be null and must not be empty
     * @param <T>      the type the failure would have carried on success
     * @return a {@code Failure} carrying the given problems
     * @throws NullPointerException     if the problems are null or contain null elements
     * @throws IllegalArgumentException if the problems are empty
     */
    static <T> Outcome<T> failure(Problems problems) {
        Objects.requireNonNull(problems, "problems cannot be null");
        return new Failure<>(problems);
    }

    /**
     * Creates a {@link Failure} carrying the given problems, in order.
     *
     * @param problems the problems to carry; must not be null, must not be empty,
     *                 and must not contain null elements
     * @param <T>      the type the failure would have carried on success
     * @return a {@code Failure} carrying the given problems
     * @throws NullPointerException     if the array, or any of its elements, is null
     * @throws IllegalArgumentException if the array is empty
     */
    static <T> Outcome<T> failure(Problem... problems) {
        Objects.requireNonNull(problems, "problems cannot be null");
        return new Failure<>(new Problems(Arrays.asList(problems)));
    }

    /**
     * Transforms the value of a {@link Success} by applying the given mapper,
     * returning a new {@code Outcome}. A {@link Failure} is returned unchanged
     * and the mapper is never invoked on it.
     *
     * @param mapper the function to apply to the success value; must not be null
     * @param <R>    the type of the mapped value
     * @return a new {@code Outcome} holding the mapped value, or this failure
     * @throws NullPointerException if the mapper is null
     */
    <R> Outcome<R> map(Function<? super T, ? extends R> mapper);

    /**
     * Chains an operation that itself returns an {@code Outcome}, avoiding
     * nested {@code Outcome} values. A {@link Failure} is returned unchanged
     * and the mapper is never invoked on it (short-circuit behavior).
     *
     * @param mapper the function returning the next outcome; must not be null
     * @param <R>    the type of the chained value
     * @return the outcome produced by the mapper, or this failure
     * @throws NullPointerException if the mapper, or the result it produces, is null
     */
    <R> Outcome<R> flatMap(Function<? super T, ? extends Outcome<R>> mapper);

    /**
     * Transforms every problem of a {@link Failure}, returning a new {@code Outcome}.
     * A {@link Success} is returned unchanged and the mapper is never invoked on it.
     *
     * @param mapper the function to apply to each problem; must not be null
     * @return a new {@code Outcome} with the transformed problems, or this success
     * @throws NullPointerException if the mapper, or one of the problems it produces, is null
     */
    Outcome<T> mapProblem(Function<? super Problem, ? extends Problem> mapper);

    /**
     * Collapses this {@code Outcome} into a single value by applying
     * {@code onSuccess} to the value of a {@link Success} or {@code onFailure}
     * to the {@link Problems} of a {@link Failure}. Exactly one of the functions
     * is executed.
     *
     * @param onSuccess the function applied to the success value; must not be null
     * @param onFailure the function applied to the accumulated problems; must not be null
     * @param <R>       the type of the result
     * @return the value produced by the executed function
     * @throws NullPointerException if either function is null
     */
    <R> R fold(
            Function<? super T, ? extends R> onSuccess,
            Function<? super Problems, ? extends R> onFailure);

    /**
     * Performs a side effect with the value of a {@link Success} and returns
     * this {@code Outcome} unchanged. On a {@link Failure} the action is never
     * invoked.
     *
     * @param action the consumer to invoke with the success value; must not be null
     * @return this {@code Outcome}
     * @throws NullPointerException if the action is null
     */
    Outcome<T> peek(Consumer<? super T> action);

    /**
     * Performs a side effect with the {@link Problems} of a {@link Failure} and
     * returns this {@code Outcome} unchanged. On a {@link Success} the action
     * is never invoked.
     *
     * @param action the consumer to invoke with the problems; must not be null
     * @return this {@code Outcome}
     * @throws NullPointerException if the action is null
     */
    Outcome<T> peekProblem(Consumer<? super Problems> action);

    /**
     * Recovers from a {@link Failure} by producing a success value from its
     * {@link Problems}. A {@link Success} is returned unchanged and the recovery
     * function is never invoked on it.
     *
     * @param recovery the function that maps the problems to a recovery value; must not be null
     * @return a {@link Success} holding the recovered value, or this success
     * @throws NullPointerException if the recovery function, or the value it produces, is null
     */
    Outcome<T> recover(Function<? super Problems, ? extends T> recovery);

    /**
     * Recovers from a {@link Failure} by producing a whole new {@code Outcome}
     * from its {@link Problems}. A {@link Success} is returned unchanged and the
     * recovery function is never invoked on it.
     *
     * @param recovery the function that maps the problems to a new outcome; must not be null
     * @return the outcome produced by the recovery function, or this success
     * @throws NullPointerException if the recovery function, or the outcome it produces, is null
     */
    Outcome<T> recoverWith(Function<? super Problems, ? extends Outcome<T>> recovery);

    /**
     * Returns whether this {@code Outcome} is a {@link Success}.
     *
     * @return {@code true} if this is a success, {@code false} otherwise
     */
    default boolean isSuccess() {
        return switch (this) {
            case Success<?> s -> true;
            case Failure<?> f -> false;
        };
    }

    /**
     * Returns whether this {@code Outcome} is a {@link Failure}.
     *
     * @return {@code true} if this is a failure, {@code false} otherwise
     */
    default boolean isFailure() {
        return !isSuccess();
    }

    /**
     * Returns the value of a {@link Success}, or {@code other} when this is a
     * {@link Failure}. The result of this method is never null.
     *
     * @param other the value to return on failure; must not be null
     * @return the success value, or the given fallback value
     * @throws NullPointerException if {@code other} is null
     */
    default T orElse(T other) {
        Objects.requireNonNull(other, "other cannot be null");
        return fold(value -> value, problems -> other);
    }

    /**
     * Returns the value of a {@link Success}, or the value produced by the given
     * supplier when this is a {@link Failure}. The supplier is only executed on
     * a failure. The result of this method is never null.
     *
     * @param supplier the supplier of the fallback value; must not be null and
     *                 must not produce null
     * @return the success value, or the supplied fallback value
     * @throws NullPointerException if the supplier is null, or produces null
     */
    default T orElseGet(Supplier<? extends T> supplier) {
        Objects.requireNonNull(supplier, "supplier cannot be null");
        return fold(value -> value, problems ->
                Objects.requireNonNull(supplier.get(), "supplier cannot return null"));
    }

    /**
     * Returns the value of a {@link Success}, or throws the exception produced
     * by the given mapper when this is a {@link Failure}. The mapper is only
     * executed on a failure.
     *
     * @param exceptionMapper the function that maps the problems to an exception; must
     *                        not be null and must not produce null
     * @param <X>             the type of the exception to throw
     * @return the success value
     * @throws X                    if this is a {@link Failure}
     * @throws NullPointerException if the mapper is null, or produces null
     */
    default <X extends Throwable> T orElseThrow(
            Function<? super Problems, ? extends X> exceptionMapper) throws X {
        Objects.requireNonNull(exceptionMapper, "exceptionMapper cannot be null");
        switch (this) {
            case Success<T> success -> {
                return success.value();
            }
            case Failure<T> failure -> {
                X exception = Objects.requireNonNull(
                        exceptionMapper.apply(failure.problems()),
                        "exceptionMapper cannot return null");
                throw exception;
            }
        }
    }

    /**
     * Combines a collection of outcomes into a single outcome.
     * <p>
     * If every outcome is a {@link Success}, the result is a {@code Success}
     * containing all values in their original order. If one or more outcomes
     * are {@link Failure}, the result is a {@code Failure} containing
     * <em>all</em> problems from <em>all</em> failures, preserving the order in
     * which the failures and their problems were encountered. Problems are
     * accumulated and never short-circuit on the first failure.
     * <p>
     * An empty input produces an empty {@code Success}.
     *
     * @param outcomes the outcomes to combine; must not be null
     * @param <T>      the type of the individual values
     * @return a single {@code Outcome} combining all the given outcomes
     * @throws NullPointerException if the iterable, or any of its elements, is null
     */
    static <T> Outcome<List<T>> sequence(Iterable<? extends Outcome<T>> outcomes) {
        Objects.requireNonNull(outcomes, "outcomes cannot be null");

        List<T> values = new ArrayList<>();
        List<Problem> problems = new ArrayList<>();

        for (Outcome<T> outcome : outcomes) {
            Objects.requireNonNull(outcome, "outcome cannot be null");
            if (outcome instanceof Success<T> success) {
                values.add(success.value());
            } else if (outcome instanceof Failure<T> failure) {
                for (Problem problem : failure.problems()) {
                    problems.add(problem);
                }
            } else {
                throw new IllegalStateException("Unknown Outcome subtype: " + outcome.getClass());
            }
        }

        if (problems.isEmpty()) {
            return new Success<>(List.copyOf(values));
        }
        return new Failure<>(new Problems(problems));
    }

    /**
     * Combines the given outcomes into a single outcome, delegating to
     * {@link #sequence(Iterable)}. The semantics are identical: successes are
     * collected in order, failures accumulate every problem, and an empty
     * invocation produces an empty {@code Success}.
     *
     * @param outcomes the outcomes to combine; must not be null
     * @param <T>      the type of the individual values
     * @return a single {@code Outcome} combining all the given outcomes
     * @throws NullPointerException if the array, or any of its elements, is null
     */
    @SafeVarargs
    static <T> Outcome<List<T>> sequence(Outcome<T>... outcomes) {
        Objects.requireNonNull(outcomes, "outcomes cannot be null");
        return sequence(Arrays.asList(outcomes));
    }

    /**
     * Applies the given mapper to every element of the source and combines the
     * produced outcomes with the semantics of {@link #sequence(Iterable)}.
     * <p>
     * Conceptually {@code traverse(source, mapper)} is equivalent to mapping
     * the source and sequencing the results. The mapper is executed for every
     * element; successes contribute their values in order and failures
     * contribute all of their problems, so no error is ever lost. An empty
     * source produces an empty {@code Success}.
     *
     * @param source the elements to map; must not be null
     * @param mapper the function applied to each element, producing an outcome;
     *               must not be null and must not produce null
     * @param <S>    the type of the source elements
     * @param <T>    the type of the mapped values
     * @return a single {@code Outcome} combining every mapped outcome
     * @throws NullPointerException if the source, the mapper, or a mapped result is null
     */
    static <S, T> Outcome<List<T>> traverse(
            Iterable<S> source,
            Function<? super S, ? extends Outcome<T>> mapper) {
        Objects.requireNonNull(source, "source cannot be null");
        Objects.requireNonNull(mapper, "mapper cannot be null");

        List<Outcome<T>> outcomes = new ArrayList<>();
        for (S element : source) {
            outcomes.add(Objects.requireNonNull(
                    mapper.apply(element), "mapper cannot return null"));
        }
        return sequence(outcomes);
    }
}
