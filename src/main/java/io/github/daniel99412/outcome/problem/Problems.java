package io.github.daniel99412.outcome.problem;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.StreamSupport;

/**
 * An immutable, ordered snapshot of one or more {@link Problem}s.
 * <p>
 * {@code Problems} guarantees the invariants of this library: it is never null,
 * always contains at least one problem, preserves insertion order, does not
 * deduplicate problems and never exposes a mutable collection.
 */
public final class Problems implements Iterable<Problem> {
    private final List<Problem> problems;

    /**
     * Creates an immutable snapshot of the given problems.
     *
     * @param problems the problems to wrap
     * @throws NullPointerException     if the collection or any of its elements is null
     * @throws IllegalArgumentException if the collection is empty
     */
    public Problems(Iterable<Problem> problems) {
        Objects.requireNonNull(problems, "problems cannot be null");

        this.problems = StreamSupport
                .stream(problems.spliterator(), false)
                .map(Objects::requireNonNull)
                .toList();

        if (this.problems.isEmpty()) {
            throw new IllegalArgumentException("problems cannot be empty");
        }
    }

    /**
     * Transforms every problem, preserving order, and returns a new immutable
     * {@code Problems} instance. The original instance is never mutated.
     *
     * @param mapper the function applied to each problem; must not be null
     * @return a new {@code Problems} with the transformed problems
     * @throws NullPointerException if the mapper, or any problem it produces, is null
     */
    public Problems map(Function<? super Problem, ? extends Problem> mapper) {
        Objects.requireNonNull(mapper, "mapper cannot be null");
        List<Problem> mapped = problems.stream()
                .map(mapper)
                .map(Objects::requireNonNull)
                .toList();
        return new Problems(mapped);
    }

    /**
     * Returns the number of problems contained.
     *
     * @return how many problems are present
     */
    public int size() {
        return problems.size();
    }

    /**
     * Returns the first problem contained.
     *
     * @return the first problem, never null
     */
    public Problem first() {
        return problems.getFirst();
    }

    /**
     * Returns an immutable view of all problems, in insertion order.
     *
     * @return the contained problems
     */
    public List<Problem> all() {
        return problems;
    }

    /**
     * Checks whether this collection contains the given problem, using
     * {@link Problem} semantic equality.
     *
     * @param problem the problem to look for; must not be null
     * @return {@code true} if a semantically equal problem is present
     * @throws NullPointerException if the problem is null
     */
    public boolean contains(Problem problem) {
        Objects.requireNonNull(problem, "problem cannot be null");
        return problems.contains(problem);
    }

    /**
     * Iterates over the contained problems in insertion order.
     *
     * @return an iterator over the problems
     */
    @Override
    public Iterator<Problem> iterator() {
        return problems.iterator();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Problems other)) return false;
        return problems.equals(other.problems);
    }

    @Override
    public int hashCode() {
        return problems.hashCode();
    }

    /**
     * Returns a string representation listing the contained problems.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "Problems" + problems;
    }
}
