package io.monosense.synergyflow.user.application;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/actuator")
class ActuatorStubController {

    @GetMapping("/health")
    ResponseEntity<Void> health() {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/info")
    ResponseEntity<Void> info() {
        return ResponseEntity.ok().build();
    }
}
