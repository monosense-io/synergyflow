# 3.3 Mandatory Backend Code Templates & Conventions

This section defines **mandatory** patterns for all backend code. These templates ensure consistency, type safety, and proper module boundaries across the application.

## 3.3.1 Gradle Dependencies Configuration (MANDATORY)

**File:** `backend/build.gradle.kts`

```kotlin
plugins {
    java
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.4"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

dependencies {
    // Spring Boot & Modulith
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.modulith:spring-modulith-starter-core")
    implementation("org.springframework.modulith:spring-modulith-starter-jpa")

    // Database
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")

    // MANDATORY: Lombok (MUST BE BEFORE MapStruct)
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    // MANDATORY: MapStruct (MUST BE AFTER Lombok)
    implementation("org.mapstruct:mapstruct:1.5.5.Final")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.5.5.Final")

    // MANDATORY: Lombok + MapStruct Integration
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")

    // MANDATORY: SpringDoc OpenAPI (Swagger UI)
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")

    // Flowable
    implementation("org.flowable:flowable-spring-boot-starter:7.1.0")

    // Resilience4j
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.1.0")

    // Test dependencies
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.modulith:spring-modulith-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testCompileOnly("org.projectlombok:lombok:1.18.30")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.30")
    testAnnotationProcessor("org.mapstruct:mapstruct-processor:1.5.5.Final")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

## 3.2.2 Lombok Configuration (MANDATORY)

**File:** `backend/lombok.config`

```properties
# Generate @Generated annotation for Lombok-generated code
lombok.addLombokGeneratedAnnotation = true

# Copy Spring annotations to generated code
lombok.copyableAnnotations += org.springframework.beans.factory.annotation.Qualifier
lombok.copyableAnnotations += org.springframework.beans.factory.annotation.Value
lombok.copyableAnnotations += org.springframework.context.annotation.Lazy

# Stop lombok from looking for config in parent directories
config.stopBubbling = true
```

## 3.2.3 DTO Template (MANDATORY - Lombok @Value)

All DTOs **must** be immutable using Lombok `@Value`. Public API at module root.

**Example:** `com/synergyflow/incident/IncidentDto.java`

```java
package io.monosense.synergyflow.incident;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

/**
 * Incident data transfer object (immutable).
 * Public API exposed to other modules and frontend.
 */
@Value
@Schema(description = "Incident details")
public class IncidentDto {

    @Schema(description = "Unique incident identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    UUID id;

    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 200, message = "Title must be between 5 and 200 characters")
    @Schema(description = "Incident title", example = "Database connection timeout")
    String title;

    @NotBlank(message = "Description is required")
    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    @Schema(description = "Detailed incident description")
    String description;

    @NotNull(message = "Priority is required")
    @Schema(description = "Business priority", example = "HIGH")
    Priority priority;

    @NotNull(message = "Severity is required")
    @Schema(description = "Technical severity", example = "S2")
    Severity severity;

    @Schema(description = "Current incident status", example = "IN_PROGRESS")
    Status status;

    @Schema(description = "Assigned agent ID", nullable = true)
    UUID assignedTo;

    @Schema(description = "Creator user ID")
    UUID createdBy;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    @Schema(description = "Creation timestamp", example = "2025-10-18T10:30:00.000Z")
    Instant createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    @Schema(description = "Last update timestamp")
    Instant updatedAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    @Schema(description = "Resolution timestamp", nullable = true)
    Instant resolvedAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    @Schema(description = "SLA breach deadline")
    Instant slaDeadline;

    @Schema(description = "Resolution notes", nullable = true)
    String resolution;

    @Schema(description = "Optimistic locking version")
    Integer version;

    public enum Priority { LOW, MEDIUM, HIGH, CRITICAL }
    public enum Severity { S1, S2, S3, S4 }
    public enum Status { NEW, ASSIGNED, IN_PROGRESS, RESOLVED, CLOSED }
}
```

## 3.2.4 Entity Template (MANDATORY - Lombok @Entity @Data)

All entities **must** be package-private in `.internal` package with Lombok `@Data`.

**Example:** `com/synergyflow/incident/internal/Incident.java`

```java
package io.monosense.synergyflow.incident.internal;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Incident entity (package-private, internal to incident module).
 * NOT accessible from other modules.
 */
@Entity
@Table(name = "incidents", schema = "synergyflow_incidents")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    private Instant resolvedAt;

    @Column(nullable = false)
    private Instant slaDeadline;

    @Column(columnDefinition = "TEXT")
    private String resolution;

    @Version
    private Integer version;

    enum Priority { LOW, MEDIUM, HIGH, CRITICAL }
    enum Severity { S1, S2, S3, S4 }
    enum Status { NEW, ASSIGNED, IN_PROGRESS, RESOLVED, CLOSED }
}
```

## 3.2.5 MapStruct Mapper Template (MANDATORY)

All mappers **must** be package-private in `.internal` package with Spring component model.

**Example:** `com/synergyflow/incident/internal/IncidentMapper.java`

```java
package io.monosense.synergyflow.incident.internal;

import io.monosense.synergyflow.incident.IncidentDto;
import org.mapstruct.*;

import java.time.Instant;
import java.util.List;

/**
 * MapStruct mapper for Incident entity ↔ DTO conversion.
 * Package-private, internal to incident module.
 */
@Mapper(
    componentModel = "spring",
    injectionStrategy = InjectionStrategy.CONSTRUCTOR,
    unmappedTargetPolicy = ReportingPolicy.ERROR,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
interface IncidentMapper {

    // Entity → DTO (expose to other modules)
    IncidentDto toDto(Incident entity);

    List<IncidentDto> toDtos(List<Incident> entities);

    // DTO → Entity (create operations)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "slaDeadline", expression = "java(calculateSlaDeadline(dto.getPriority()))")
    Incident toEntity(IncidentDto dto);

    // Update existing entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "slaDeadline", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(IncidentDto dto, @MappingTarget Incident entity);

    // Custom SLA deadline calculation
    default Instant calculateSlaDeadline(IncidentDto.Priority priority) {
        return switch (priority) {
            case CRITICAL -> Instant.now().plusSeconds(4 * 3600);   // 4 hours
            case HIGH -> Instant.now().plusSeconds(8 * 3600);       // 8 hours
            case MEDIUM -> Instant.now().plusSeconds(24 * 3600);    // 24 hours
            case LOW -> Instant.now().plusSeconds(72 * 3600);       // 72 hours
        };
    }
}
```

## 3.2.6 REST Controller Template (MANDATORY - SpringDoc + RFC7807)

All controllers **must** use SpringDoc annotations and return RFC7807 Problem Details for errors.

**Example:** `com/synergyflow/incident/IncidentController.java`

```java
package io.monosense.synergyflow.incident;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

/**
 * REST API for Incident Management.
 * OpenAPI documentation via SpringDoc, RFC7807 error responses.
 */
@RestController
@RequestMapping("/api/v1/incidents")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Incidents", description = "Incident Management API")
public class IncidentController {

    private final IncidentService incidentService;

    @Operation(
        summary = "Create new incident",
        description = "Creates a new incident and triggers SLA timer workflow"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Incident created successfully",
            content = @Content(schema = @Schema(implementation = IncidentDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request body",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IncidentDto createIncident(
        @Valid @RequestBody IncidentDto dto,
        @AuthenticationPrincipal Jwt jwt
    ) {
        log.info("Creating incident: {}", dto.getTitle());
        return incidentService.createIncident(dto);
    }

    @Operation(
        summary = "Get incident by ID",
        description = "Retrieves incident details by unique identifier"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Incident found",
            content = @Content(schema = @Schema(implementation = IncidentDto.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Incident not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    @GetMapping("/{id}")
    public IncidentDto getIncident(
        @Parameter(description = "Incident UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        @PathVariable UUID id
    ) {
        log.debug("Fetching incident: {}", id);
        return incidentService.getIncident(id);
    }

    @Operation(
        summary = "List incidents",
        description = "Retrieves paginated list of incidents with optional filters"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Incidents retrieved successfully"
        )
    })
    @GetMapping
    public Page<IncidentDto> listIncidents(
        @Parameter(description = "Filter by priority")
        @RequestParam(required = false) IncidentDto.Priority priority,

        @Parameter(description = "Filter by status")
        @RequestParam(required = false) IncidentDto.Status status,

        @Parameter(description = "Filter by assigned agent ID")
        @RequestParam(required = false) UUID assignedTo,

        @PageableDefault(size = 50, sort = "createdAt,desc") Pageable pageable
    ) {
        log.debug("Listing incidents - priority: {}, status: {}", priority, status);
        return incidentService.listIncidents(priority, status, assignedTo, pageable);
    }

    @Operation(
        summary = "Assign incident to agent",
        description = "Assigns incident to specified agent and triggers routing policy evaluation"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Incident assigned successfully"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Incident not found"
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Optimistic locking conflict",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class))
        )
    })
    @PostMapping("/{id}/assign")
    public IncidentDto assignIncident(
        @PathVariable UUID id,
        @RequestParam UUID assignedTo,
        @AuthenticationPrincipal Jwt jwt
    ) {
        log.info("Assigning incident {} to agent {}", id, assignedTo);
        return incidentService.assignIncident(id, assignedTo);
    }

    @Operation(
        summary = "Resolve incident",
        description = "Marks incident as resolved and stops SLA timer"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Incident resolved"),
        @ApiResponse(responseCode = "404", description = "Incident not found"),
        @ApiResponse(responseCode = "409", description = "Optimistic locking conflict")
    })
    @PostMapping("/{id}/resolve")
    public IncidentDto resolveIncident(
        @PathVariable UUID id,
        @RequestParam String resolution,
        @AuthenticationPrincipal Jwt jwt
    ) {
        log.info("Resolving incident {}", id);
        return incidentService.resolveIncident(id, resolution);
    }
}
```

## 3.2.7 RFC7807 Problem Details Global Exception Handler (MANDATORY)

**File:** `com/synergyflow/commons/exception/GlobalExceptionHandler.java`

```java
package io.monosense.synergyflow.commons.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Global exception handler using RFC7807 Problem Details.
 * All errors return standardized ProblemDetail responses.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(
        ResourceNotFoundException ex,
        WebRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            ex.getMessage()
        );
        problem.setType(URI.create("https://synergyflow.example.com/errors/not-found"));
        problem.setTitle("Resource Not Found");
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("correlationId", getCorrelationId(request));
        problem.setProperty("resourceType", ex.getResourceType());
        problem.setProperty("resourceId", ex.getResourceId());

        log.warn("Resource not found: {} with ID {}", ex.getResourceType(), ex.getResourceId());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(
        MethodArgumentNotValidException ex,
        WebRequest request
    ) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Validation failed for request body"
        );
        problem.setType(URI.create("https://synergyflow.example.com/errors/validation-error"));
        problem.setTitle("Validation Error");
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("correlationId", getCorrelationId(request));
        problem.setProperty("validationErrors", errors);

        log.warn("Validation errors: {}", errors);
        return problem;
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(
        OptimisticLockingFailureException ex,
        WebRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            "The resource was modified by another transaction. Please retry with latest data."
        );
        problem.setType(URI.create("https://synergyflow.example.com/errors/optimistic-lock"));
        problem.setTitle("Concurrent Modification");
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("correlationId", getCorrelationId(request));

        log.warn("Optimistic locking conflict", ex);
        return problem;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(
        AccessDeniedException ex,
        WebRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.FORBIDDEN,
            "You do not have permission to access this resource"
        );
        problem.setType(URI.create("https://synergyflow.example.com/errors/forbidden"));
        problem.setTitle("Access Denied");
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("correlationId", getCorrelationId(request));

        log.warn("Access denied: {}", ex.getMessage());
        return problem;
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthenticationError(
        AuthenticationException ex,
        WebRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.UNAUTHORIZED,
            "Authentication failed"
        );
        problem.setType(URI.create("https://synergyflow.example.com/errors/unauthorized"));
        problem.setTitle("Unauthorized");
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("correlationId", getCorrelationId(request));

        log.warn("Authentication error: {}", ex.getMessage());
        return problem;
    }

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusinessException(
        BusinessException ex,
        WebRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            ex.getStatus(),
            ex.getMessage()
        );
        problem.setType(URI.create("https://synergyflow.example.com/errors/business-rule"));
        problem.setTitle("Business Rule Violation");
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("correlationId", getCorrelationId(request));
        problem.setProperty("errorCode", ex.getErrorCode());

        log.warn("Business exception: {}", ex.getMessage());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(
        Exception ex,
        WebRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred. Please contact support with the correlation ID."
        );
        problem.setType(URI.create("https://synergyflow.example.com/errors/internal-error"));
        problem.setTitle("Internal Server Error");
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("correlationId", getCorrelationId(request));

        log.error("Unexpected error", ex);
        return problem;
    }

    private String getCorrelationId(WebRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        return correlationId != null ? correlationId : UUID.randomUUID().toString();
    }
}
```

## 3.2.8 Custom Exception Classes

**ResourceNotFoundException:**

```java
package io.monosense.synergyflow.commons.exception;

import lombok.Getter;
import java.util.UUID;

@Getter
public class ResourceNotFoundException extends RuntimeException {
    private final String resourceType;
    private final UUID resourceId;

    public ResourceNotFoundException(String resourceType, UUID resourceId) {
        super(String.format("%s with ID %s not found", resourceType, resourceId));
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }
}
```

**BusinessException:**

```java
package io.monosense.synergyflow.commons.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;

    public BusinessException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public BusinessException(String message, String errorCode) {
        this(message, HttpStatus.BAD_REQUEST, errorCode);
    }
}
```

## 3.2.9 SpringDoc OpenAPI Configuration (MANDATORY)

**File:** `com/synergyflow/commons/config/OpenApiConfig.java`

```java
package io.monosense.synergyflow.commons.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * SpringDoc OpenAPI configuration.
 * Swagger UI available at: /swagger-ui/index.html
 * OpenAPI JSON at: /v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI synergyFlowOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("SynergyFlow API")
                .version("1.0.0")
                .description("Unified ITSM+PM platform with intelligent workflow automation")
                .contact(new Contact()
                    .name("SynergyFlow Team")
                    .email("support@synergyflow.example.com"))
                .license(new License()
                    .name("Proprietary")
                    .url("https://synergyflow.example.com/license")))
            .servers(List.of(
                new Server()
                    .url("https://synergyflow.example.com/api/v1")
                    .description("Production server"),
                new Server()
                    .url("https://staging.synergyflow.example.com/api/v1")
                    .description("Staging server"),
                new Server()
                    .url("http://localhost:8080/api/v1")
                    .description("Local development server")
            ))
            .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"))
            .components(new io.swagger.v3.oas.models.Components()
                .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT obtained from OAuth2 provider (Keycloak/Auth0)")));
    }
}
```

## 3.2.10 Service Layer Template (MANDATORY)

All service implementations **must** be package-private in `.internal` package, implementing public interface at module root.

**Public Service Interface:** `com/synergyflow/incident/IncidentService.java`

```java
package io.monosense.synergyflow.incident;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Public service interface for Incident module.
 * Exposed to other modules and controllers.
 */
public interface IncidentService {

    /**
     * Creates a new incident and triggers SLA timer workflow.
     *
     * @param dto incident data (without ID)
     * @return created incident with generated ID
     */
    IncidentDto createIncident(IncidentDto dto);

    /**
     * Retrieves incident by ID.
     *
     * @param id incident UUID
     * @return incident details
     * @throws ResourceNotFoundException if incident not found
     */
    IncidentDto getIncident(UUID id);

    /**
     * Lists incidents with optional filters and pagination.
     *
     * @param priority filter by priority (optional)
     * @param status filter by status (optional)
     * @param assignedTo filter by assigned agent (optional)
     * @param pageable pagination parameters
     * @return paginated list of incidents
     */
    Page<IncidentDto> listIncidents(
        IncidentDto.Priority priority,
        IncidentDto.Status status,
        UUID assignedTo,
        Pageable pageable
    );

    /**
     * Assigns incident to agent.
     *
     * @param id incident UUID
     * @param assignedTo agent UUID
     * @return updated incident
     * @throws ResourceNotFoundException if incident not found
     * @throws OptimisticLockingFailureException if concurrent modification
     */
    IncidentDto assignIncident(UUID id, UUID assignedTo);

    /**
     * Resolves incident and stops SLA timer.
     *
     * @param id incident UUID
     * @param resolution resolution notes
     * @return resolved incident
     * @throws ResourceNotFoundException if incident not found
     */
    IncidentDto resolveIncident(UUID id, String resolution);
}
```

**Service Implementation:** `com/synergyflow/incident/internal/IncidentServiceImpl.java`

```java
package io.monosense.synergyflow.incident.internal;

import io.monosense.synergyflow.commons.exception.ResourceNotFoundException;
import io.monosense.synergyflow.incident.IncidentCreatedEvent;
import io.monosense.synergyflow.incident.IncidentDto;
import io.monosense.synergyflow.incident.IncidentResolvedEvent;
import io.monosense.synergyflow.incident.IncidentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Incident service implementation (package-private, internal to incident module).
 * Uses Spring Modulith events for cross-module communication.
 */
@Service
@RequiredArgsConstructor
@Slf4j
class IncidentServiceImpl implements IncidentService {

    private final IncidentRepository repository;
    private final IncidentMapper mapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public IncidentDto createIncident(IncidentDto dto) {
        log.info("Creating incident: {}", dto.getTitle());

        // Map DTO to entity
        Incident incident = mapper.toEntity(dto);
        incident.setStatus(Incident.Status.NEW);

        // Save entity (triggers JPA auditing)
        incident = repository.save(incident);

        // Publish domain event (Spring Modulith transactional outbox)
        UUID correlationId = UUID.randomUUID();
        eventPublisher.publishEvent(new IncidentCreatedEvent(
            incident.getId(),
            incident.getTitle(),
            mapPriority(incident.getPriority()),
            mapSeverity(incident.getSeverity()),
            incident.getCreatedBy(),
            incident.getCreatedAt(),
            correlationId
        ));

        log.info("Incident created: {} (correlationId: {})", incident.getId(), correlationId);
        return mapper.toDto(incident);
    }

    @Override
    @Transactional(readOnly = true)
    public IncidentDto getIncident(UUID id) {
        log.debug("Fetching incident: {}", id);
        return repository.findById(id)
            .map(mapper::toDto)
            .orElseThrow(() -> new ResourceNotFoundException("Incident", id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<IncidentDto> listIncidents(
        IncidentDto.Priority priority,
        IncidentDto.Status status,
        UUID assignedTo,
        Pageable pageable
    ) {
        log.debug("Listing incidents - priority: {}, status: {}, assignedTo: {}", priority, status, assignedTo);

        Specification<Incident> spec = Specification.where(null);

        if (priority != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("priority"), Incident.Priority.valueOf(priority.name())));
        }

        if (status != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("status"), Incident.Status.valueOf(status.name())));
        }

        if (assignedTo != null) {
            spec = spec.and((root, query, cb) ->
                cb.equal(root.get("assignedTo"), assignedTo));
        }

        return repository.findAll(spec, pageable)
            .map(mapper::toDto);
    }

    @Override
    @Transactional
    public IncidentDto assignIncident(UUID id, UUID assignedTo) {
        log.info("Assigning incident {} to agent {}", id, assignedTo);

        Incident incident = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Incident", id));

        incident.setAssignedTo(assignedTo);
        incident.setStatus(Incident.Status.ASSIGNED);

        incident = repository.save(incident);

        // Could publish IncidentAssignedEvent here for notification module
        log.info("Incident {} assigned to {}", id, assignedTo);
        return mapper.toDto(incident);
    }

    @Override
    @Transactional
    public IncidentDto resolveIncident(UUID id, String resolution) {
        log.info("Resolving incident {}", id);

        Incident incident = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Incident", id));

        incident.setStatus(Incident.Status.RESOLVED);
        incident.setResolution(resolution);
        incident.setResolvedAt(Instant.now());

        incident = repository.save(incident);

        // Publish resolved event (triggers SLA timer cancellation)
        UUID correlationId = UUID.randomUUID();
        eventPublisher.publishEvent(new IncidentResolvedEvent(
            incident.getId(),
            incident.getResolution(),
            incident.getResolvedAt(),
            correlationId
        ));

        log.info("Incident {} resolved (correlationId: {})", id, correlationId);
        return mapper.toDto(incident);
    }

    // Helper methods for enum mapping
    private IncidentCreatedEvent.Priority mapPriority(Incident.Priority priority) {
        return IncidentCreatedEvent.Priority.valueOf(priority.name());
    }

    private IncidentCreatedEvent.Severity mapSeverity(Incident.Severity severity) {
        return IncidentCreatedEvent.Severity.valueOf(severity.name());
    }
}
```

## 3.2.11 Repository Layer Template (MANDATORY)

All repositories **must** be package-private in `.internal` package.

**Example:** `com/synergyflow/incident/internal/IncidentRepository.java`

```java
package io.monosense.synergyflow.incident.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * JPA Repository for Incident entity (package-private, internal to incident module).
 * Extends JpaSpecificationExecutor for dynamic query support.
 */
interface IncidentRepository extends JpaRepository<Incident, UUID>, JpaSpecificationExecutor<Incident> {

    /**
     * Find incidents by status (query method by naming convention).
     */
    List<Incident> findByStatus(Incident.Status status);

    /**
     * Find incidents assigned to specific agent.
     */
    List<Incident> findByAssignedTo(UUID assignedTo);

    /**
     * Find incidents approaching SLA deadline.
     *
     * @param threshold time threshold (e.g., now + 1 hour)
     * @return incidents with SLA deadline before threshold
     */
    @Query("SELECT i FROM Incident i WHERE i.slaDeadline < :threshold AND i.status NOT IN ('RESOLVED', 'CLOSED')")
    List<Incident> findApproachingSlaDeadline(@Param("threshold") Instant threshold);

    /**
     * Find incidents breaching SLA.
     */
    @Query("SELECT i FROM Incident i WHERE i.slaDeadline < CURRENT_TIMESTAMP AND i.status NOT IN ('RESOLVED', 'CLOSED')")
    List<Incident> findSlaBreach();

    /**
     * Count incidents by priority.
     */
    long countByPriority(Incident.Priority priority);
}
```

## 3.2.12 Domain Event Template (MANDATORY - Java Records)

All domain events **must** be Java records at module root, using Spring Modulith's transactional outbox.

**Example:** `com/synergyflow/incident/IncidentCreatedEvent.java`

```java
package io.monosense.synergyflow.incident;

import lombok.NonNull;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when a new incident is created.
 * Other modules (change, task, workflow) can listen to this event.
 *
 * Published atomically with incident creation via Spring Modulith transactional outbox.
 */
public record IncidentCreatedEvent(
    @NonNull UUID incidentId,
    @NonNull String title,
    @NonNull Priority priority,
    @NonNull Severity severity,
    @NonNull UUID createdBy,
    @NonNull Instant createdAt,
    @NonNull UUID correlationId  // For distributed tracing
) {
    public enum Priority { LOW, MEDIUM, HIGH, CRITICAL }
    public enum Severity { S1, S2, S3, S4 }
}
```

**Example:** `com/synergyflow/incident/IncidentResolvedEvent.java`

```java
package io.monosense.synergyflow.incident;

import lombok.NonNull;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when an incident is resolved.
 * Triggers SLA timer cancellation via workflow module.
 */
public record IncidentResolvedEvent(
    @NonNull UUID incidentId,
    @NonNull String resolution,
    @NonNull Instant resolvedAt,
    @NonNull UUID correlationId
) {}
```

## 3.2.13 Event Listener Template (MANDATORY - Spring Modulith)

All event listeners **must** be package-private in `.internal` package with idempotency support.

**Example:** `com/synergyflow/workflow/internal/SLATimerListener.java`

```java
package io.monosense.synergyflow.workflow.internal;

import io.monosense.synergyflow.incident.IncidentCreatedEvent;
import io.monosense.synergyflow.incident.IncidentResolvedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Event listener for SLA timer management (package-private, internal to workflow module).
 * Uses Spring Modulith @ApplicationModuleListener for asynchronous event processing.
 *
 * Spring Modulith ensures:
 * - At-least-once delivery (events stored in event_publication table)
 * - Transactional outbox pattern (event published atomically with incident creation)
 * - Automatic retry on failure
 */
@Component
@RequiredArgsConstructor
@Slf4j
class SLATimerListener {

    private final RuntimeService flowableRuntimeService;
    private final ProcessedEventRepository processedEventRepository;

    /**
     * Starts SLA timer when incident is created.
     * Idempotent via processed_events table.
     */
    @ApplicationModuleListener
    @Transactional
    void onIncidentCreated(IncidentCreatedEvent event) {
        log.info("Received IncidentCreatedEvent: {} (correlationId: {})", event.incidentId(), event.correlationId());

        // Idempotency check
        if (processedEventRepository.existsByEventIdAndListenerName(
            event.incidentId().toString(), "SLATimerListener.onIncidentCreated")) {
            log.warn("Event already processed: {}", event.incidentId());
            return;
        }

        // Start BPMN SLA timer process
        Map<String, Object> variables = new HashMap<>();
        variables.put("incidentId", event.incidentId().toString());
        variables.put("priority", event.priority().name());
        variables.put("slaHours", calculateSlaHours(event.priority()));
        variables.put("correlationId", event.correlationId().toString());

        String processInstanceId = flowableRuntimeService.startProcessInstanceByKey(
            "incident-sla-timer",
            event.incidentId().toString(),
            variables
        );

        log.info("Started SLA timer process: {} for incident: {}", processInstanceId, event.incidentId());

        // Mark event as processed
        processedEventRepository.save(new ProcessedEvent(
            event.incidentId().toString(),
            "SLATimerListener.onIncidentCreated",
            event.correlationId()
        ));
    }

    /**
     * Cancels SLA timer when incident is resolved.
     */
    @ApplicationModuleListener
    @Transactional
    void onIncidentResolved(IncidentResolvedEvent event) {
        log.info("Received IncidentResolvedEvent: {} (correlationId: {})", event.incidentId(), event.correlationId());

        // Idempotency check
        if (processedEventRepository.existsByEventIdAndListenerName(
            event.incidentId().toString(), "SLATimerListener.onIncidentResolved")) {
            log.warn("Event already processed: {}", event.incidentId());
            return;
        }

        // Delete running process instance (cancels timer)
        flowableRuntimeService.createProcessInstanceQuery()
            .processInstanceBusinessKey(event.incidentId().toString())
            .list()
            .forEach(processInstance -> {
                flowableRuntimeService.deleteProcessInstance(
                    processInstance.getId(),
                    "Incident resolved"
                );
                log.info("Cancelled SLA timer process: {} for incident: {}",
                    processInstance.getId(), event.incidentId());
            });

        // Mark event as processed
        processedEventRepository.save(new ProcessedEvent(
            event.incidentId().toString(),
            "SLATimerListener.onIncidentResolved",
            event.correlationId()
        ));
    }

    private int calculateSlaHours(IncidentCreatedEvent.Priority priority) {
        return switch (priority) {
            case CRITICAL -> 4;
            case HIGH -> 8;
            case MEDIUM -> 24;
            case LOW -> 72;
        };
    }
}
```

**Idempotency Support:** `com/synergyflow/workflow/internal/ProcessedEvent.java`

```java
package io.monosense.synergyflow.workflow.internal;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Tracks processed events for idempotent event listeners.
 */
@Entity
@Table(
    name = "processed_events",
    schema = "synergyflow_workflows",
    uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "listener_name"})
)
@Data
@NoArgsConstructor
@AllArgsConstructor
class ProcessedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String eventId;

    @Column(nullable = false)
    private String listenerName;

    @Column(nullable = false)
    private UUID correlationId;

    @Column(nullable = false)
    private Instant processedAt = Instant.now();

    public ProcessedEvent(String eventId, String listenerName, UUID correlationId) {
        this.eventId = eventId;
        this.listenerName = listenerName;
        this.correlationId = correlationId;
        this.processedAt = Instant.now();
    }
}
```

## 3.2.14 Testing Templates (MANDATORY)

### Unit Test Template (Mockito)

**Example:** `com/synergyflow/incident/internal/IncidentServiceImplTest.java`

```java
package io.monosense.synergyflow.incident.internal;

import io.monosense.synergyflow.commons.exception.ResourceNotFoundException;
import io.monosense.synergyflow.incident.IncidentCreatedEvent;
import io.monosense.synergyflow.incident.IncidentDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("IncidentServiceImpl Unit Tests")
class IncidentServiceImplTest {

    @Mock
    private IncidentRepository repository;

    @Mock
    private IncidentMapper mapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private IncidentServiceImpl service;

    private Incident testIncident;
    private IncidentDto testDto;

    @BeforeEach
    void setUp() {
        UUID testId = UUID.randomUUID();
        testIncident = Incident.builder()
            .id(testId)
            .title("Test incident")
            .description("Test description")
            .priority(Incident.Priority.HIGH)
            .severity(Incident.Severity.S2)
            .status(Incident.Status.NEW)
            .createdBy(UUID.randomUUID())
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .slaDeadline(Instant.now().plusSeconds(8 * 3600))
            .version(0)
            .build();

        testDto = new IncidentDto(
            testId,
            "Test incident",
            "Test description",
            IncidentDto.Priority.HIGH,
            IncidentDto.Severity.S2,
            IncidentDto.Status.NEW,
            null,
            UUID.randomUUID(),
            Instant.now(),
            Instant.now(),
            null,
            Instant.now().plusSeconds(8 * 3600),
            null,
            0
        );
    }

    @Test
    @DisplayName("Should create incident and publish event")
    void shouldCreateIncidentAndPublishEvent() {
        // Given
        when(mapper.toEntity(any(IncidentDto.class))).thenReturn(testIncident);
        when(repository.save(any(Incident.class))).thenReturn(testIncident);
        when(mapper.toDto(any(Incident.class))).thenReturn(testDto);

        // When
        IncidentDto result = service.createIncident(testDto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Test incident");

        // Verify event published
        ArgumentCaptor<IncidentCreatedEvent> eventCaptor = ArgumentCaptor.forClass(IncidentCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        IncidentCreatedEvent event = eventCaptor.getValue();
        assertThat(event.incidentId()).isEqualTo(testId);
        assertThat(event.priority()).isEqualTo(IncidentCreatedEvent.Priority.HIGH);

        verify(repository).save(any(Incident.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when incident not found")
    void shouldThrowExceptionWhenIncidentNotFound() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        when(repository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.getIncident(nonExistentId))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Incident")
            .hasMessageContaining(nonExistentId.toString());

        verify(repository).findById(nonExistentId);
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("Should resolve incident and publish event")
    void shouldResolveIncidentAndPublishEvent() {
        // Given
        UUID incidentId = testIncident.getId();
        when(repository.findById(incidentId)).thenReturn(Optional.of(testIncident));
        when(repository.save(any(Incident.class))).thenReturn(testIncident);
        when(mapper.toDto(any(Incident.class))).thenReturn(testDto);

        // When
        IncidentDto result = service.resolveIncident(incidentId, "Issue fixed");

        // Then
        assertThat(result).isNotNull();
        verify(repository).save(argThat(incident ->
            incident.getStatus() == Incident.Status.RESOLVED &&
            incident.getResolution().equals("Issue fixed") &&
            incident.getResolvedAt() != null
        ));
        verify(eventPublisher).publishEvent(any(IncidentResolvedEvent.class));
    }
}
```

### Integration Test Template

**Example:** `com/synergyflow/incident/IncidentControllerIT.java`

```java
package io.monosense.synergyflow.incident;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Incident API Integration Tests")
class IncidentControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "AGENT")
    @DisplayName("Should create incident and return 201 Created")
    void shouldCreateIncident() throws Exception {
        // Given
        IncidentDto dto = new IncidentDto(
            null,
            "Production server down",
            "Web server is not responding",
            IncidentDto.Priority.CRITICAL,
            IncidentDto.Severity.S1,
            null,
            null,
            UUID.randomUUID(),
            null,
            null,
            null,
            null,
            null,
            null
        );

        // When / Then
        mockMvc.perform(post("/api/v1/incidents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.title").value("Production server down"))
            .andExpect(jsonPath("$.priority").value("CRITICAL"))
            .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    @WithMockUser(roles = "AGENT")
    @DisplayName("Should return 400 Bad Request for invalid incident")
    void shouldReturn400ForInvalidIncident() throws Exception {
        // Given - missing required fields
        IncidentDto dto = new IncidentDto(
            null,
            "AB", // Too short (min 5 chars)
            null, // Missing description
            null, // Missing priority
            null, // Missing severity
            null,
            null,
            UUID.randomUUID(),
            null,
            null,
            null,
            null,
            null,
            null
        );

        // When / Then
        mockMvc.perform(post("/api/v1/incidents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.type").value("https://synergyflow.example.com/errors/validation-error"))
            .andExpect(jsonPath("$.validationErrors").exists());
    }

    @Test
    @WithMockUser(roles = "AGENT")
    @DisplayName("Should return 404 Not Found for non-existent incident")
    void shouldReturn404ForNonExistentIncident() throws Exception {
        // Given
        UUID nonExistentId = UUID.randomUUID();

        // When / Then
        mockMvc.perform(get("/api/v1/incidents/{id}", nonExistentId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.type").value("https://synergyflow.example.com/errors/not-found"))
            .andExpect(jsonPath("$.resourceType").value("Incident"))
            .andExpect(jsonPath("$.resourceId").value(nonExistentId.toString()));
    }
}
```

### Spring Modulith Verification Test (MANDATORY)

**Example:** `com/synergyflow/ModularityTests.java`

```java
package io.monosense.synergyflow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

import java.io.IOException;

/**
 * Spring Modulith module structure verification tests.
 * MANDATORY: This test must pass before merging any PR.
 */
@DisplayName("Spring Modulith Architecture Verification")
class ModularityTests {

    ApplicationModules modules = ApplicationModules.of(SynergyFlowApplication.class);

    @Test
    @DisplayName("Should verify module structure and dependencies")
    void verifiesModularStructure() {
        // Verifies:
        // - No cyclic dependencies between modules
        // - Internal packages are not accessed from other modules
        // - Declared allowedDependencies are respected
        modules.verify();
    }

    @Test
    @DisplayName("Should generate module documentation")
    void documentsModules() throws IOException {
        new Documenter(modules)
            .writeModulesAsPlantUml()          // Generate PlantUML component diagram
            .writeIndividualModulesAsPlantUml(); // Generate per-module diagrams
    }
}
```

---
