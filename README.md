# Outcome

A small, immutable, dependency-free result type for Java 21+.

`Outcome<T>` represents the result of a possibly failing operation as one of
two sealed alternatives:

```
Outcome<T>
├── Success<T>
│   └── value          (non-null)
└── Failure<T>
    └── Problems         (1..N Problem)
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
implementation 'io.github.daniel99412:outcome:1.0.0'
```

```xml
<!-- Maven -->
<dependency>
    <groupId>io.github.daniel99412</groupId>
    <artifactId>outcome</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Quick example

```java
import io.github.daniel99412.outcome.Outcome;
import io.github.daniel99412.outcome.Success;
import io.github.daniel99412.outcome.problem.Problem;
import io.github.daniel99412.outcome.problem.ProblemType;

Outcome<String> result = lookupUser("ada")
        .map(user -> user.displayName())
        .recover(problems -> "unknown user");
```

## Core semantics

The two halves of `Outcome` behave as strict counterparts:

| Operation        | `Success<T>`                                | `Failure<T>`                                   |
|------------------|---------------------------------------------|------------------------------------------------|
| `map`            | transforms the value                        | returns `this`; mapper **not** executed        |
| `flatMap`        | chains the returned outcome                 | returns `this`; mapper **not** executed        |
| `mapProblem`       | returns `this`; mapper **not** executed     | transforms **every** problem                     |
| `fold`           | runs the success branch                     | runs the failure branch                        |
| `peek`           | runs the action                             | returns `this`; action **not** executed        |
| `peekProblem`      | returns `this`; action **not** executed     | runs the action                                |
| `recover`        | returns `this`; recovery **not** executed   | returns `Success` with the recovered value     |
| `recoverWith`    | returns `this`; recovery **not** executed   | returns the recovered `Outcome`                |
| `isSuccess()`    | `true`                                      | `false`                                        |
| `isFailure()`    | `false`                                     | `true`                                         |

The two central behaviors that distinguish the combinators:

- **`flatMap` short-circuits** — the first `Failure` stops all further work.
- **`sequence` accumulates** — it never stops at the first `Failure`; it
  collects the problems from every failure.

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
| `Outcome<T> mapProblem(Function<? super Problem,? extends Problem>)`         | transform every problem                              |
| `<R> R fold(Function<? super T,? extends R>, Function<? super Problems,? extends R>)` | collapse into a single value        |
| `Outcome<T> peek(Consumer<? super T>)`                                 | side effect on success                             |
| `Outcome<T> peekProblem(Consumer<? super Problems>)`                       | side effect on failure                             |
| `Outcome<T> recover(Function<? super Problems,? extends T>)`            | recover a value from failure                       |
| `Outcome<T> recoverWith(Function<? super Problems,? extends Outcome<T>>)` | recover an outcome from failure                  |
| `boolean isSuccess()` / `boolean isFailure()`                          | type checks (default methods)                      |
| `static <T> Outcome<List<T>> sequence(Iterable<? extends Outcome<T>>)` | combine outcomes, accumulating all problems          |

### `Success<T>` (record)

`Success<T>(T value)` — the value may not be `null`.

### `Failure<T>` (record)

`Failure<T>(Problems problems)` — the problems may not be `null`, and `Problems`
always contains at least one problem.

### `Problems`

An immutable, ordered snapshot of one or more `Problem`s.

| Method | Description |
|--------|-------------|
| `int size()` | number of problems |
| `Problem first()` | first problem |
| `List<Problem> all()` | immutable view of all problems, in order |
| `boolean contains(Problem)` | semantic equality check |
| `Problems map(Function<Problem,Problem>)` | transform every problem, preserving order |

### `Problem`

An immutable problem with:

- `String code` — non-blank
- `String description` — non-blank
- `ProblemType type` — non-null
- `Map<String, Object> metadata` — immutable; `null` normalizes to an empty map
- `Throwable cause` — optional

**Equality** is based on `code`, `description`, `type` and `metadata`. The
`cause` is diagnostic information and does not participate in equality.

### `ProblemType`

`VALIDATION`, `NOT_FOUND`, `CONFLICT`, `UNAUTHORIZED`, `FORBIDDEN`,
`DEPENDENCY`, `TIMEOUT`, `UNAVAILABLE`, `INTERNAL`.

## Invariants

- `Success` never holds a `null` value.
- `Failure` always holds a non-empty `Problems`.
- `Problems` is immutable, preserves insertion order, and never deduplicates.
- `Problem.metadata` is immutable after construction.
- Null is rejected for every function argument, and any mapper/recovery that
  returns `null` fails fast with a `NullPointerException`.
- `Outcome` values are immutable; no operation ever mutates the instance it
  is called on.

## Building

```bash
./gradlew build
./gradlew test
```
