package io.github.daniel99412.outcome.problem;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A single, immutable domain problem.
 * <p>
 * The semantic identity of a {@code Problem} is its {@code code},
 * {@code description}, {@code type} and {@code metadata}. The {@code cause}
 * is diagnostic information and does not participate in equality.
 *
 * @param code        a unique, non-blank identifier for the problem
 * @param description a human-readable, non-blank description
 * @param type        the category of the problem
 * @param metadata    optional contextual data; never null, never empty
 * @param cause       the underlying throwable, if any; may be null
 */
public record Problem(
        String code,
        String description,
        ProblemType type,
        Map<String, Object> metadata,
        Throwable cause
) {
    /**
     * Creates a {@code Problem}.
     *
     * @param code        a non-blank problem code
     * @param description a non-blank problem description
     * @param type        a non-null problem type
     * @param metadata    optional metadata; normalized to an immutable empty map when null
     * @param cause       the underlying throwable; may be null
     * @throws NullPointerException     if code, description or type is null
     * @throws IllegalArgumentException if code or description is blank
     */
    public Problem {
        Objects.requireNonNull(code, "code cannot be null");
        Objects.requireNonNull(description, "description cannot be null");
        Objects.requireNonNull(type, "type cannot be null");

        if (code.isBlank()) {
            throw new IllegalArgumentException("code cannot be blank");
        }

        if (description.isBlank()) {
            throw new IllegalArgumentException("description cannot be blank");
        }

        metadata = metadata == null
                ? Map.of()
                : Map.copyOf(metadata);
    }

    /**
     * Creates a {@code Problem} with empty metadata and no cause.
     *
     * @param code        a non-blank problem code
     * @param description a non-blank problem description
     * @param type        a non-null problem type
     * @return a new {@code Problem}
     * @throws NullPointerException     if code, description or type is null
     * @throws IllegalArgumentException if code or description is blank
     */
    public static Problem of(String code, String description, ProblemType type) {
        return new Problem(code, description, type, null, null);
    }

    /**
     * Creates a {@link ProblemType#VALIDATION} problem.
     *
     * @param code        a non-blank problem code
     * @param description a non-blank problem description
     * @return a new {@code Problem} of type {@code VALIDATION}
     */
    public static Problem validation(String code, String description) {
        return of(code, description, ProblemType.VALIDATION);
    }

    /**
     * Creates a {@link ProblemType#NOT_FOUND} problem.
     *
     * @param code        a non-blank problem code
     * @param description a non-blank problem description
     * @return a new {@code Problem} of type {@code NOT_FOUND}
     */
    public static Problem notFound(String code, String description) {
        return of(code, description, ProblemType.NOT_FOUND);
    }

    /**
     * Creates a {@link ProblemType#CONFLICT} problem.
     *
     * @param code        a non-blank problem code
     * @param description a non-blank problem description
     * @return a new {@code Problem} of type {@code CONFLICT}
     */
    public static Problem conflict(String code, String description) {
        return of(code, description, ProblemType.CONFLICT);
    }

    /**
     * Creates a {@link ProblemType#UNAUTHORIZED} problem.
     *
     * @param code        a non-blank problem code
     * @param description a non-blank problem description
     * @return a new {@code Problem} of type {@code UNAUTHORIZED}
     */
    public static Problem unauthorized(String code, String description) {
        return of(code, description, ProblemType.UNAUTHORIZED);
    }

    /**
     * Creates a {@link ProblemType#FORBIDDEN} problem.
     *
     * @param code        a non-blank problem code
     * @param description a non-blank problem description
     * @return a new {@code Problem} of type {@code FORBIDDEN}
     */
    public static Problem forbidden(String code, String description) {
        return of(code, description, ProblemType.FORBIDDEN);
    }

    /**
     * Creates a {@link ProblemType#DEPENDENCY} problem.
     *
     * @param code        a non-blank problem code
     * @param description a non-blank problem description
     * @return a new {@code Problem} of type {@code DEPENDENCY}
     */
    public static Problem dependency(String code, String description) {
        return of(code, description, ProblemType.DEPENDENCY);
    }

    /**
     * Creates a {@link ProblemType#TIMEOUT} problem.
     *
     * @param code        a non-blank problem code
     * @param description a non-blank problem description
     * @return a new {@code Problem} of type {@code TIMEOUT}
     */
    public static Problem timeout(String code, String description) {
        return of(code, description, ProblemType.TIMEOUT);
    }

    /**
     * Creates a {@link ProblemType#UNAVAILABLE} problem.
     *
     * @param code        a non-blank problem code
     * @param description a non-blank problem description
     * @return a new {@code Problem} of type {@code UNAVAILABLE}
     */
    public static Problem unavailable(String code, String description) {
        return of(code, description, ProblemType.UNAVAILABLE);
    }

    /**
     * Creates an {@link ProblemType#INTERNAL} problem.
     *
     * @param code        a non-blank problem code
     * @param description a non-blank problem description
     * @return a new {@code Problem} of type {@code INTERNAL}
     */
    public static Problem internal(String code, String description) {
        return of(code, description, ProblemType.INTERNAL);
    }

    /**
     * Returns a copy of this problem with the given cause. The original
     * instance is never mutated. The cause is diagnostic information and does
     * not participate in equality.
     *
     * @param cause the underlying throwable; must not be null
     * @return a new {@code Problem} carrying the given cause
     * @throws NullPointerException if the cause is null — use the canonical
     *                              constructor to build a problem without a cause
     */
    public Problem withCause(Throwable cause) {
        Objects.requireNonNull(cause, "cause cannot be null");
        return new Problem(code, description, type, metadata, cause);
    }

    /**
     * Returns a copy of this problem whose metadata is the current metadata
     * merged with the given entry. The original instance is never mutated.
     *
     * @param key   the metadata key; must not be null
     * @param value the metadata value; must not be null; replaces any previous
     *              value stored under the same key
     * @return a new {@code Problem} with the merged metadata
     * @throws NullPointerException if the key or value is null
     */
    public Problem withMetadata(String key, Object value) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(value, "value cannot be null");
        Map<String, Object> merged = new HashMap<>(metadata);
        merged.put(key, value);
        return new Problem(code, description, type, merged, cause);
    }

    /**
     * Returns a copy of this problem whose metadata is the current metadata
     * merged with the given entries (merge, not replacement: existing keys not
     * present in {@code extra} are preserved). For repeated keys the new value
     * replaces the previous one. The original instance is never mutated.
     *
     * @param extra the metadata entries to merge; must not be null and must not
     *              contain null keys or values
     * @return a new {@code Problem} with the merged metadata
     * @throws NullPointerException if the map is null, or contains null keys or values
     */
    public Problem withMetadata(Map<String, Object> extra) {
        Objects.requireNonNull(extra, "extra cannot be null");
        for (Map.Entry<String, Object> entry : extra.entrySet()) {
            Objects.requireNonNull(entry.getKey(), "extra metadata key cannot be null");
            Objects.requireNonNull(entry.getValue(), "extra metadata value cannot be null");
        }
        Map<String, Object> merged = new HashMap<>(metadata);
        merged.putAll(extra);
        return new Problem(code, description, type, merged, cause);
    }

    /**
     * Compares this problem with another object based on its semantic identity:
     * {@code code}, {@code description}, {@code type} and {@code metadata}.
     * The {@code cause} is ignored.
     *
     * @param o the object to compare with
     * @return {@code true} if semantically equal, {@code false} otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Problem other)) {
            return false;
        }
        return code.equals(other.code)
                && description.equals(other.description)
                && type == other.type
                && metadata.equals(other.metadata);
    }

    /**
     * Computes a hash code from the semantic identity fields, so that problems
     * that are equal share the same hash code. The {@code cause} is ignored.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(code, description, type, metadata);
    }

    /**
     * Returns a string representation of this problem that includes its semantic
     * fields. The {@code cause} is not included.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "Problem{"
                + "code='" + code + '\''
                + ", description='" + description + '\''
                + ", type=" + type
                + ", metadata=" + metadata
                + '}';
    }
}
