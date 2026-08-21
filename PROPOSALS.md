# Propuestas de mejora para Outcome

Análisis del estado actual de la librería y propuestas priorizadas para
fortalecer la API sin romper la filosofía *deliberately small*.

---

## Estado actual

Fortalezas detectadas:

- Tipos sellados (`sealed`) con invariantes claras y verificadas.
- Inmutabilidad garantizada (`records`, colecciones copiadas).
- Cobertura con JaCoCo (≥ 85 %), mutación con PIT (≥ 85 %) y reglas
  arquitectónicas con ArchUnit.
- Cero dependencias externas en producción.

Debilidades detectadas:

- No existen fábricas estáticas; la única construcción es `new Success<>(...)`
  / `new Failure<>(...)`.
- **No hay forma de extraer el valor de un `Success` sin pasar por `fold`.**
- No existe captura de excepciones (`try/catch` manual obligatorio).
- Faltan combinadores básicos (`traverse`, `combine` tipado, `filter`,
  fallback).
- `Problem`/`Problems` no ofrecen consultas (por código, por tipo) ni copias
  derivadas.
- Detalles técnicos: mensaje de NPE inconsistente en `Success.map`, sin soporte
  JPMS, sin anotaciones de nulabilidad.

---

## 1. Ergonomía de construcción — Prioridad: crítica

Hoy la única forma de crear valores es invocar los records directamente.
Falta lo idiomático en APIs modernas:

```java
Outcome.success(value)
Outcome.failure(problem)
Outcome.failure(problems)
```

Y el más valioso — captura de excepciones, hoy imposible sin `try/catch`
manual:

```java
static <T> Outcome<T> catching(Supplier<T> work)
// excepción -> Problem(type=INTERNAL, cause=excepción)

static <T> Outcome<T> catching(Supplier<T> work, Function<Throwable, Problem> toProblem)

static <T> Outcome<T> fromOptional(Optional<T> optional, Supplier<Problem> ifEmpty)
```

**Impacto:** reduce el boilerplate de adopción; `catching` es el punto de
entrada natural desde código imperativo que lanza excepciones.

---

## 2. Extracción del valor — Prioridad: crítica

Actualmente no hay forma de obtener el valor de un `Success` sin usar `fold`.
Esto obliga a ceremonia innecesaria:

```java
T orElse(T other)                                            // valor por defecto
T orElseGet(Supplier<? extends T>)                           // evaluación perezosa
<X extends Throwable> T orElseThrow(Function<Problems, X>)   // convertir a excepción
Optional<T> toOptional()                                     // puente con Optional
Stream<T> stream()                                           // interop con Streams
```

Semántica propuesta:

| Método        | `Success<T>`            | `Failure<T>`                          |
|---------------|-------------------------|---------------------------------------|
| `orElse`      | devuelve el valor       | devuelve `other`                      |
| `orElseGet`   | devuelve el valor       | ejecuta el supplier                   |
| `orElseThrow` | devuelve el valor       | lanza la excepción producida          |
| `toOptional`  | `Optional.of(value)`    | `Optional.empty()`                    |
| `stream`      | `Stream.of(value)`      | `Stream.empty()`                      |

**Impacto:** cierra el ciclo completo de la librería (construir → transformar →
consumir). Sin esto, el consumidor siempre paga el costo de `fold`.

---

## 3. Combinadores que faltan — Prioridad: alta

| Método                                        | Utilidad                                                                 |
|-----------------------------------------------|--------------------------------------------------------------------------|
| `<S,T> traverse(Iterable<S>, Function<S, Outcome<T>>)` | `map` + `sequence` en un paso, acumulando problemas              |
| `combine(o1, o2)` / `combine(o1, o2, o3)`     | `sequence` pierde los tipos (`List<T>`); combine los preserva por posición |
| `filter(Predicate<? super T>, Supplier<Problem>)` | validar el valor dentro del flujo; falla con el problema dado        |
| `otherwise(Outcome<T>)` / `or(Supplier<Outcome<T>>)` | encadenar fallbacks; solo se evalúa el primero exitoso            |

Ejemplo de uso:

```java
Outcome<User> user = Outcome.combine(loadUser(id), loadProfile(id), loadSettings(id))
        .map(User::new); // cada carga mantiene su tipo

Outcome<Account> acc = findCached(id)
        .otherwise(() -> fetchFromDb(id));
```

---

## 4. Consultas sobre Problem / Problems — Prioridad: media

```java
boolean hasCode(String code)                 // routing/lógica por código
Optional<Problem> findByCode(String code)
List<Problem> byType(ProblemType type)
Stream<Problem> stream()

Problem withCause(Throwable cause)           // copias derivadas estilo record-with
Problem withMetadata(String key, Object value)

// Fábricas por tipo:
Problem.validation(code, description)
Problem.notFound(code, description)
Problem.conflict(code, description)
Problem.unauthorized(...), forbidden(...), dependency(...),
timeout(...), unavailable(...), internal(...)
```

**Nota:** las fábricas por tipo son azúcar sobre `new Problem(code, desc,
type, null, null)`; no agregan acoplamiento.

---

## 5. Endurecimiento técnico — Prioridad: alta

1. **`Success.map`:** si el mapper retorna `null`, el NPE proviene del
   constructor del record con el mensaje `"value cannot be null"` — confuso.
   Alinear con `flatMap`, que sí valida explícitamente
   (`"flatMap mapper cannot return null"`). Agregar check con mensaje
   `"map mapper cannot return null"`.
2. **JPMS:** agregar como mínimo `Automatic-Module-Name:
   io.github.daniel99412.outcome` al manifest del jar. Un `module-info.java`
   completo es opcional (evaluar compatibilidad con herramientas).
3. **Anotaciones de nulabilidad** (JSpecify, alcance `compileOnly`):
   documentación machine-readable de dónde puede haber `null`, sin romper
   "dependency-free" en runtime.
4. **`ProblemException extends RuntimeException`** opcional: transporta los
   `Problems` dentro; complementa `orElseThrow` para integración con código
   imperativo y frameworks que esperan excepciones.
5. **Varargs overload** de `sequence`: `sequence(Outcome<T>...)` para llamadas
   cortas.

---

## Fuera de alcance

Respeta la filosofía *deliberately small* del README. Si acaso, como módulo
separado futuro (`outcome-support`):

- Retry/resiliencia.
- Adaptadores asíncronos (`CompletableFuture`).
- Serialización.
- Bindings de frameworks (Spring, Jakarta Validation, etc.).

---

## Plan de implementación sugerido

| Fase | Contenido                                   |
|------|---------------------------------------------|
| 1    | Secciones 1 + 2 + 5.1 (fábricas, extracción, fix de `map`) |
| 2    | Sección 3 (combinadores)                     |
| 3    | Secciones 4 + resto de 5                     |

Cada fase debe mantener: cobertura JaCoCo ≥ 85 %, umbral PIT ≥ 85 %, reglas de
ArchTest actualizadas (`publicApiIsLimited` debe incluir los nuevos tipos
públicos, p. ej. `ProblemException`), y README sincronizado con la tabla de la
API.

## Preguntas abiertas

1. ¿Cuáles secciones entran al primer release? Recomendación mínima viable:
   fases 1.
2. ¿`combine` para 2 y 3 argumentos con qué forma de retorno — un
   `record Pair<A,B>` nuevo, o versión con función combinadora
   `combine(o1, o2, BiFunction)`?
3. ¿Las fábricas `Problem.validation(...)` etc. van como statics en `Problem`?
4. ¿Se actualiza el README (tabla de API) como parte del trabajo?
