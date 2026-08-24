# Revisión de API_PLAN.md — Outcome

## Veredicto general

El plan va en una dirección mucho mejor que la propuesta anterior. Ya toma varias decisiones importantes que antes estaban abiertas:

- mantiene el core sin dependencias;
- elimina Optional del alcance;
- evita copiar Vavr indiscriminadamente;
- usa interfaces funcionales anidadas para no inflar el package público;
- separa claramente short-circuit, fallback y acumulación;
- define fail-fast para null;
- prioriza pruebas y mutación como parte del diseño.

Mi recomendación es:

- **Sí implementar:** Fase 2, Fase 3 y la mayor parte de Fase 4 y Fase 5.
- **Implementar con cambios:** Fase 1 y algunas partes de Fase 4.
- **No implementaría tal cual:** algunos detalles concretos de `catching`, `otherwise`, `stream()` y la lista completa de factories.

El punto más importante: el plan ya es bueno, pero no metería todo sin ajustar esas semánticas primero.

---

# 1. Cambios que sí implementaría

## 1.1 `ensure(...)`

### Propuesta

```java
Outcome<T> ensure(
    Predicate<? super T> predicate,
    Supplier<Problem> problem
);
```

### Decisión

**Sí.**

### Justificación

Antes era razonable rechazar un `filter`, porque un `filter` genérico puede ocultar que estamos generando un fallo.

`ensure` es distinto: el nombre expresa explícitamente una condición que debe cumplirse para mantener el `Success`.

```java
loadUser(id)
    .ensure(
        User::isActive,
        () -> Problem.conflict(
            "USER_INACTIVE",
            "El usuario está suspendido"
        )
    );
```

La semántica es clara:

- `Success` + predicado verdadero → `this`.
- `Success` + predicado falso → `Failure`.
- `Failure` → `this`.
- En `Failure` no se ejecutan ni predicado ni supplier.

Además, evita bastante boilerplate respecto a:

```java
.flatMap(value ->
    predicate.test(value)
        ? Outcome.success(value)
        : Outcome.failure(problem.get())
)
```

Eso justifica su existencia como primitiva.

---

## 1.2 `zip(...)`

### Propuesta

```java
static <A, B, R> Outcome<R> zip(
    Outcome<A> first,
    Outcome<B> second,
    BiFunction<? super A, ? super B, ? extends R> combiner
);
```

y una variante de tres argumentos.

### Decisión

**Sí.**

### Justificación

Esta API resuelve una limitación real de `sequence`.

`sequence`:

```text
Outcome<T> + Outcome<T> + ...
-> Outcome<List<T>>
```

Eso funciona para valores homogéneos.

Pero:

```java
loadUser(id)
loadProfile(id)
loadSettings(id)
```

tienen tipos diferentes.

`zip` permite:

```java
Outcome<Account> account = Outcome.zip(
    loadUser(id),
    loadProfile(id),
    loadSettings(id),
    Account::new
);
```

y conserva los tipos.

Lo más importante es que su semántica sea acumulativa:

- todos `Success` → ejecutar combinador;
- uno o varios `Failure` → acumular todos los problemas;
- el combinador no se ejecuta si hay algún fallo.

Eso encaja perfectamente con la filosofía actual:

```text
flatMap -> short-circuit
zip     -> accumulation
sequence -> accumulation
traverse -> accumulation
```

---

## 1.3 `TriFunction` anidada

### Propuesta

```java
@FunctionalInterface
interface TriFunction<A, B, C, R> {
    R apply(A a, B b, C c);
}
```

dentro de `Outcome`.

### Decisión

**Sí, si se mantiene sólo para soportar el overload de `zip` de tres argumentos.**

### Justificación

Antes rechazábamos crear `TriFunction` como tipo público top-level porque sería ampliar innecesariamente la librería.

Anidarla en `Outcome` cambia bastante el panorama:

```java
Outcome.TriFunction
```

no introduce otro concepto central del package ni pretende convertirse en una librería de funciones.

Su razón de existir es concreta: Java sólo ofrece `Function` y `BiFunction`.

El límite debe mantenerse aquí. No abriría:

- `QuadFunction`
- `PentaFunction`
- funciones de aridad arbitraria.

Dos y tres fuentes cubren la mayoría de casos sin convertir Outcome en una librería funcional.

---

## 1.4 Fábricas estáticas de `Outcome`

Mantendría:

```java
Outcome.success(value);

Outcome.failure(problem);

Outcome.failure(problems);

Outcome.failure(problem1, problem2);
```

### Decisión

**Sí.**

### Justificación

Desacoplan al consumidor de:

```java
new Success<>(...)
new Failure<>(...)
```

y hacen la API más expresiva.

También permiten que `Success` y `Failure` sigan siendo implementaciones del sealed type sin obligar al consumidor a conocer detalles internos.

---

## 1.5 `sequence(...)` con varargs

### Propuesta

```java
Outcome.sequence(
    first,
    second,
    third
);
```

### Decisión

**Sí.**

### Justificación

Es ergonomía pura sobre una operación ya definida.

Debe delegar a la versión basada en `Iterable` para mantener una sola implementación de la semántica.

La entrada vacía debe seguir devolviendo:

```java
Success(List.of())
```

---

## 1.6 `traverse(...)`

### Decisión

**Sí.**

### Justificación

Es una composición natural de:

```text
map + sequence
```

y elimina boilerplate real.

Debe:

- procesar todos los elementos;
- conservar el orden de los éxitos;
- acumular todos los problemas;
- no perder fallos posteriores.

---

## 1.7 Fábricas por tipo en `Problem`

### Propuesta

```java
Problem.validation(...)
Problem.notFound(...)
Problem.conflict(...)
...
```

### Decisión

**Sí, con un pequeño matiz de alcance.**

### Justificación

El documento ya demuestra que el constructor canónico es verboso:

```java
new Problem(
    "USER_NOT_FOUND",
    "no such user",
    ProblemType.NOT_FOUND,
    null,
    null
);
```

Las factories reducen ruido y hacen el código más expresivo.

Yo sí agregaría las factories para los tipos de uso frecuente, pero no las trataría como requisito para todos los valores del enum automáticamente.

Primero aseguraría que exista una factory genérica sólida:

```java
Problem.of(code, description, type);
```

Después:

```java
Problem.validation(...)
Problem.notFound(...)
Problem.conflict(...)
Problem.unauthorized(...)
Problem.forbidden(...)
Problem.dependency(...)
Problem.timeout(...)
Problem.unavailable(...)
Problem.internal(...)
```

El caso `INVALID` merece una revisión aparte: si semánticamente se solapa con `VALIDATION`, no agregaría una factory sólo porque existe en el enum.

---

## 1.8 Consultas en `Problems`

### Propuesta

```java
boolean hasCode(String code);

List<Problem> byCode(String code);

List<Problem> byType(ProblemType type);
```

### Decisión

**Sí.**

### Justificación

Aquí el documento mejora una decisión anterior: como Optional está explícitamente fuera del API, no tiene sentido usar:

```java
Optional<Problem> findByCode(...)
```

Además, `byCode` es más coherente con el modelo de acumulación porque puede haber múltiples problemas con el mismo código.

La combinación:

```java
hasCode(...)
byCode(...)
byType(...)
```

cubre consultas comunes sin perder información.

Las listas devueltas deben ser inmutables.

---

## 1.9 Copias derivadas de `Problem`

### Propuesta

```java
Problem withCause(Throwable cause);

Problem withMetadata(Map<String, Object> extra);

Problem withMetadata(String key, Object value);
```

### Decisión

**Sí.**

### Justificación

Encajan muy bien con un modelo inmutable.

Por ejemplo:

```java
Problem enriched = base
    .withCause(exception)
    .withMetadata("queryId", queryId);
```

Evita reconstruir manualmente:

```text
code
description
type
metadata
cause
```

Debe preservarse la decisión anterior:

> `cause` es información diagnóstica y no participa en la igualdad semántica ni en `hashCode`.

`withMetadata(Map...)` debe hacer merge y no reemplazar silenciosamente todo el metadata.

Para claves repetidas, el nuevo valor debe reemplazar al anterior.

---

## 1.10 Endurecimiento técnico de `map`

### Decisión

**Sí.**

### Justificación

Un callback que devuelve `null` viola el contrato del Outcome.

Por consistencia:

```text
Resultado de dominio esperado
-> Success / Failure

Violación de contrato del programador
-> NullPointerException
```

El mensaje contextual:

```text
map mapper cannot return null
```

es mejor que depender de un mensaje genérico del constructor.

La misma filosofía debe mantenerse en:

- `map`
- `flatMap`
- `recover`
- `recoverWith`
- `zip` combiners
- factories o mappers que produzcan `null`.

---

## 1.11 ArchTest, documentación y calidad

### Decisión

**Sí.**

### Justificación

No son adornos.

Si el objetivo es una librería pública:

- ArchTest protege la superficie pública;
- README evita que la API real y la documentada diverjan;
- JaCoCo protege cobertura;
- PIT es especialmente útil para detectar tests que ejecutan código pero no verifican realmente su comportamiento.

La regla de un commit por fase también me parece correcta.

---

# 2. Cambios que implementaría, pero modificados

## 2.1 `catching(...)`

### Propuesta actual

```java
static <T> Outcome<T> catching(
    ThrowingSupplier<? extends T> work
)
```

y:

```java
static <T> Outcome<T> catching(
    ThrowingSupplier<? extends T> work,
    Function<? super Throwable, ? extends Problem> toProblem
)
```

### Decisión

**Sí, pero cambiaría el segundo mapper.**

Yo usaría:

```java
Function<? super Exception, ? extends Problem>
```

en lugar de:

```java
Function<? super Throwable, ? extends Problem>
```

### Justificación

El primer overload declara explícitamente que atrapa:

```text
Exception
```

y deja pasar:

```text
Error
```

Entonces el mapper debería reflejar exactamente lo que puede recibir.

Usar `Throwable` sugiere que podría recibir:

- `OutOfMemoryError`
- `StackOverflowError`
- `VirtualMachineError`

pero esos errores no se están atrapando.

La API debe expresar la semántica real.

Propuesta:

```java
static <T> Outcome<T> catching(
    ThrowingSupplier<? extends T> work,
    Function<? super Exception, ? extends Problem> toProblem
);
```

---

## 2.2 `ThrowingSupplier`

### Decisión

**Sí, con una aclaración de visibilidad.**

La idea de tenerlo anidado dentro de `Outcome` es buena:

```java
Outcome.ThrowingSupplier<T>
```

pero si forma parte de la firma de un método público, debe ser accesible al consumidor.

Por lo tanto:

```java
@FunctionalInterface
public interface ThrowingSupplier<T> {
    T get() throws Exception;
}
```

anidada dentro de `Outcome`.

### Justificación

No añade un tipo top-level nuevo y resuelve el problema real de las checked exceptions.

No agregaría más interfaces todavía:

- `ThrowingFunction`
- `ThrowingConsumer`
- `ThrowingRunnable`

hasta que exista una necesidad real.

---

## 2.3 `otherwise(...)`

### Propuesta

```java
Outcome<T> otherwise(Outcome<T> other);

Outcome<T> otherwise(
    Supplier<? extends Outcome<T>> other
);
```

### Decisión

**Sí, pero cuestionaría el overload eager.**

El overload:

```java
otherwise(Outcome<T> other)
```

obliga al caller a tener el fallback ya construido.

Eso significa que este código:

```java
primary().otherwise(expensiveFallback());
```

ejecuta:

```java
expensiveFallback()
```

antes de que `otherwise` pueda decidir si hace falta.

El nombre no comunica claramente esa evaluación eager.

### Mi recomendación

Para el core inicial dejaría sólo:

```java
Outcome<T> otherwise(
    Supplier<? extends Outcome<T>> fallback
);
```

Si después aparece un caso real donde ya tienes otro `Outcome` calculado y quieres elegirlo, el overload eager se puede agregar.

### Semántica

Estoy de acuerdo con el documento:

```text
Failure + Failure
-> se reemplazan los problemas originales
```

Eso debe documentarse claramente.

`otherwise` significa:

> intenta otra estrategia.

No:

> acumula errores de todas las estrategias.

Para acumulación ya existen:

```text
zip
sequence
traverse
```

---

## 2.4 `ensure(...)` y el supplier de problema

### Propuesta

```java
Outcome<T> ensure(
    Predicate<? super T> predicate,
    Supplier<Problem> problem
);
```

### Decisión

**Sí, pero usaría wildcards.**

Propuesta:

```java
Outcome<T> ensure(
    Predicate<? super T> predicate,
    Supplier<? extends Problem> problem
);
```

### Justificación

Es ligeramente más flexible y consistente con el resto de la API.

También validaría explícitamente:

- predicate no null;
- supplier no null;
- resultado del supplier no null.

---

## 2.5 `zip(...)` de 2 y 3 elementos

### Decisión

**Sí, pero sólo 2 y 3 por ahora.**

### Justificación

La propuesta es buena porque la `TriFunction` está justificada por el overload de tres.

No seguiría con:

```text
zip4
zip5
zip6
...
```

Eso empieza a inflar el API y no escala bien en Java.

Dos y tres cubren los casos más comunes. Para más combinaciones:

- componer `zip`;
- usar `sequence` si los tipos son homogéneos;
- reevaluar un diseño futuro si aparece una necesidad real.

---

## 2.6 `withCause(...)`

### Decisión

**Sí, pero permitiría explícitamente decidir si `null` elimina la causa o si está prohibido.**

Mi preferencia:

```text
null -> NPE
```

Si se quiere eliminar una causa, mejor tener una operación explícita en el futuro:

```java
withoutCause()
```

aunque no la agregaría ahora.

### Justificación

Mantiene el contrato fail-fast y evita usar `null` como comando implícito.

---

# 3. Cambios que no implementaría tal cual

## 3.1 `Problems.stream()`

### Propuesta

```java
Stream<Problem> stream();
```

### Decisión

**No.**

### Justificación

El documento dice explícitamente que quiere mantener Outcome ligero y con una superficie pública mínima.

`stream()` no aporta una capacidad nueva al modelo de problemas; es una integración con otra abstracción.

Además, si `Problems` se convierte poco a poco en:

```text
Iterable
+ Stream
+ múltiples operaciones funcionales
+ conversiones
```

terminamos creando una mini colección funcional.

El usuario ya puede iterar los problemas si la estructura base lo permite.

Yo mantendría:

```java
hasCode
byCode
byType
```

y no abriría una API de streaming dentro de `Problems`.

---

## 3.2 No agregaría el overload eager de `otherwise` en el primer corte

Como se explicó arriba:

```java
otherwise(Outcome<T> other)
```

puede inducir evaluación innecesaria.

El lazy:

```java
otherwise(Supplier<? extends Outcome<T>> fallback)
```

resuelve el caso principal y es más seguro.

---

## 3.3 No copiaría automáticamente una factory por cada enum

No agregaría factories simplemente porque existe un `ProblemType`.

El enum actual debe mantener una semántica clara.

En particular revisaría:

```text
VALIDATION
INVALID
```

Si son categorías distintas, perfecto.

Si en la práctica ambas terminan representando el mismo tipo de fallo, agregar:

```java
Problem.validation(...)
Problem.invalid(...)
```

sólo aumenta ambigüedad.

Primero definiría el contrato de cada `ProblemType`.

---

# 4. Mi orden recomendado

## Fase 1 — Base de ergonomía y consistencia

```text
1. Outcome.success(...)
2. Outcome.failure(...)
3. Outcome.failure(Problem...)
4. sequence(varargs)
5. traverse(...)
6. orElse(...)
7. orElseGet(...)
8. orElseThrow(...)
9. NPE consistentes
10. Automatic-Module-Name
```

Si estos elementos ya están parcialmente implementados, consolidarlos y cerrar su contrato con pruebas.

---

## Fase 2 — Entrada y control de flujo

```text
1. ThrowingSupplier
2. catching(...)
3. catching(..., mapper)
4. ensure(...)
5. otherwise(Supplier<Outcome<T>>)
```

Esta fase sí requiere cuidado semántico, pero el diseño es sólido con los cambios indicados.

---

## Fase 3 — Composición acumulativa tipada

```text
1. zip de 2 Outcomes
2. zip de 3 Outcomes
3. TriFunction anidada
```

Esta fase aporta una capacidad importante que no está cubierta por `sequence`.

---

## Fase 4 — Ergonomía del modelo de problemas

```text
1. Problem.of(...)
2. Factories específicas justificadas
3. Problems.hasCode(...)
4. Problems.byCode(...)
5. Problems.byType(...)
6. Problem.withCause(...)
7. Problem.withMetadata(...)
```

Sin `Problems.stream()`.

---

## Fase 5 — Hardening

```text
1. Fix contextual de NPE
2. ArchTest
3. README
4. Actualizar PROPOSALS.md
5. JaCoCo >= 85 %
6. PIT >= 85 %
```

---

# 5. Veredicto final

## Sí implementaría

- `ThrowingSupplier`
- `catching(...)`
- `ensure(...)`
- `otherwise(...)` lazy
- `zip(...)` de 2 y 3
- `TriFunction` anidada
- factories de `Outcome`
- `sequence(varargs)`
- `traverse(...)`
- `orElse`, `orElseGet`, `orElseThrow`
- factories de `Problem` con semántica bien definida
- `Problems.hasCode(...)`
- `Problems.byCode(...)`
- `Problems.byType(...)`
- `Problem.withCause(...)`
- `Problem.withMetadata(...)`
- fail-fast consistente
- ArchTest, README, JaCoCo y PIT

## Sí, pero con cambios

- `catching(..., mapper)` -> mapper recibe `Exception`, no `Throwable`
- `ensure(...)` -> usar `Supplier<? extends Problem>`
- `otherwise(...)` -> empezar sólo con la variante lazy
- `zip(...)` -> limitar a 2 y 3 fuentes
- factories por tipo -> no crear una automáticamente para cada enum sin revisar semántica
- `withCause(...)` -> `null` debe fallar explícitamente, no tener significado implícito

## No implementaría

- `Problems.stream()`
- overload eager de `otherwise(...)` en el primer corte
- una familia creciente de `zip4`, `zip5`, etc.
- `Pair`, `Triple` o tuplas
- `ThrowingFunction`, `ThrowingConsumer` y más interfaces hasta que exista un caso real
- conversiones `Optional`
- async, retry o resiliencia en el core

---

# Conclusión

El nuevo plan es bastante más coherente que el anterior.

Lo más fuerte del documento es que cada operación nueva tiene una razón funcional distinta:

```text
catching   -> entrar desde código que lanza excepciones
ensure     -> imponer una condición sobre un Success
otherwise  -> intentar una estrategia alternativa
zip        -> combinar tipos distintos acumulando fallos
sequence   -> acumular resultados homogéneos
traverse   -> transformar y acumular una colección
```

Ahí es donde Outcome empieza a tener identidad propia.

Mi recomendación no sería recortar el plan por miedo a crecer. Sí implementaría casi todo, porque estas operaciones ya no son azúcar aleatoria: cubren huecos concretos del modelo.

Pero mantendría una regla:

> Cada método nuevo debe aportar una semántica que las primitivas existentes no expresen claramente o eliminar una cantidad significativa de boilerplate.

Con esos ajustes, sí veo este plan como un roadmap sólido para evolucionar Outcome sin convertirlo en una copia de Vavr.
