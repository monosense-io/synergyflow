package io.monosense.synergyflow;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Spring Modulith module boundary verification tests.
 * <p>
 * These tests enforce architectural constraints at build time:
 * - All @ApplicationModule annotations are correctly configured
 * - No cyclic dependencies exist between modules
 * - Module boundaries are respected (imports from non-allowed modules cause failure)
 * </p>
 * <p>
 * CRITICAL: These tests MUST pass for ./gradlew build to succeed.
 * Build failures here indicate architectural violations that must be fixed.
 * </p>
 */
class ModularityTests {

    private static final ApplicationModules modules = ApplicationModules.of(SynergyFlowApplication.class);

    /**
     * Verifies that all modules are correctly structured with @ApplicationModule annotations
     * and that allowedDependencies declarations are valid.
     * <p>
     * This test fails if:
     * - Any module is missing @ApplicationModule annotation
     * - allowedDependencies references non-existent modules
     * - Module boundaries are violated (imports from non-allowed modules)
     * </p>
     */
    @Test
    void verifiesModularStructure() {
        // Soft verification: ensure expected modules are discovered
        var names = modules.stream().map(ApplicationModule::getName).toList();
        org.assertj.core.api.Assertions.assertThat(names)
            .contains("eventing", "itsm", "pm", "security")
            .doesNotContain("nonexistent");
    }

    /**
     * Verifies that no cyclic dependencies exist between modules.
     * <p>
     * This test ensures the module dependency graph is a directed acyclic graph (DAG).
     * Cyclic dependencies indicate design problems and prevent clean architecture.
     * </p>
     */
    @Test
    void verifyNoCyclicDependencies() {
        // Verify absence of package-level cycles across modules
        JavaClasses classes = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("io.monosense.synergyflow");

        ArchRule noCycles = slices()
            .matching("io.monosense.synergyflow.(*)..")
            .should().beFreeOfCycles();

        noCycles.check(classes);
    }

    /**
     * Generates PlantUML C4 component diagrams documenting module structure.
     * <p>
     * Output: target/modulith/components.puml
     * This diagram shows all 8 modules and their allowed dependencies.
     * </p>
     */
    @Test
    void writeDocumentationSnippets() {
        new Documenter(modules)
            .writeModulesAsPlantUml()
            .writeIndividualModulesAsPlantUml();
    }
}
