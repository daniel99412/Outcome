package io.github.daniel99412.outcome;

import io.github.daniel99412.outcome.problem.Problem;
import io.github.daniel99412.outcome.problem.Problems;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * An {@link Outcome} representing a failed result.
 * <p>
 * A {@code Failure} always carries a non-null {@link Problems} instance that
 * contains at least one {@link Problem}. It executes the failure operations
 * ({@code mapProblem}, {@code fold} failure branch, {@code peekProblem},
 * {@code recover}, {@code recoverWith}) and ignores the success operations
 * ({@code map}, {@code flatMap}, {@code peek}) with short-circuit behavior.
 *
 * @param <T>    the type the failure would have carried on success
 * @param problems the accumulated problems; must not be empty
 */
public record Failure<T>(Problems problems) implements Outcome<T> {

    /**
     * Creates a {@code Failure} carrying the given problems.
     *
     * @param problems the accumulated problems
     * @throws NullPointerException if the problems are null
     */
    public Failure {
        Objects.requireNonNull(problems, "problems cannot be null");
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> Outcome<R> map(Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper cannot be null");
        return (Outcome<R>) this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> Outcome<R> flatMap(Function<? super T, ? extends Outcome<R>> mapper) {
        Objects.requireNonNull(mapper, "mapper cannot be null");
        return (Outcome<R>) this;
    }

    @Override
    public Outcome<T> mapProblem(Function<? super Problem, ? extends Problem> mapper) {
        Objects.requireNonNull(mapper, "mapper cannot be null");
        return new Failure<>(problems.map(mapper));
    }

    @Override
    public <R> R fold(
            Function<? super T, ? extends R> onSuccess,
            Function<? super Problems, ? extends R> onFailure) {
        Objects.requireNonNull(onSuccess, "onSuccess cannot be null");
        Objects.requireNonNull(onFailure, "onFailure cannot be null");
        return onFailure.apply(problems);
    }

    @Override
    public Outcome<T> peek(Consumer<? super T> action) {
        Objects.requireNonNull(action, "action cannot be null");
        return this;
    }

    @Override
    public Outcome<T> peekProblem(Consumer<? super Problems> action) {
        Objects.requireNonNull(action, "action cannot be null");
        action.accept(problems);
        return this;
    }

    @Override
    public Outcome<T> recover(Function<? super Problems, ? extends T> recovery) {
        Objects.requireNonNull(recovery, "recovery cannot be null");
        T recovered = recovery.apply(problems);
        return new Success<>(Objects.requireNonNull(
                recovered, "recover recovery cannot return null"));
    }

    @Override
    public Outcome<T> recoverWith(Function<? super Problems, ? extends Outcome<T>> recovery) {
        Objects.requireNonNull(recovery, "recovery cannot be null");
        Outcome<T> recovered = recovery.apply(problems);
        return Objects.requireNonNull(recovered, "recoverWith recovery cannot return null");
    }
}
