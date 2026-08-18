package dev.daniel.outcome;

import dev.daniel.outcome.error.Error;
import dev.daniel.outcome.error.Errors;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * An {@link Outcome} representing a failed result.
 * <p>
 * A {@code Failure} always carries a non-null {@link Errors} instance that
 * contains at least one {@link Error}. It executes the failure operations
 * ({@code mapError}, {@code fold} failure branch, {@code peekError},
 * {@code recover}, {@code recoverWith}) and ignores the success operations
 * ({@code map}, {@code flatMap}, {@code peek}) with short-circuit behavior.
 *
 * @param <T>    the type the failure would have carried on success
 * @param errors the accumulated errors; must not be empty
 */
public record Failure<T>(Errors errors) implements Outcome<T> {

    /**
     * Creates a {@code Failure} carrying the given errors.
     *
     * @param errors the accumulated errors
     * @throws NullPointerException if the errors are null
     */
    public Failure {
        Objects.requireNonNull(errors, "errors cannot be null");
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
    public Outcome<T> mapError(Function<? super Error, ? extends Error> mapper) {
        Objects.requireNonNull(mapper, "mapper cannot be null");
        return new Failure<>(errors.map(mapper));
    }

    @Override
    public <R> R fold(
            Function<? super T, ? extends R> onSuccess,
            Function<? super Errors, ? extends R> onFailure) {
        Objects.requireNonNull(onSuccess, "onSuccess cannot be null");
        Objects.requireNonNull(onFailure, "onFailure cannot be null");
        return onFailure.apply(errors);
    }

    @Override
    public Outcome<T> peek(Consumer<? super T> action) {
        Objects.requireNonNull(action, "action cannot be null");
        return this;
    }

    @Override
    public Outcome<T> peekError(Consumer<? super Errors> action) {
        Objects.requireNonNull(action, "action cannot be null");
        action.accept(errors);
        return this;
    }

    @Override
    public Outcome<T> recover(Function<? super Errors, ? extends T> recovery) {
        Objects.requireNonNull(recovery, "recovery cannot be null");
        T recovered = recovery.apply(errors);
        return new Success<>(Objects.requireNonNull(recovered, "recovery cannot return null"));
    }

    @Override
    public Outcome<T> recoverWith(Function<? super Errors, ? extends Outcome<T>> recovery) {
        Objects.requireNonNull(recovery, "recovery cannot be null");
        Outcome<T> recovered = recovery.apply(errors);
        return Objects.requireNonNull(recovered, "recovery cannot return null");
    }
}
