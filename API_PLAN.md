# Plan de API — Paridad funcional con Vavr manteniendo la filosofía ligera

Propuesta de evolución del API de `Outcome` para cerrar la brecha funcional con las
librerías de result-types maduras (Vavr `Try`/`Either`/`Validation`) **sin** romper la
filosofía *deliberately small*: cero dependencias de producción, sin conversiones a
`Optional`, sin colecciones funcionales, sin async/retry.

Este documento complementa a `PROPOSALS.md` e **incorpora los ajustes de la revisión**
externa (`OUTCOME_API_PLAN_REVIEW.md`): mapper de `catching` sobre `Exception`,
solo `otherwise` perezoso, `ensure` con wildcards, `zip` limitado a 2 y 3 fuentes,
`Problem.of(...)` genérico antes que factories específicas, sin `Problems.stream()` y
fail-fast explícito en `withCause(null)`. Las decisiones quedan resueltas en la
sección 10.

---

## 1. Objetivo

Que un consumidor pueda expresar flujos completos — levantar código que lanza
excepciones, validar, encadenar fallbacks y combinar resultados tipados acumulando
errores — usando únicamente `Outcome`/`Problem`/`Problems`, con la misma potencia que
ofrece Vavr para sus tipos equivalentes.

Regla de admisión para cada método nuevo:

> Debe aportar una semántica que las primitivas existentes no expresen claramente o
> eliminar una cantidad significativa de boilerplate.

## 2. Principios no negociables

- **Cero dependencias en runtime** — todo se construye sobre `java.*`.
- **No `Optional`** — explícitamente fuera del API (decisión del autor).
- Inmutabilidad total; ninguna operación muta la instancia.
- Fail-fast en `null`: argumentos nulos y callbacks que producen `null` lanzan
  `NullPointerException` con mensajes descriptivos y contextuales
  (`"map mapper cannot return null"`, `"ensure predicate cannot be null"`, etc.).
- Acumulación de problemas: ningún combinador acumulativo pierde errores.
- Cobertura JaCoCo ≥ 85 % y mutación PIT ≥ 85 % se mantienen tras cada fase.
- Superficie pública mínima: los tipos nuevos son interfaces funcionales anidadas;
  el top-level del package se mantiene en 6 tipos públicos.

## 3. Brecha actual vs objetivo

| Capacidad | Vavr | Outcome hoy | Propuesto |
|---|---|---|---|
| Encadenar con short-circuit | `flatMap` | ✅ `flatMap` | — |
| Acumular problemas | Validation `.ap()` | ✅ `sequence`/`traverse` | — |
| Recuperación | `recover`/`recoverWith` | ✅ | — |
| Transformar problemas | `leftMap` | ✅ `mapProblem` | — |
| Levantar código que lanza excepciones | `Try.of(...)` | ❌ | **Fase 2** |
| Lambdas con excepciones *checked* | nativo | ❌ | **Fase 2** |
| Guarda de validación en cadena | `filterOrElse` | ❌ | **Fase 2** |
| Cadena de fallbacks entre outcomes | `orElse(Either)` | ❌ | **Fase 2** |
| Combinación tipada (conserva tipos) | `combine().ap()` | ❌ (`sequence` → `List<T>`) | **Fase 3** |
| Fábricas ergonómicas de `Problem` | — | ⚠️ solo constructor | **Fase 4** |
| Consultas sobre `Problems` | — | ⚠️ solo `contains` | **Fase 4** |

Lo que **no** se copia de Vavr (filosofía ligera): conversiones a `Optional`/`Stream`
sobre `Outcome`, colecciones funcionales, `Lazy`/`Future`, helpers de pattern matching
(Java 21 sealed + records ya lo dan gratis).

---

## 4. Fase 1 — Consolidación de lo existente

Los elementos del "bloque 1" ya están implementados (`Outcome.success/failure`,
`sequence(Iterable)` + varargs, `traverse`, familia `orElse*`). Esta fase solo cierra
su contrato:

1. Verificar mensajes NPE consistentes en toda la superficie actual.
2. Confirmar que `sequence(varargs)` delega en la versión `Iterable`
   (una sola implementación de la semántica) y que entrada vacía produce
   `Success(List.of())`.
3. Confirmar que `Automatic-Module-Name` sigue presente en el manifest del jar.
4. Suite verde con umbrales JaCoCo/PIT.

Sin código nuevo de API: solo pruebas de cierre y documentación si hace falta.

---

## 5. Fase 2 — Entrada desde excepciones y control de flujo

### 5.1 Nuevo tipo anidado: `Outcome.ThrowingSupplier`

```java
@FunctionalInterface
public interface ThrowingSupplier<T> {
    T get() throws Exception;
}
```

- Anidada dentro de `Outcome` (no agrega un tipo top-level) pero **pública**: forma
  parte de la firma de métodos públicos y el consumidor debe poder nombrarla.
- Una sola interfaz cubre ambos casos: un lambda que lanza excepciones *checked* y un
  `Supplier` común (por target-typing). No se agrega sobrecarga con `Supplier` para
  evitar ambigüedad en la resolución de lambdas.
- No se agregan más interfaces (`ThrowingFunction`, `ThrowingConsumer`,
  `ThrowingRunnable`) hasta que exista una necesidad real.

### 5.2 `catching` — equivalente a `Try.of`

```java
// Ejecuta work; cualquier Exception se convierte en un Problem interno.
// Los Error (OOM, StackOverflowError, VirtualMachineError) NO se atrapan: son fatales.
static <T> Outcome<T> catching(ThrowingSupplier<? extends T> work)

// Igual, pero el llamador decide cómo mapear la excepción a Problem.
// El mapper no puede ser null ni producir null.
//
// AJUSTE POR REVISIÓN: el mapper recibe Exception, NO Throwable.
// El primer overload solo atrapa Exception y deja pasar Error; la firma
// debe reflejar exactamente lo que el mapper puede recibir.
static <T> Outcome<T> catching(
        ThrowingSupplier<? extends T> work,
        Function<? super Exception, ? extends Problem> toProblem)
```

Mapeo por defecto:

| Campo | Valor |
|---|---|
| `type` | `INTERNAL` |
| `code` | `"UNEXPECTED_FAILURE"` |
| `description` | `ex.getMessage()`; si es nulo o blank, `ex.getClass().getName()` |
| `metadata` | vacío |
| `cause` | la excepción original |

```java
// Antes (obligatorio try/catch manual):
User user;
try {
    user = parse(json);
} catch (Exception e) {
    return Outcome.failure(new Problem("PARSE_FAILED", e.getMessage(),
            ProblemType.INTERNAL, null, e));
}

// Después:
Outcome<User> user = Outcome.catching(() -> parse(json));

// Con mapeo custom:
Outcome<User> user = Outcome.catching(
        () -> parse(json),
        ex -> new Problem("PARSE_FAILED", "JSON inválido",
                ProblemType.VALIDATION,
                Map.of("detail", String.valueOf(ex.getMessage())), ex));

// Lambdas con excepciones checked SIN envolver:
Outcome<Path> dir = Outcome.catching(() -> Files.createDirectory(path)); // IOException checked ✓

// Encadenado natural con el API existente:
Outcome<Report> report = Outcome.catching(() -> Files.readString(configPath))
        .map(this::parseConfig)
        .flatMap(this::generateReport);
```

### 5.3 `ensure` — guarda de validación dentro de la cadena

```java
// AJUSTE POR REVISIÓN: wildcard en el supplier, consistente con el resto del API.
Outcome<T> ensure(
        Predicate<? super T> predicate,
        Supplier<? extends Problem> problem);
```

Validaciones explícitas fail-fast: `predicate` no nulo, `problem` no nulo y el
problema producido por el supplier no nulo.

| Caso | Resultado |
|---|---|
| `Success` y predicado cumple | `this` (sin copia) |
| `Success` y predicado falla | `Failure(problem.get())` |
| `Failure` | `this`; predicado y supplier **nunca** se ejecutan |

El nombre expresa la condición explícitamente (a diferencia de un `filter` genérico,
que puede ocultar que se está generando un fallo); elimina el boilerplate del patrón
equivalente con `flatMap`.

```java
Outcome<User> active = loadUser(id)
        .ensure(u -> u.isActive(),
                () -> Problem.conflict("USER_INACTIVE", "el usuario está suspendido"));

// En cadena con acumulación downstream:
Outcome<List<Order>> payable = Outcome.traverse(orderIds,
        id -> loadOrder(id).ensure(Order::isPayable,
                () -> Problem.validation("ORDER_NOT_PAYABLE",
                        "la orden " + id + " no es pagable")));
```

### 5.4 `otherwise` — cadena de fallbacks perezosos

El nombre `orElse` ya está tomado por el fallback de valor, así que el fallback de
*outcome* se llama `otherwise`.

```java
// AJUSTE POR REVISIÓN: SOLO la variante perezosa en el primer corte.
// El overload eager otherwise(Outcome<T>) obliga a construir el fallback antes
// de saber si se necesita y su nombre no comunica esa evaluación anticipada;
// se podrá agregar después si aparece un caso real.
Outcome<T> otherwise(Supplier<? extends Outcome<T>> fallback);
```

| Caso | Resultado |
|---|---|
| `Success` | `this`; el supplier **nunca** se evalúa |
| `Failure` + fallback `Success` | el `Success` del fallback |
| `Failure` + fallback `Failure` | el `Failure` del fallback; los problemas originales **se reemplazan** |

`otherwise` significa *"intenta otra estrategia"* — no *"acumula errores de todas las
estrategias"*. Para acumular existen `zip`, `sequence` y `traverse`. La semántica de
reemplazo queda documentada explícitamente en el Javadoc.

```java
// Cascada clásica caché → DB → API; cada fuente solo se consulta si la anterior falló:
Outcome<User> user = cacheLookup(id)
        .otherwise(() -> dbLookup(id))
        .otherwise(() -> apiLookup(id));
```

---

## 6. Fase 3 — Combinación tipada (`zip`)

`sequence` pierde los tipos (`List<T>`). `zip` conserva el tipo de cada fuente por
posición y **acumula** los problemas de todos los fallos. Es el equivalente al
`combine(...).ap(...)` de Vavr Validation.

### Nuevo tipo anidado: `Outcome.TriFunction`

```java
@FunctionalInterface
public interface TriFunction<A, B, C, R> {
    R apply(A a, B b, C c);
}
```

Su razón de existir es concreta: Java solo ofrece `Function` y `BiFunction`. El límite
se mantiene aquí — **no** habrá `QuadFunction` ni aridades arbitrarias.

### Métodos nuevos en `Outcome`

```java
static <A, B, R> Outcome<R> zip(
        Outcome<A> first, Outcome<B> second,
        BiFunction<? super A, ? super B, ? extends R> combiner);

static <A, B, C, R> Outcome<R> zip(
        Outcome<A> first, Outcome<B> second, Outcome<C> third,
        TriFunction<? super A, ? super B, ? super C, ? extends R> combiner);

// AJUSTE POR REVISIÓN: SOLO aridades 2 y 3. Sin zip4/zip5/... — para más
// fuentes: componer zip, o usar sequence si los tipos son homogéneos.
```

Semántica:

- Todos `Success` → `Success(combiner(v1, v2, ...))`; resultado `null` → NPE
  fail-fast con mensaje contextual (`"zip combiner cannot return null"`).
- Uno o más `Failure` → `Failure` con **todos** los problemas acumulados en orden;
  el combiner nunca se ejecuta.
- Argumentos nulos → NPE inmediata.

```java
// Antes con sequence (pierde tipos):
Outcome<List<Object>> parts = Outcome.sequence(loadUser(id), loadProfile(id)); // List<Object>

// Después con zip (tipos preservados):
Outcome<Account> account = Outcome.zip(
        loadUser(id), loadProfile(id), loadSettings(id),
        Account::new);

// Dos fuentes:
Outcome<Transfer> transfer = Outcome.zip(fromAccount, toAccount, Transfer::new);
```

Responde la pregunta abierta #2 de `PROPOSALS.md`: función combinadora en vez de
`Pair`/tuplas — cero tipos públicos de datos nuevos.

---

## 7. Fase 4 — Ergonomía de `Problem` / `Problems`

### 7.1 Factory genérica primero, luego fábricas por tipo justificadas

```java
// AJUSTE POR REVISIÓN: primero una factory genérica sólida:
Problem.of(code, description, type)

// Después, fábricas específicas para los tipos de uso frecuente:
Problem.validation(code, description)
Problem.notFound(code, description)
Problem.conflict(code, description)
Problem.unauthorized(code, description)
Problem.forbidden(code, description)
Problem.dependency(code, description)
Problem.timeout(code, description)
Problem.unavailable(code, description)
Problem.internal(code, description)
```

Regla: **no** se crea una factory automáticamente por cada constante del enum; solo
cuando el tipo tenga semántica clara y sin solapes. Los 9 valores actuales de
`ProblemType` cumplen ese criterio (verificados contra el enum real; hoy no existe
ningún tipo ambiguo tipo `INVALID`). Si en el futuro se agregan constantes al enum,
cada factory nueva pasa por esta revisión semántica.

```java
// Antes:
new Problem("USER_NOT_FOUND", "no such user", ProblemType.NOT_FOUND, null, null);
// Después:
Problem.notFound("USER_NOT_FOUND", "no such user");
```

El constructor canónico sigue disponible para metadata y causa.

### 7.2 Consultas en `Problems`

```java
boolean hasCode(String code)            // routing rápido
List<Problem> byCode(String code)       // 0..N resultados; coherente con acumulación
List<Problem> byType(ProblemType type)  // 0..N
```

- Sin `Optional`: `findByCode(...): Optional<Problem>` quedó descartado por decisión
  del autor; `hasCode` + `byCode` cubren los dos usos.
- **AJUSTE POR REVISIÓN: sin `stream()`** — no aporta capacidad nueva al modelo, es
  integración con otra abstracción y abriría la puerta a convertir `Problems` en una
  mini colección funcional. Ya es `Iterable`; iterar es suficiente.
- Las listas devueltas por `byCode`/`byType` son **inmutables**.

### 7.3 Copias derivadas en `Problem` (estilo record-with)

```java
Problem withCause(Throwable cause)               // agrega/reemplaza la causa diagnóstica
Problem withMetadata(Map<String, Object> extra)  // merge con el metadata actual
Problem withMetadata(String key, Object value)   // merge de una entrada
```

- **AJUSTE POR REVISIÓN:** `withCause(null)` lanza NPE — `null` nunca tiene
  significado implícito de "eliminar"; mantener el contrato fail-fast. Una futura
  operación explícita `withoutCause()` podría agregarse, pero no ahora.
- `withMetadata(Map)` hace **merge** (no reemplazo silencioso); para claves repetidas,
  el nuevo valor reemplaza al anterior.
- Se preserva la decisión de igualdad semántica: `cause` no participa en
  `equals`/`hashCode`.

```java
Problem base = Problem.timeout("DB_TIMEOUT", "la BD no respondió");
Problem enriched = base.withCause(sqlTimeoutException)
                       .withMetadata("queryId", queryId);
// 'base' queda intacto — inmutabilidad garantizada
```

---

## 8. Fase 5 — Endurecimiento técnico

1. **`Success.map`:** alinear el NPE con `flatMap` — check explícito con mensaje
   `"map mapper cannot return null"` en vez del genérico del constructor del record
   (sección 5.1 de `PROPOSALS.md`).
2. **Consistencia NPE en todos los callbacks** (existente y nuevo): `map`,
   `flatMap`, `recover`, `recoverWith`, combiners de `zip`, supplier de `ensure`,
   fallback de `otherwise`, mapper de `catching` — mismo patrón: mensaje contextual,
   fail-fast.
3. **ArchTest:** actualizar `publicApiIsLimited` — los tipos anidados nuevos viven
   dentro de `Outcome`, así que el set de clases públicas top-level no cambia; agregar
   verificación de que las interfaces anidadas sean `public static` y funcionales.
4. **README:** nueva tabla de API + sección de ejemplos sincronizada con este plan.
5. **PROPOSALS.md:** marcar secciones implementadas; mover lo restante a backlog.
6. Mantener umbrales JaCoCo ≥ 85 % / PIT ≥ 85 % en todo momento.

---

## 9. Tabla semántica consolidada (métodos nuevos)

| Operación | `Success<T>` | `Failure<T>` |
|---|---|---|
| `catching(work)` *(estática)* | ejecuta `work` | — (una `Exception` produce `Failure`) |
| `ensure(pred, sup)` | cumple → `this`; no cumple → `Failure(sup.get())` | `this`; nada se ejecuta |
| `otherwise(sup)` | `this`; el supplier no se evalúa | el outcome del fallback (reemplaza problemas) |
| `zip(a, b, f)` | combina valores | acumula **todos** los problemas |

Identidad de cada operación (por qué existe):

```text
catching   -> entrar desde código que lanza excepciones
ensure     -> imponer una condición sobre un Success
otherwise  -> intentar una estrategia alternativa
zip        -> combinar tipos distintos acumulando fallos
sequence   -> acumular resultados homogéneos
traverse   -> transformar y acumular una colección
```

Invariantes que se preservan en todo lo nuevo:

- Ningún callback se ejecuta en la rama que no corresponde.
- Ningún resultado intermedio puede ser `null` (NPE fail-fast con mensaje contextual).
- Los problemas nunca se deduplican ni se pierden en `zip`.
- Ninguna operación muta la instancia original.

---

## 10. Decisiones resueltas

1. **Nombres:** `catching` / `ensure` / `otherwise` — claros, sin colisiones y en
   línea con el estilo existente (`recover`, `orElse`).
2. **Ubicación de `ThrowingSupplier` y `TriFunction`:** anidadas dentro de `Outcome`
   pero **públicas** (parte de firmas públicas); el top-level del package se mantiene
   en 6 tipos públicos. En el uso diario nadie las escribe: los lambdas conforman por
   target-typing.
3. **Mapper de `catching`:** recibe `Exception`, no `Throwable` — la firma refleja
   exactamente lo que se atrapa (los `Error` pasan de largo).
4. **`ensure`:** `Supplier<? extends Problem>` con wildcards y validaciones null
   explícitas de predicate, supplier y problema producido.
5. **`otherwise`:** solo la variante perezosa en el primer corte; el overload eager
   espera un caso real. En `Failure`+`Failure` los problemas se **reemplazan**
   (semántica de "intenta otra estrategia"); documentado explícitamente.
6. **`zip`:** solo aridades 2 y 3; sin familia creciente.
7. **Fábricas de `Problem`:** `Problem.of(code, description, type)` genérica primero;
   factories específicas solo con semántica clara y revisada (los 9 tipos actuales la
   cumplen).
8. **Consultas:** `hasCode` / `byCode` / `byType` devolviendo listas inmutables;
   **sin** `stream()`.
9. **Copias derivadas:** `withCause(null)` → NPE (sin significado implícito);
   `withMetadata` hace merge con reemplazo por clave.
10. **Azúcar extra (`bimap`, `flatten`):** fuera — mínima superficie; `map` +
    `mapProblem` ya cubren el caso.

## 11. Orden de implementación y verificación

| Fase | Contenido | Verificación |
|---|---|---|
| 1 | Consolidación del bloque 1 existente | suite verde, mensajes NPE consistentes, umbrales respetados |
| 2 | `ThrowingSupplier` + `catching` ×2 + `ensure` + `otherwise` (lazy) | tests: éxito / excepción / `Error` fatal pasa de largo / mapper-null y mapper-null-result / lazy-eval / predicado falso; PIT |
| 3 | `zip` ×2 + `TriFunction` | tests: acumulación multi-fallo / tipos / combiner-null; PIT |
| 4 | `Problem.of` + fábricas + consultas + copias derivadas | EqualsVerifier donde aplique; listas inmutables verificado; PIT |
| 5 | fix NPE `map` + ArchTest + README + PROPOSALS | suite completa verde, umbrales respetados |

Commits: uno por fase, estilo convencional (`feat: add catching entry points`, etc.).

Cada método nuevo requiere tests exhaustivos: casos success/failure, evaluación perezosa,
argumentos nulos, resultados nulos y mensajes de excepción exactos.

## 12. Fuera de alcance (explícito)

- Conversiones `toOptional()` / `fromOptional()` / `stream()` sobre `Outcome` —
  rechazado por el autor.
- **`Problems.stream()`** — rechazado en revisión: `Iterable` ya basta y evitaría
  convertir `Problems` en una mini colección funcional.
- Overload eager `otherwise(Outcome<T>)` en el primer corte.
- Familia creciente `zip4`/`zip5`/... y `QuadFunction`+.
- `Pair`/`Triple` u otras tuplas.
- `ThrowingFunction`/`ThrowingConsumer`/`ThrowingRunnable` hasta necesidad real.
- Retry/resiliencia, adaptadores async (`CompletableFuture`), serialización,
  bindings de frameworks — candidatos futuros para un módulo `outcome-support`,
  nunca en el core.
