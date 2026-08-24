package io.github.daniel99412.outcome;

import io.github.daniel99412.outcome.problem.Problem;
import io.github.daniel99412.outcome.problem.ProblemType;
import io.github.daniel99412.outcome.problem.Problems;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
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

    /**
     * Runs the given work and captures its result as an {@code Outcome},
     * converting any thrown {@link Exception} into a {@link Failure} carrying
     * an internal problem.
     * <p>
     * This is the entry point from imperative code that signals failure by
     * throwing: it is the equivalent of wrapping the call in {@code try/catch}.
     * Fatal JVM errors ({@link OutOfMemoryError}, {@link StackOverflowError},
     * {@link java.lang.VirtualMachineError}) are deliberately <em>not</em>
     * caught; only {@code Exception} is converted.
     *
     * @param work the operation to run; must not be null
     * @param <T>  the type of the produced value
     * @return a {@code Success} holding the produced value, or a {@code Failure}
     *         carrying one internal problem whose cause is the exception
     * @throws NullPointerException if the work is null, or produces null
     */
    static <T> Outcome<T> catching(ThrowingSupplier<? extends T> work) {
        Objects.requireNonNull(work, "work cannot be null");
        T result;
        try {
            result = work.get();
        } catch (Exception exception) {
            String message = exception.getMessage();
            String description =
                    message == null || message.isBlank()
                            ? exception.getClass().getName()
                            : message;
            return failure(new Problem(
                    "UNEXPECTED_FAILURE",
                    description,
                    ProblemType.INTERNAL,
                    null,
                    exception));
        }
        return new Success<>(Objects.requireNonNull(result, "work cannot return null"));
    }

    /**
     * Runs the given work and captures its result as an {@code Outcome},
     * converting any thrown {@link Exception} into a {@link Failure} carrying
     * the problem produced by the given mapper.
     * <p>
     * The mapper receives exactly what this method catches: an {@code Exception}.
     * Fatal JVM errors are never caught and therefore never reach the mapper.
     *
     * @param work      the operation to run; must not be null
     * @param toProblem the function that maps the exception to a problem; must
     *                  not be null and must not produce null
     * @param <T>       the type of the produced value
     * @return a {@code Success} holding the produced value, or a {@code Failure}
     *         carrying the mapped problem
     * @throws NullPointerException if the work, the mapper, or the produced problem is null
     */
    static <T> Outcome<T> catching(
            ThrowingSupplier<? extends T> work,
            Function<? super Exception, ? extends Problem> toProblem) {
        Objects.requireNonNull(work, "work cannot be null");
        Objects.requireNonNull(toProblem, "toProblem cannot be null");
        T result;
        try {
            result = work.get();
        } catch (Exception exception) {
            return failure(Objects.requireNonNull(
                    toProblem.apply(exception),
                    "catching mapper cannot return null"));
        }
        return new Success<>(Objects.requireNonNull(result, "work cannot return null"));
    }

    /**
     * Guards a {@code Success}: if its value satisfies the predicate, this
     * {@code Outcome} is returned unchanged; otherwise a {@link Failure}
     * carrying the supplied problem replaces it. A {@link Failure} is returned
     * unchanged and neither the predicate nor the supplier is invoked on it.
     *
     * @param predicate the condition the success value must satisfy; must not be null
     * @param problem   the supplier of the problem used when the condition fails;
     *                  must not be null and must not produce null
     * @return this {@code Outcome} when the guard holds, or a {@code Failure}
     *         carrying the supplied problem
     * @throws NullPointerException if the predicate or supplier is null, or the
     *                              supplier produces null
     */
    default Outcome<T> ensure(
            Predicate<? super T> predicate,
            Supplier<? extends Problem> problem) {
        Objects.requireNonNull(predicate, "ensure predicate cannot be null");
        Objects.requireNonNull(problem, "ensure problem supplier cannot be null");
        switch (this) {
            case Failure<T> ignored -> {
                return this;
            }
            case Success<T> success -> {
                if (predicate.test(success.value())) {
                    return this;
                }
                return failure(Objects.requireNonNull(problem.get(),
                        "ensure problem supplier cannot return null"));
            }
        }
    }

    /**
     * Tries an alternative strategy when this {@code Outcome} is a
     * {@link Failure}. A {@code Success} is returned unchanged and the fallback
     * supplier is never evaluated.
     * <p>
     * When both this outcome and the fallback are failures, the fallback's
     * problems fully replace the original ones: {@code otherwise} means "try
     * another strategy", not "accumulate every strategy". To accumulate problems
     * use {@link #zip(Outcome, Outcome, BiFunction)}, {@link #sequence(Iterable)}
     * or {@link #traverse(Iterable, Function)}.
     *
     * @param fallback the supplier of the alternative outcome; must not be null
     *                 and must not produce null; evaluated lazily, only on failure
     * @return this {@code Outcome} on success, or the outcome produced by the fallback
     * @throws NullPointerException if the fallback is null, or produces null
     */
    default Outcome<T> otherwise(Supplier<? extends Outcome<T>> fallback) {
        Objects.requireNonNull(fallback, "otherwise fallback cannot be null");
        if (isSuccess()) {
            return this;
        }
        return Objects.requireNonNull(fallback.get(),
                "otherwise fallback cannot return null");
    }

    /**
     * Combines two outcomes into one, preserving the type of each source.
     * <p>
     * If both outcomes are {@link Success}, the result is a {@code Success}
     * holding the combiner's output. If one or both are {@link Failure}, the
     * result is a {@code Failure} carrying <em>all</em> problems from <em>all</em>
     * failures, in order, and the combiner is never executed.
     *
     * @param first    the first outcome; must not be null
     * @param second   the second outcome; must not be null
     * @param combiner the function combining both values; must not be null and
     *                 must not produce null
     * @param <A>      the type of the first value
     * @param <B>      the type of the second value
     * @param <R>      the type of the combined value
     * @return a single {@code Outcome} combining both sources
     * @throws NullPointerException if any argument is null, or the combiner produces null
     */
    static <A, B, R> Outcome<R> zip(
            Outcome<A> first,
            Outcome<B> second,
            BiFunction<? super A, ? super B, ? extends R> combiner) {
        Objects.requireNonNull(first, "first cannot be null");
        Objects.requireNonNull(second, "second cannot be null");
        Objects.requireNonNull(combiner, "combiner cannot be null");

        List<Problem> problems = new ArrayList<>();
        A firstValue = collect(first, problems);
        B secondValue = collect(second, problems);

        if (!problems.isEmpty()) {
            return new Failure<>(new Problems(problems));
        }
        R combined = Objects.requireNonNull(combiner.apply(firstValue, secondValue),
                "zip combiner cannot return null");
        return new Success<>(combined);
    }

    /**
     * Combines three outcomes into one, preserving the type of each source.
     * The semantics are identical to {@link #zip(Outcome, Outcome, BiFunction)}:
     * all successes combine their values, any failure accumulates every problem
     * from every failed source and skips the combiner.
     *
     * @param first    the first outcome; must not be null
     * @param second   the second outcome; must not be null
     * @param third    the third outcome; must not be null
     * @param combiner the function combining the three values; must not be null
     *                 and must not produce null
     * @param <A>      the type of the first value
     * @param <B>      the type of the second value
     * @param <C>      the type of the third value
     * @param <R>      the type of the combined value
     * @return a single {@code Outcome} combining all three sources
     * @throws NullPointerException if any argument is null, or the combiner produces null
     */
    static <A, B, C, R> Outcome<R> zip(
            Outcome<A> first,
            Outcome<B> second,
            Outcome<C> third,
            TriFunction<? super A, ? super B, ? super C, ? extends R> combiner) {
        Objects.requireNonNull(first, "first cannot be null");
        Objects.requireNonNull(second, "second cannot be null");
        Objects.requireNonNull(third, "third cannot be null");
        Objects.requireNonNull(combiner, "combiner cannot be null");

        List<Problem> problems = new ArrayList<>();
        A firstValue = collect(first, problems);
        B secondValue = collect(second, problems);
        C thirdValue = collect(third, problems);

        if (!problems.isEmpty()) {
            return new Failure<>(new Problems(problems));
        }
        R combined = Objects.requireNonNull(
                combiner.apply(firstValue, secondValue, thirdValue),
                "zip combiner cannot return null");
        return new Success<>(combined);
    }

    /**
     * Returns the value of a success, or records every problem of a failure
     * into the sink and returns null. Package-private helper for {@code zip}.
     */
    private static <T> T collect(Outcome<T> outcome, List<Problem> sink) {
        return switch (outcome) {
            case Success<T> success -> success.value();
            case Failure<T> failure -> {
                for (Problem problem : failure.problems()) {
                    sink.add(problem);
                }
                yield null;
            }
        };
    }

    /**
     * A supplier whose operation may throw a checked exception. It lets
     * {@link #catching(ThrowingSupplier)} lift imperative, exception-throwing
     * code into an {@code Outcome} without manual wrapping. An ordinary
     * non-throwing lambda also conforms to this interface.
     *
     * @param <T> the type of the produced value
     */
    @FunctionalInterface
    interface ThrowingSupplier<T> {

        /**
         * Produces a value, possibly throwing a checked exception.
         *
         * @return the produced value; must not be null
         * @throws Exception if the operation fails
         */
        T get() throws Exception;
    }

    /**
     * A function of three arguments. Java's standard library only provides
     * {@link Function} and {@link BiFunction}; this interface exists solely to
     * support the three-source overload of {@link #zip}.
     *
     * @param <A> the type of the first argument
     * @param <B> the type of the second argument
     * @param <C> the type of the third argument
     * @param <R> the type of the result
     */
    @FunctionalInterface
    interface TriFunction<A, B, C, R> {

        /**
         * Applies this function to the given arguments.
         *
         * @param a the first argument
         * @param b the second argument
         * @param c the third argument
         * @return the function result; must not be null where the caller requires it
         */
        R apply(A a, B b, C c);
    }
}
