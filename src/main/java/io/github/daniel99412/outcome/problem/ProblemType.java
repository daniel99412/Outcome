package io.github.daniel99412.outcome.problem;

/**
 * The category of a {@link Problem}.
 * <p>
 * These types are deliberately generic and domain-agnostic. They can be used
 * to drive uniform handling such as logging, status mapping or user feedback
 * without coupling the core library to any framework.
 */
public enum ProblemType {
    /** The input or state did not satisfy the required constraints. */
    VALIDATION,
    /** A referenced entity could not be found. */
    NOT_FOUND,
    /** The operation conflicts with the current state of the system. */
    CONFLICT,
    /** The caller could not be identified. */
    UNAUTHORIZED,
    /** The caller is identified but not allowed to perform the operation. */
    FORBIDDEN,
    /** A failure propagated from a dependency of this operation. */
    DEPENDENCY,
    /** The operation exceeded its allotted time. */
    TIMEOUT,
    /** The required resource or capability is currently unavailable. */
    UNAVAILABLE,
    /** An unexpected internal failure occurred. */
    INTERNAL
}
