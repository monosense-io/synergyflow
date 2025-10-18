package io.monosense.synergyflow.time.api;

import io.monosense.synergyflow.common.events.CorrelationContext;
import io.monosense.synergyflow.time.application.TimeEntryService;
import io.monosense.synergyflow.time.domain.TimeEntry;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * REST controller for managing time entries in SynergyFlow.
 *
 * <p>Provides endpoints for creating single or bulk time entries
 * with automatic mirroring to linked entities via event-driven architecture.
 *
 * @author monosense
 * @since 0.1.0
 * @version 0.1.0
 */
@RestController
@RequestMapping("/api/v1/time-entries")
public class TimeEntryController {

    private static final Logger log = LoggerFactory.getLogger(TimeEntryController.class);
    private final TimeEntryService timeEntryService;

    public TimeEntryController(TimeEntryService timeEntryService) {
        this.timeEntryService = timeEntryService;
    }

    /**
     * Creates a single time entry with optimistic UI updates in mind.
     *
     * @param request the time entry creation request
     * @param jwt the authenticated user's JWT token
     * @return tracking ID for monitoring mirroring progress
     */
    @PostMapping
    public ResponseEntity<TimeEntryResponse> createTimeEntry(
            @Valid @RequestBody CreateTimeEntryRequest request) {

        String userId = "test-user"; // TODO: Extract from JWT when auth is implemented
        String correlationId = CorrelationContext.getCorrelationId();

        log.info("Creating time entry for user: {}, duration: {} minutes, targets: {}",
                userId, request.durationMinutes(), request.targetEntities().size());

        TimeEntry timeEntry = timeEntryService.createTimeEntry(
                userId,
                Duration.ofMinutes(request.durationMinutes()),
                request.description(),
                request.occurredAt() != null ? request.occurredAt() : Instant.now(),
                request.targetEntities(),
                correlationId
        );

        return ResponseEntity.accepted()
                .body(new TimeEntryResponse(
                        timeEntry.id(),
                        "Time entry created and mirroring initiated",
                        List.of(timeEntry.id())
                ));
    }

    /**
     * Creates multiple time entries in a single transaction for bulk operations.
     *
     * @param request the bulk time entry creation request
     * @param jwt the authenticated user's JWT token
     * @return list of tracking IDs for monitoring progress
     */
    @PostMapping("/bulk")
    public ResponseEntity<BulkTimeEntryResponse> createBulkTimeEntries(
            @Valid @RequestBody CreateBulkTimeEntryRequest request) {

        String userId = "test-user"; // TODO: Extract from JWT when auth is implemented
        String correlationId = CorrelationContext.getCorrelationId();

        log.info("Creating {} bulk time entries for user: {}",
                request.entries().size(), userId);

        List<String> trackingIds = timeEntryService.createBulkTimeEntries(
                userId,
                request.entries(),
                correlationId
        );

        return ResponseEntity.accepted()
                .body(new BulkTimeEntryResponse(
                        "Bulk time entries created and mirroring initiated",
                        trackingIds
                ));
    }

    /**
     * Request DTO for creating a single time entry.
     */
    public record CreateTimeEntryRequest(
            @NotNull(message = "Duration is required")
            @Positive(message = "Duration must be positive")
            Integer durationMinutes,

            @NotBlank(message = "Description is required")
            String description,

            Instant occurredAt,

            @NotNull(message = "At least one target entity is required")
            List<TargetEntityDto> targetEntities
    ) {}

    /**
     * Request DTO for creating bulk time entries.
     */
    public record CreateBulkTimeEntryRequest(
            @NotNull(message = "Entries list is required")
            List<CreateTimeEntryRequest> entries
    ) {}

    /**
     * DTO representing a target entity for time mirroring.
     */
    public record TargetEntityDto(
            @NotNull(message = "Entity type is required")
            TimeEntryCreatedEvent.TargetEntity.EntityType type,

            @NotBlank(message = "Entity ID is required")
            String entityId,

            String entityTitle
    ) {}

    /**
     * Response DTO for time entry creation.
     */
    public record TimeEntryResponse(
            String trackingId,
            String message,
            List<String> timeEntryIds
    ) {}

    /**
     * Response DTO for bulk time entry creation.
     */
    public record BulkTimeEntryResponse(
            String message,
            List<String> trackingIds
    ) {}

    /**
     * Handles validation errors and returns RFC7807 compliant responses.
     *
     * @param ex the validation exception
     * @return RFC7807 problem details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        List<Map<String, String>> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> {
                    Map<String, String> errorMap = new HashMap<>();
                    errorMap.put("name", error.getField());
                    errorMap.put("message", error.getDefaultMessage());
                    errorMap.put("code", "VALIDATION_ERROR");
                    return errorMap;
                })
                .collect(Collectors.toList());

        Map<String, Object> problemDetails = new HashMap<>();
        problemDetails.put("type", "https://synergyflow.com/problems/validation-error");
        problemDetails.put("title", "Validation Error");
        problemDetails.put("status", HttpStatus.BAD_REQUEST.value());
        problemDetails.put("detail", "Request validation failed. Please check the provided data.");
        problemDetails.put("instance", "/api/v1/time-entries");
        problemDetails.put("timestamp", Instant.now().toString());
        problemDetails.put("correlationId", CorrelationContext.getCorrelationId());
        problemDetails.put("fields", fieldErrors);

        return ResponseEntity.badRequest()
                .contentType(org.springframework.http.MediaType.parseMediaType("application/problem+json"))
                .body(problemDetails);
    }

    /**
     * Handles generic exceptions and returns RFC7807 compliant responses.
     *
     * @param ex the exception
     * @return RFC7807 problem details
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericExceptions(Exception ex) {
        log.error("Unexpected error in time entry API", ex);

        Map<String, Object> problemDetails = new HashMap<>();
        problemDetails.put("type", "https://synergyflow.com/problems/internal-server-error");
        problemDetails.put("title", "Internal Server Error");
        problemDetails.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        problemDetails.put("detail", "An unexpected error occurred while processing your request.");
        problemDetails.put("instance", "/api/v1/time-entries");
        problemDetails.put("timestamp", Instant.now().toString());
        problemDetails.put("correlationId", CorrelationContext.getCorrelationId());

        // Don't expose stack trace in production
        if (log.isDebugEnabled()) {
            problemDetails.put("debugMessage", ex.getMessage());
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(org.springframework.http.MediaType.parseMediaType("application/problem+json"))
                .body(problemDetails);
    }
}