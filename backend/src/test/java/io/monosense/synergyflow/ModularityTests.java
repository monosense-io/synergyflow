package io.monosense.synergyflow;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

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
        modules.verify();
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
        // verify() automatically checks for cycles
        modules.verify();
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
