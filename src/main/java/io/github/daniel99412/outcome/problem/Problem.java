package io.github.daniel99412.outcome.problem;

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
