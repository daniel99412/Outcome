package io.github.daniel99412.outcome.error;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.StreamSupport;

/**
 * An immutable, ordered snapshot of one or more {@link Error}s.
 * <p>
 * {@code Errors} guarantees the invariants of this library: it is never null,
 * always contains at least one error, preserves insertion order, does not
 * deduplicate errors and never exposes a mutable collection.
 */
public final class Errors implements Iterable<Error> {
    private final List<Error> errors;

    /**
     * Creates an immutable snapshot of the given errors.
     *
     * @param errors the errors to wrap
     * @throws NullPointerException     if the collection or any of its elements is null
     * @throws IllegalArgumentException if the collection is empty
     */
    public Errors(Iterable<Error> errors) {
        Objects.requireNonNull(errors, "errors cannot be null");

        this.errors = StreamSupport
                .stream(errors.spliterator(), false)
                .map(Objects::requireNonNull)
                .toList();

        if (this.errors.isEmpty()) {
            throw new IllegalArgumentException("errors cannot be empty");
        }
    }

    /**
     * Transforms every error, preserving order, and returns a new immutable
     * {@code Errors} instance. The original instance is never mutated.
     *
     * @param mapper the function applied to each error; must not be null
     * @return a new {@code Errors} with the transformed errors
     * @throws NullPointerException if the mapper, or any error it produces, is null
     */
    public Errors map(Function<? super Error, ? extends Error> mapper) {
        Objects.requireNonNull(mapper, "mapper cannot be null");
        List<Error> mapped = errors.stream()
                .map(mapper)
                .map(Objects::requireNonNull)
                .toList();
        return new Errors(mapped);
    }

    /**
     * Returns the number of errors contained.
     *
     * @return how many errors are present
     */
    public int size() {
        return errors.size();
    }

    /**
     * Returns the first error contained.
     *
     * @return the first error, never null
     */
    public Error first() {
        return errors.getFirst();
    }

    /**
     * Returns an immutable view of all errors, in insertion order.
     *
     * @return the contained errors
     */
    public List<Error> all() {
        return errors;
    }

    /**
     * Checks whether this collection contains the given error, using
     * {@link Error} semantic equality.
     *
     * @param error the error to look for; must not be null
     * @return {@code true} if a semantically equal error is present
     * @throws NullPointerException if the error is null
     */
    public boolean contains(Error error) {
        Objects.requireNonNull(error, "error cannot be null");
        return errors.contains(error);
    }

    /**
     * Iterates over the contained errors in insertion order.
     *
     * @return an iterator over the errors
     */
    @Override
    public Iterator<Error> iterator() {
        return errors.iterator();
    }

    /**
     * Returns a string representation listing the contained errors.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "Errors" + errors;
    }
}
