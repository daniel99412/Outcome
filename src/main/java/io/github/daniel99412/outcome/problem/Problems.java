package io.github.daniel99412.outcome.problem;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * An immutable, ordered snapshot of one or more {@link Problem}s.
 * <p>
 * {@code Problems} guarantees the invariants of this library: it is never null,
 * always contains at least one problem, preserves insertion order, does not
 * deduplicate problems and never exposes a mutable collection.
 *
 * @param problems the contained problems, in insertion order; never null, never empty
 */
public record Problems(List<Problem> problems) implements Iterable<Problem> {

    /**
     * Creates an immutable snapshot of the given problems.
     *
     * @param problems the problems to wrap; must not be null, must not be empty,
     *                 and must not contain null elements
     * @throws NullPointerException     if the list is null or contains null elements
     * @throws IllegalArgumentException if the list is empty
     */
    public Problems {
        Objects.requireNonNull(problems, "problems cannot be null");
        problems = List.copyOf(problems);

        if (problems.isEmpty()) {
            throw new IllegalArgumentException("problems cannot be empty");
        }
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
        return new Problems(problems.stream()
                .map(mapper)
                .map(problem -> Objects.requireNonNull(problem, "mapper cannot return null"))
                .toList());
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
     * Checks whether any contained problem has the given code.
     *
     * @param code the code to look for; must not be null
     * @return {@code true} if at least one problem carries the code
     * @throws NullPointerException if the code is null
     */
    public boolean hasCode(String code) {
        Objects.requireNonNull(code, "code cannot be null");
        return problems.stream().anyMatch(problem -> problem.code().equals(code));
    }

    /**
     * Returns every problem carrying the given code, in insertion order.
     * Multiple problems may share a code; the returned list is immutable and
     * may be empty.
     *
     * @param code the code to look for; must not be null
     * @return an immutable list of matching problems, possibly empty
     * @throws NullPointerException if the code is null
     */
    public List<Problem> byCode(String code) {
        Objects.requireNonNull(code, "code cannot be null");
        return problems.stream()
                .filter(problem -> problem.code().equals(code))
                .toList();
    }

    /**
     * Returns every problem of the given type, in insertion order. The returned
     * list is immutable and may be empty.
     *
     * @param type the type to look for; must not be null
     * @return an immutable list of matching problems, possibly empty
     * @throws NullPointerException if the type is null
     */
    public List<Problem> byType(ProblemType type) {
        Objects.requireNonNull(type, "type cannot be null");
        return problems.stream()
                .filter(problem -> problem.type() == type)
                .toList();
    }
}
