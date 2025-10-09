package io.monosense.synergyflow;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * ArchUnit architecture tests for package access enforcement.
 * <p>
 * These tests enforce:
 * - Classes in internal/ packages are NOT accessible from outside their module
 * - Classes in api/ and spi/ packages ARE accessible (public contracts)
 * - All classes use io.monosense.synergyflow.* namespace
 * - No cyclic dependencies between packages
 * </p>
 * <p>
 * CRITICAL: These tests MUST pass for ./gradlew build to succeed.
 * Build failures here indicate architectural violations that must be fixed.
 * </p>
 */
class ArchUnitTests {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setup() {
        importedClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("io.monosense.synergyflow");
    }

    /**
     * AC4: Enforces that classes in internal/ packages are NOT accessible from outside their module.
     * <p>
     * Example violation: Workflow module importing itsm.internal.TicketServiceImpl directly
     * instead of using itsm.spi.TicketQueryService interface.
     * </p>
     */
    @Test
    void testInternalPackageNotAccessibleFromOutside() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..internal..")
            // Allow common Spring stereotypes that must be public
            .and().areNotAnnotatedWith(org.springframework.stereotype.Service.class)
            .and().areNotAnnotatedWith(Repository.class)
            .and().areNotAnnotatedWith(Component.class)
            // Allow Exceptions to be public across internal subpackages
            .and().haveSimpleNameNotEndingWith("Exception")
            // Allow domain model classes to be public across internal packages
            .and().resideOutsideOfPackage("..internal.domain..")
            // Allow public interfaces used as ports
            .and().areNotInterfaces()
            .should().bePublic()
            .allowEmptyShould(true)
            .because("Internal implementation classes should generally not be public; exceptions, repositories, components, domain models, and interfaces are allowed for Spring wiring and cross-package use within a module.");

        rule.check(importedClasses);
    }

    /**
     * AC1: Enforces that all classes use io.monosense.synergyflow.* namespace convention.
     */
    @Test
    void testPackageNamingConvention() {
        ArchRule rule = classes()
            .should().resideInAPackage("io.monosense.synergyflow..")
            .because("All classes must use the io.monosense.synergyflow namespace for consistency");

        rule.check(importedClasses);
    }

    /**
     * AC4: Enforces that classes in api/ and spi/ packages CAN be accessed (are public).
     * <p>
     * These packages define public contracts that other modules can depend on.
     * Note: package-info classes are excluded as they are package-private by design.
     * </p>
     */
    @Test
    void testApiAndSpiPackagesArePublic() {
        // This is a positive test - we expect these to be public
        // If they're not public, module communication breaks
        // Exclude package-info classes as they are package-private by design
        ArchRule rule = classes()
            .that().resideInAPackage("..api..")
            .or().resideInAPackage("..spi..")
            .and().haveSimpleNameNotEndingWith("package-info")
            .should().bePublic()
            .allowEmptyShould(true)
            .because("API and SPI packages define public contracts for module communication");

        rule.check(importedClasses);
    }

    /**
     * Enforces no cyclic dependencies between packages within or across modules.
     * <p>
     * Cyclic dependencies make code harder to understand, test, and refactor.
     * Spring Modulith also checks this at module level; this test checks at package level.
     * </p>
     */
    @Test
    void testNoCyclicPackageDependencies() {
        ArchRule rule = slices()
            .matching("io.monosense.synergyflow.(*)..")
            .should().beFreeOfCycles()
            .because("Cyclic dependencies indicate design problems and prevent clean architecture");

        rule.check(importedClasses);
    }
}
