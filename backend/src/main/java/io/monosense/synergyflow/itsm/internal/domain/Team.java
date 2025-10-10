package io.monosense.synergyflow.itsm.internal.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

import static org.hibernate.annotations.UuidGenerator.Style;

/**
 * Organizational team entity used for auto-assignment routing.
 *
 * <p>Teams represent resolver groups (e.g., Network, Software, Hardware) that
 * tickets can be routed to. Story 2.4 introduces team-based routing with
 * round-robin selection among team members.</p>
 */
@Entity
@Table(name = "teams")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Team {

    @Id
    @UuidGenerator(style = Style.VERSION_7)
    private UUID id;

    @NotBlank
    @Column(name = "name", nullable = false, unique = true, length = 200)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    Team(String name, String description, boolean enabled) {
        this.name = name;
        this.description = description;
        this.enabled = enabled;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
