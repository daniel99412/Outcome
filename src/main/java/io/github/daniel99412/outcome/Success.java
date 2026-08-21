package io.github.daniel99412.outcome;

import io.github.daniel99412.outcome.problem.Problem;
import io.github.daniel99412.outcome.problem.Problems;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * An {@link Outcome} representing a successful result.
 * <p>
 * A {@code Success} always carries a non-null value. It executes the success
 * operations ({@code map}, {@code flatMap}, {@code fold} success branch,
 * {@code peek}) and ignores the failure operations ({@code mapProblem},
 * {@code peekProblem}, {@code recover}, {@code recoverWith}).
 *
 * @param <T> the type of the value
 * @param value the successful value; must not be null
 */
public record Success<T>(T value) implements Outcome<T> {

    /**
     * Creates a {@code Success} holding the given value.
     *
     * @param value the successful value
     * @throws NullPointerException if the value is null
     */
    public Success {
        Objects.requireNonNull(value, "value cannot be null");
    }

    @Override
    public <R> Outcome<R> map(Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper cannot be null");
        return new Success<>(mapper.apply(value));
    }

    @Override
    public <R> Outcome<R> flatMap(Function<? super T, ? extends Outcome<R>> mapper) {
        Objects.requireNonNull(mapper, "mapper cannot be null");
        Outcome<R> result = mapper.apply(value);
        return Objects.requireNonNull(result, "flatMap mapper cannot return null");
    }

    @Override
    public Outcome<T> mapProblem(Function<? super Problem, ? extends Problem> mapper) {
        Objects.requireNonNull(mapper, "mapper cannot be null");
        return this;
    }

    @Override
    public <R> R fold(
            Function<? super T, ? extends R> onSuccess,
            Function<? super Problems, ? extends R> onFailure) {
        Objects.requireNonNull(onSuccess, "onSuccess cannot be null");
        Objects.requireNonNull(onFailure, "onFailure cannot be null");
        return onSuccess.apply(value);
    }

    @Override
    public Outcome<T> peek(Consumer<? super T> action) {
        Objects.requireNonNull(action, "action cannot be null");
        action.accept(value);
        return this;
    }

    @Override
    public Outcome<T> peekProblem(Consumer<? super Problems> action) {
        Objects.requireNonNull(action, "action cannot be null");
        return this;
    }

    @Override
    public Outcome<T> recover(Function<? super Problems, ? extends T> recovery) {
        Objects.requireNonNull(recovery, "recovery cannot be null");
        return this;
    }

    @Override
    public Outcome<T> recoverWith(Function<? super Problems, ? extends Outcome<T>> recovery) {
        Objects.requireNonNull(recovery, "recovery cannot be null");
        return this;
    }
}
