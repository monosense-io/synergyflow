package io.monosense.synergyflow;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ArchitectureTests {

    @Test
    void verifyModularity() {
        ApplicationModules.of(SynergyFlowApplication.class).verify();
    }
}

