# Outcome

A small, immutable, dependency-free result type for Java 21+.

`Outcome<T>` represents the result of a possibly failing operation as one of
two sealed alternatives:

```
Outcome<T>
├── Success<T>
│   └── value          (non-null)
└── Failure<T>
    └── Errors         (1..N Error)
```

It is deliberately small. It is **not** a replacement for
`java.util.Optional` and it is **not** a general functional programming
framework. It carries no serialization, no framework bindings, no
distributed-system concepts, and no external dependencies.

## Installation

The library is a plain Java 21 class library. Add it to your build as a
dependency once published, or include the sources directly:

```groovy
// Gradle
implementation 'dev.daniel.outcome:outcome:1.0.0'
```

```xml
<!-- Maven -->
<dependency>
    <groupId>dev.daniel.outcome</groupId>
    <artifactId>outcome</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Quick example

```java
import dev.daniel.outcome.Outcome;
import dev.daniel.outcome.Success;
import dev.daniel.outcome.error.Error;
import dev.daniel.outcome.error.ErrorType;

Outcome<String> result = lookupUser("ada")
        .map(user -> user.displayName())
        .recover(errors -> "unknown user");
```

## Core semantics

The two halves of `Outcome` behave as strict counterparts:

| Operation        | `Success<T>`                                | `Failure<T>`                                   |
|------------------|---------------------------------------------|------------------------------------------------|
| `map`            | transforms the value                        | returns `this`; mapper **not** executed        |
| `flatMap`        | chains the returned outcome                 | returns `this`; mapper **not** executed        |
| `mapError`       | returns `this`; mapper **not** executed     | transforms **every** error                     |
| `fold`           | runs the success branch                     | runs the failure branch                        |
| `peek`           | runs the action                             | returns `this`; action **not** executed        |
| `peekError`      | returns `this`; action **not** executed     | runs the action                                |
| `recover`        | returns `this`; recovery **not** executed   | returns `Success` with the recovered value     |
| `recoverWith`    | returns `this`; recovery **not** executed   | returns the recovered `Outcome`                |
| `isSuccess()`    | `true`                                      | `false`                                        |
| `isFailure()`    | `false`                                     | `true`                                         |

The two central behaviors that distinguish the combinators:

- **`flatMap` short-circuits** — the first `Failure` stops all further work.
- **`sequence` accumulates** — it never stops at the first `Failure`; it
  collects the errors from every failure.

### Example: `sequence`

```java
Outcome<List<String>> result = Outcome.sequence(List.of(
        new Success<>("A"),
        new Success<>("B")))                  // => Success([A, B])

Outcome<List<String>> result = Outcome.sequence(List.of(
        new Success<>("A"),
        failure("E1", "E2"),
        failure("E3")))                       // => Failure(E1, E2, E3)
```

An empty input produces `Success(List.of())`.

## API

### `Outcome<T>` (sealed interface)

| Signature                                                              | Description                                        |
|------------------------------------------------------------------------|----------------------------------------------------|
| `<R> Outcome<R> map(Function<? super T,? extends R>)`                  | transform the success value                        |
| `<R> Outcome<R> flatMap(Function<? super T,? extends Outcome<R>>)`     | chain an outcome-returning operation               |
| `Outcome<T> mapError(Function<? super Error,? extends Error>)`         | transform every error                              |
| `<R> R fold(Function<? super T,? extends R>, Function<? super Errors,? extends R>)` | collapse into a single value        |
| `Outcome<T> peek(Consumer<? super T>)`                                 | side effect on success                             |
| `Outcome<T> peekError(Consumer<? super Errors>)`                       | side effect on failure                             |
| `Outcome<T> recover(Function<? super Errors,? extends T>)`            | recover a value from failure                       |
| `Outcome<T> recoverWith(Function<? super Errors,? extends Outcome<T>>)` | recover an outcome from failure                  |
| `boolean isSuccess()` / `boolean isFailure()`                          | type checks (default methods)                      |
| `static <T> Outcome<List<T>> sequence(Iterable<? extends Outcome<T>>)` | combine outcomes, accumulating all errors          |

### `Success<T>` (record)

`Success<T>(T value)` — the value may not be `null`.

### `Failure<T>` (record)

`Failure<T>(Errors errors)` — the errors may not be `null`, and `Errors`
always contains at least one error.

### `Errors`

An immutable, ordered snapshot of one or more `Error`s.

| Method | Description |
|--------|-------------|
| `int size()` | number of errors |
| `Error first()` | first error |
| `List<Error> all()` | immutable view of all errors, in order |
| `boolean contains(Error)` | semantic equality check |
| `Errors map(Function<Error,Error>)` | transform every error, preserving order |

### `Error`

An immutable error with:

- `String code` — non-blank
- `String description` — non-blank
- `ErrorType type` — non-null
- `Map<String, Object> metadata` — immutable; `null` normalizes to an empty map
- `Throwable cause` — optional

**Equality** is based on `code`, `description`, `type` and `metadata`. The
`cause` is diagnostic information and does not participate in equality.

### `ErrorType`

`VALIDATION`, `NOT_FOUND`, `CONFLICT`, `UNAUTHORIZED`, `FORBIDDEN`,
`DEPENDENCY`, `TIMEOUT`, `UNAVAILABLE`, `INTERNAL`.

## Invariants

- `Success` never holds a `null` value.
- `Failure` always holds a non-empty `Errors`.
- `Errors` is immutable, preserves insertion order, and never deduplicates.
- `Error.metadata` is immutable after construction.
- Null is rejected for every function argument, and any mapper/recovery that
  returns `null` fails fast with a `NullPointerException`.
- `Outcome` values are immutable; no operation ever mutates the instance it
  is called on.

## Building

```bash
./gradlew build
./gradlew test
```
