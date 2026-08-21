package io.github.daniel99412.outcome.error;

import java.util.Map;
import java.util.Objects;

/**
 * A single, immutable domain error.
 * <p>
 * The semantic identity of an {@code Error} is its {@code code},
 * {@code description}, {@code type} and {@code metadata}. The {@code cause}
 * is diagnostic information and does not participate in equality.
 *
 * @param code        a unique, non-blank identifier for the error
 * @param description a human-readable, non-blank description
 * @param type        the category of the error
 * @param metadata    optional contextual data; never null, never empty
 * @param cause       the underlying throwable, if any; may be null
 */
public record Error(
        String code,
        String description,
        ErrorType type,
        Map<String, Object> metadata,
        Throwable cause
) {
    /**
     * Creates an {@code Error}.
     *
     * @param code        a non-blank error code
     * @param description a non-blank error description
     * @param type        a non-null error type
     * @param metadata    optional metadata; normalized to an immutable empty map when null
     * @param cause       the underlying throwable; may be null
     * @throws NullPointerException     if code, description or type is null
     * @throws IllegalArgumentException if code or description is blank
     */
    public Error {
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
     * Compares this error with another object based on its semantic identity:
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
        if (!(o instanceof Error other)) {
            return false;
        }
        return code.equals(other.code)
                && description.equals(other.description)
                && type == other.type
                && metadata.equals(other.metadata);
    }

    /**
     * Computes a hash code from the semantic identity fields, so that errors
     * that are equal share the same hash code. The {@code cause} is ignored.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(code, description, type, metadata);
    }

    /**
     * Returns a string representation of this error that includes its semantic
     * fields. The {@code cause} is not included.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return "Error{"
                + "code='" + code + '\''
                + ", description='" + description + '\''
                + ", type=" + type
                + ", metadata=" + metadata
                + '}';
    }
}
