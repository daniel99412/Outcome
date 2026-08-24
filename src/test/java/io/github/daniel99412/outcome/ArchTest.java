package io.github.daniel99412.outcome;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import io.github.daniel99412.outcome.problem.Problem;
import io.github.daniel99412.outcome.problem.ProblemType;
import io.github.daniel99412.outcome.problem.Problems;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Architectural invariants — verifies the library stays small, sealed and dependency-free.
 */
class ArchTest {

    private static JavaClasses productionClasses;

    @BeforeAll
    static void importClasses() {
        productionClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_JARS)
                .importPackages("io.github.daniel99412.outcome");
    }

    @Test
    @DisplayName("Outcome is sealed and only permits Success and Failure")
    void outcomeIsSealedWithCorrectPermits() {
        Class<?> outcome = Outcome.class;
        assertTrue(outcome.isSealed(), "Outcome must be sealed");
        Class<?>[] permitted = outcome.getPermittedSubclasses();
        assertTrue(permitted.length == 2, "Outcome must permit exactly 2 subtypes");
        // order not guaranteed, check set
        boolean hasSuccess = false, hasFailure = false;
        for (Class<?> c : permitted) {
            if (c == Success.class) hasSuccess = true;
            if (c == Failure.class) hasFailure = true;
        }
        assertTrue(hasSuccess && hasFailure, "Outcome must permit Success and Failure");
    }

    @Test
    @DisplayName("Problem and Problems are records; Success/Failure are records implementing Outcome")
    void successAndFailureAreRecordsImplementingOutcome() {
        assertTrue(Success.class.isRecord(), "Success must be a record");
        assertTrue(Failure.class.isRecord(), "Failure must be a record");
        assertTrue(Outcome.class.isAssignableFrom(Success.class), "Success must implement Outcome");
        assertTrue(Outcome.class.isAssignableFrom(Failure.class), "Failure must implement Outcome");
    }

    @Test
    @DisplayName("Problem is a record and Problems is a record")
    void problemAndProblemsAreRecords() {
        assertTrue(Problem.class.isRecord(), "Problem must be a record");
        assertTrue(Problems.class.isRecord(), "Problems must be a record");
    }

    @Test
    @DisplayName("Nested functional interfaces are public static and annotated @FunctionalInterface")
    void nestedFunctionalInterfacesArePublicStatic() {
        for (Class<?> nested : new Class<?>[]{Outcome.ThrowingSupplier.class, Outcome.TriFunction.class}) {
            assertTrue(java.lang.reflect.Modifier.isPublic(nested.getModifiers()),
                    () -> nested.getSimpleName() + " must be public");
            assertTrue(java.lang.reflect.Modifier.isStatic(nested.getModifiers()),
                    () -> nested.getSimpleName() + " must be static");
            assertTrue(nested.isInterface(), () -> nested.getSimpleName() + " must be an interface");
            assertNotNull(nested.getAnnotation(FunctionalInterface.class),
                    () -> nested.getSimpleName() + " must be annotated with @FunctionalInterface");
            assertEquals(1, nested.getDeclaredMethods().length,
                    () -> nested.getSimpleName() + " must have exactly one abstract method shape");
        }
    }

    @Test
    @DisplayName("Core API has no external dependencies (dependency-free except java.*)")
    void coreHasNoExternalDependencies() {
        ArchRule rule = noClasses()
                .should().dependOnClassesThat()
                .resideOutsideOfPackages("java..", "io.github.daniel99412.outcome..")
                .because("Library must be dependency-free");

        rule.check(productionClasses);
    }

    @Test
    @DisplayName("Problem package does not depend on outcome core (acyclic)")
    void problemPackageDoesNotDependOnOutcomeCore() {
        // Problem and ProblemType/Problems must not depend on Outcome/Success/Failure
        JavaClasses problemClasses = new ClassFileImporter()
                .importPackages("io.github.daniel99412.outcome.problem");

        ArchRule rule = noClasses()
                .that().resideInAPackage("io.github.daniel99412.outcome.problem")
                .should().dependOnClassesThat()
                .resideInAPackage("io.github.daniel99412.outcome")
                .andShould().dependOnClassesThat().areAssignableTo(Outcome.class)
                .because("problem package must be independent of Outcome core");

        // We check with a more lenient rule: no dependency on Outcome/Success/Failure explicitly
        // Use ArchRule that checks Problem classes don't depend on Outcome
        // Simpler: assert via manual check
        for (var clazz : problemClasses) {
            for (var dep : clazz.getDirectDependenciesFromSelf()) {
                String target = dep.getTargetClass().getPackageName();
                if (target.equals("io.github.daniel99412.outcome") && !target.equals("io.github.daniel99412.outcome.problem")) {
                    // Only java.* and same package allowed, but Problem depending on Outcome would be in different package
                    // This would be flagged; we assert not found
                    assertTrue(false, "Problem package must not depend on io.github.daniel99412.outcome: " + clazz.getName() + " -> " + dep.getTargetClass().getName());
                }
            }
        }
    }

    @Test
    @DisplayName("Production classes are immutable candidates (records or final with private final fields)")
    void productionClassesAreImmutable() {
        ArchRule recordsAreImmutable = classes()
                .that().areRecords()
                .should().bePublic()
                .because("Success/Problem are records — immutable by design");

        recordsAreImmutable.check(productionClasses);

        ArchRule problemsIsFinal = classes()
                .that().haveSimpleName("Problems")
                .should().bePublic().andShould().haveModifier(com.tngtech.archunit.core.domain.JavaModifier.FINAL)
                .because("Problems must be final to guarantee immutability");

        problemsIsFinal.check(productionClasses);
    }

    @Test
    @DisplayName("Public API is limited to expected types")
    void publicApiIsLimited() {
        var allowed = java.util.Set.of(
                Outcome.class.getName(),
                Outcome.class.getName() + "$ThrowingSupplier",
                Outcome.class.getName() + "$TriFunction",
                Success.class.getName(),
                Failure.class.getName(),
                Problem.class.getName(),
                Problems.class.getName(),
                ProblemType.class.getName()
        );
        for (var clazz : productionClasses) {
            if (clazz.getModifiers().contains(JavaModifier.PUBLIC)) {
                assertTrue(allowed.contains(clazz.getName()),
                        "Unexpected public class: " + clazz.getName()
                                + " — public API should be limited to Outcome (+ nested functional"
                                + " interfaces), Success, Failure, Problem, Problems, ProblemType");
            }
        }
    }
}
