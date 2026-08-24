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

The jar carries `Automatic-Module-Name: io.github.daniel99412.outcome`, so it
can also be used on the JPMS module path without a full `module-info.java`.

## Quick example

```java
import io.github.daniel99412.outcome.Outcome;

Outcome<String> result = lookupUser("ada")
        .map(user -> user.displayName())
        .recover(problems -> "unknown user");
```

## Factories

```java
Problem notFound = new Problem("USER_NOT_FOUND", "no such user",
        ProblemType.NOT_FOUND, null, null);

Outcome<User> loaded  = Outcome.success(user);
Outcome<User> missing = Outcome.failure(notFound);
Outcome<User> several = Outcome.failure(notFound, validationProblem); // varargs
Outcome<User> batched = Outcome.failure(problems);                    // Problems
```

Exceptions are intentionally **not** caught by the core: converting a thrown
exception into a `Problem` is the caller's decision.

## Core semantics

The two halves of `Outcome` behave as strict counterparts:

| Operation      | `Success<T>`                                 | `Failure<T>`                                 |
|----------------|----------------------------------------------|----------------------------------------------|
| `map`          | transforms the value                         | returns `this`; mapper **not** executed      |
| `flatMap`      | chains the returned outcome                  | returns `this`; mapper **not** executed      |
| `mapProblem`   | returns `this`; mapper **not** executed      | transforms **every** problem                 |
| `fold`         | runs the success branch                      | runs the failure branch                      |
| `peek`         | runs the action                              | returns `this`; action **not** executed      |
| `peekProblem`  | returns `this`; action **not** executed      | runs the action                              |
| `recover`      | returns `this`; recovery **not** executed    | returns `Success` with the recovered value   |
| `recoverWith`  | returns `this`; recovery **not** executed    | returns the recovered `Outcome`              |
| `orElse`       | returns the value                            | returns the fallback value                   |
| `orElseGet`    | returns the value; supplier **not** executed | returns the supplied fallback value          |
| `orElseThrow`  | returns the value; mapper **not** executed   | throws the mapped exception                  |
| `isSuccess()`  | `true`                                       | `false`                                      |
| `isFailure()`  | `false`                                      | `true`                                       |

The two central behaviors that distinguish the combinators:

- **`flatMap` short-circuits** — the first `Failure` stops all further work.
- **`sequence`/`traverse` accumulate** — they never stop at the first
  `Failure`; they collect the problems from every failure.
- **`zip` accumulates across types** — like `sequence`, but preserves the type
  of every source position.

## Entry point from exceptions

```java
// imperative code becomes a one-liner; checked exceptions included
Outcome<String> config = Outcome.catching(() -> Files.readString(path));

// or map the exception yourself
Outcome<User> parsed = Outcome.catching(
        () -> parse(json),
        ex -> Problem.validation("PARSE_FAILED", "invalid payload"));

// validate a success inside the chain
Outcome<User> active = loadUser(id)
        .ensure(User::isActive,
                () -> Problem.conflict("USER_INACTIVE", "suspended"));

// cascade of fallbacks; each strategy runs only if the previous one failed
Outcome<User> user = cacheLookup(id)
        .otherwise(() -> dbLookup(id))
        .otherwise(() -> apiLookup(id));

// combine differently-typed sources, accumulating all problems
Outcome<Account> account = Outcome.zip(
        loadUser(id), loadProfile(id), loadSettings(id),
        Account::new);
```

### Example: `sequence`

```java
// every outcome succeeds => values collected in order
Outcome<List<String>> ok = Outcome.sequence(
        Outcome.success("A"),
        Outcome.success("B"));                // => Success([A, B])

// given Problem instances e1, e2 and e3...
// one or more failures => ALL problems accumulated, in order
Outcome<List<String>> bad = Outcome.sequence(
        Outcome.success("A"),
        Outcome.failure(e1, e2),              // Failure carrying e1, e2
        Outcome.failure(e3));                 // => Failure(e1, e2, e3)
```

An empty input produces `Success(List.of())`.

`sequence` also accepts varargs:

```java
Outcome<List<String>> result =
        Outcome.sequence(Outcome.success("A"), Outcome.success("B"));
```

### Example: `traverse`

```java
// validate every user and accumulate ALL validation problems
Outcome<List<User>> result = Outcome.traverse(users, this::validateUser);
```

Conceptually `traverse(source, mapper)` is equivalent to mapping the source
and sequencing the results: the mapper runs for every element, successes keep
their order, failures contribute all their problems.

## API

### `Outcome<T>` (sealed interface)

| Signature                                                                                             | Description                                  |
|-------------------------------------------------------------------------------------------------------|----------------------------------------------|
| `static <T> Outcome<T> success(T)`                                                                    | create a `Success`; value must not be null   |
| `static <T> Outcome<T> failure(Problem)` / `failure(Problems)` / `failure(Problem...)`                | create a `Failure`                           |
| `<R> Outcome<R> map(Function<? super T,? extends R>)`                                                 | transform the success value                  |
| `<R> Outcome<R> flatMap(Function<? super T,? extends Outcome<R>>)`                                    | chain an outcome-returning operation         |
| `Outcome<T> mapProblem(Function<? super Problem,? extends Problem>)`                                  | transform every problem                      |
| `<R> R fold(Function<? super T,? extends R>, Function<? super Problems,? extends R>)`                 | collapse into a single value                 |
| `Outcome<T> peek(Consumer<? super T>)`                                                                | side effect on success                       |
| `Outcome<T> peekProblem(Consumer<? super Problems>)`                                                  | side effect on failure                       |
| `Outcome<T> recover(Function<? super Problems,? extends T>)`                                          | recover a value from failure                 |
| `Outcome<T> recoverWith(Function<? super Problems,? extends Outcome<T>>)`                             | recover an outcome from failure              |
| `T orElse(T)`                                                                                         | success value, or the given fallback (never null) |
| `T orElseGet(Supplier<? extends T>)`                                                                  | success value, or lazily supplied fallback   |
| `<X extends Throwable> T orElseThrow(Function<? super Problems,? extends X>) throws X`                | success value, or throw mapped exception     |
| `boolean isSuccess()` / `boolean isFailure()`                                                         | type checks (default methods)                |
| `static <T> Outcome<T> catching(ThrowingSupplier<? extends T>)`                                       | run throwing code; any `Exception` becomes an internal `Failure` (`Error` passes through) |
| `static <T> Outcome<T> catching(ThrowingSupplier<? extends T>, Function<? super Exception,? extends Problem>)` | run throwing code with a custom exception-to-problem mapping |
| `Outcome<T> ensure(Predicate<? super T>, Supplier<? extends Problem>)`                                | guard: success keeps satisfying value, else fails with the supplied problem |
| `Outcome<T> otherwise(Supplier<? extends Outcome<T>>)`                                                | lazy fallback strategy; evaluated only on failure |
| `static <T> Outcome<List<T>> sequence(Iterable<? extends Outcome<T>>)`                                | combine outcomes, accumulating all problems  |
| `static <T> Outcome<List<T>> sequence(Outcome<T>...)`                                                 | varargs convenience overload of `sequence`   |
| `static <S,T> Outcome<List<T>> traverse(Iterable<S>, Function<? super S,? extends Outcome<T>>)`       | map + sequence in one step                   |
| `static <A,B,R> Outcome<R> zip(Outcome<A>, Outcome<B>, BiFunction<? super A,? super B,? extends R>)`  | typed combination of two outcomes, accumulating all problems |
| `static <A,B,C,R> Outcome<R> zip(Outcome<A>, Outcome<B>, Outcome<C>, TriFunction<A,B,C,R>)`           | typed combination of three outcomes          |

Nested functional interfaces: `Outcome.ThrowingSupplier<T>` (a supplier that may
throw checked exceptions) and `Outcome.TriFunction<A,B,C,R>` (three-argument
function for the three-source `zip`).

### `Success<T>` (record)

`Success<T>(T value)` — the value may not be `null`.

### `Failure<T>` (record)

`Failure<T>(Problems problems)` — the problems may not be `null`, and `Problems`
always contains at least one problem.

### `Problems`

An immutable record snapshot of one or more `Problem`s.

| Method                                      | Description                                 |
|---------------------------------------------|---------------------------------------------|
| `int size()`                                | number of problems                          |
| `Problem first()`                           | first problem                               |
| `List<Problem> all()`                       | immutable view of all problems, in order    |
| `boolean contains(Problem)`                 | semantic equality check                     |
| `boolean hasCode(String)`                   | whether any problem carries the code        |
| `List<Problem> byCode(String)`              | all problems with the code, immutable, possibly empty |
| `List<Problem> byType(ProblemType)`         | all problems of the type, immutable, possibly empty |
| `Problems map(Function<Problem,Problem>)`   | transform every problem, preserving order   |

### `Problem`

An immutable problem with:

- `String code` — non-blank
- `String description` — non-blank
- `ProblemType type` — non-null
- `Map<String, Object> metadata` — immutable; `null` normalizes to an empty map
- `Throwable cause` — optional

**Equality** is based on `code`, `description`, `type` and `metadata`. The
`cause` is diagnostic information and does not participate in equality.

Typed factories remove the ceremony of the canonical constructor:

```java
Problem.of(code, description, type)     // generic
Problem.validation(code, description)
Problem.notFound(code, description)
Problem.conflict(code, description)
Problem.unauthorized(code, description)
Problem.forbidden(code, description)
Problem.dependency(code, description)
Problem.timeout(code, description)
Problem.unavailable(code, description)
Problem.internal(code, description)

// derived copies (record-with style); the original is never mutated
Problem enriched = Problem.timeout("DB_TIMEOUT", "slow")
        .withCause(sqlTimeoutException)
        .withMetadata("queryId", queryId);
```

### `ProblemType`

`VALIDATION`, `NOT_FOUND`, `CONFLICT`, `UNAUTHORIZED`, `FORBIDDEN`,
`DEPENDENCY`, `TIMEOUT`, `UNAVAILABLE`, `INTERNAL`.

## Invariants

- `Success` never holds a `null` value.
- `Failure` always holds a non-empty `Problems`.
- `Problems` is immutable, preserves insertion order, and never deduplicates.
- `Problem.metadata` is immutable after construction.
- Null is rejected for every function argument, and any callback (`map`,
  `flatMap`, `recover`, `recoverWith`, `ensure`, `otherwise`, `catching`,
  `zip`) that returns `null` fails fast with a `NullPointerException` — a null
  result is a programming-contract violation, never a domain problem.
- `catching` converts only `Exception`; fatal JVM errors (`Error`) propagate.
- `orElse`, `orElseGet` and `orElseThrow` never return null.
- `Outcome` values are immutable; no operation ever mutates the instance it
  is called on.

## Building

```bash
./gradlew build
./gradlew test
```
