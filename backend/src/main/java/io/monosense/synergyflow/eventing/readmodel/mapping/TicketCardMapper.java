package io.monosense.synergyflow.eventing.readmodel.mapping;

import io.monosense.synergyflow.eventing.worker.model.TicketAssignedMessage;
import io.monosense.synergyflow.eventing.worker.model.TicketCreatedMessage;
import io.monosense.synergyflow.eventing.readmodel.TicketCardEntity;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.time.Instant;
import java.util.UUID;

/**
 * MapStruct mapper for converting ticket event messages to TicketCardEntity read model.
 *
 * <p>This mapper implements the data transformation layer between domain events and the
 * read model entities. It uses MapStruct code generation to provide type-safe mapping
 * with compile-time validation and performance optimization.</p>
 *
 * <p><strong>Mapping Responsibilities:</strong>
 * <ul>
 *   <li><strong>Event-to-Entity Conversion:</strong> Transform domain event messages to read model entities</li>
 *   <li><strong>Field Mapping:</strong> Handle field name differences and data type conversions</li>
 *   <li><strong>Default Value Handling:</strong> Ensure consistent default values for missing data</li>
 *   <li><strong>Incremental Updates:</strong> Support partial updates for entity modification events</li>
 * </ul>
 *
 * <p><strong>Design Principles:</strong>
 * <ul>
 *   <li><strong>Immutability:</strong> Source messages are never modified</li>
 *   <li><strong>Type Safety:</strong> Compile-time checking prevents mapping errors</li>
 *   <li><strong>Performance:</strong> Generated code provides optimal performance</li>
 *   <li><strong>Validation:</strong> ERROR policy ensures all fields are explicitly mapped</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * @Service
 * public class TicketReadModelHandler {
 *     @Autowired
 *     private TicketCardMapper mapper;
 *
 *     public void handleTicketCreated(TicketCreatedMessage message) {
 *         TicketCardEntity entity = mapper.toTicketCard(message, message.getVersion());
 *         repository.save(entity);
 *     }
 *
 *     public void handleTicketAssigned(TicketAssignedMessage message) {
 *         TicketCardEntity entity = repository.findByTicketId(message.getTicketId());
 *         if (entity != null) {
 *             mapper.applyAssignment(entity, message);
 *             repository.save(entity);
 *         }
 *     }
 * }
 * }</pre>
 *
 * @author monosense
 * @since 1.0.0
 * @version 1.0.0
 * @see TicketCardEntity
 * @see TicketCreatedMessage
 * @see TicketAssignedMessage
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TicketCardMapper {

    @Mapping(target = "id", expression = "java(java.util.UUID.randomUUID())")
    @Mapping(target = "ticketId", source = "message.ticketId")
    @Mapping(target = "ticketNumber", source = "message.ticketNumber")
    @Mapping(target = "title", source = "message.title")
    @Mapping(target = "status", source = "message.status")
    @Mapping(target = "priority", source = "message.priority")
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "requesterName", source = "message.requesterName")
    @Mapping(target = "requesterEmail", source = "message.requesterEmail")
    @Mapping(target = "assigneeName", ignore = true)
    @Mapping(target = "assigneeEmail", ignore = true)
    @Mapping(target = "slaDueAt", ignore = true)
    @Mapping(target = "slaStatus", ignore = true)
    @Mapping(target = "overdue", constant = "false")
    @Mapping(target = "createdAt", source = "message.createdAt")
    @Mapping(target = "updatedAt", source = "message.updatedAt")
    @Mapping(target = "version", source = "version")
    /**
     * Maps a TicketCreatedMessage to a new TicketCardEntity.
     *
     * <p>This method creates a new TicketCardEntity from a ticket creation event,
     * establishing the initial state of the read model. The mapping includes
     * field transformations and default value assignments to ensure data consistency.</p>
     *
     * <p><strong>Mapping Details:</strong>
     * <ul>
     *   <li>Generates a new UUID for the read model primary key</li>
     *   <li>Copies all relevant fields from the event message</li>
     *   <li>Sets initial values for SLA and assignment fields</li>
     *   <li>Applies version control for optimistic locking</li>
     * </ul>
     *
     * @param message the ticket creation event message containing source data
    * @param version the aggregate version for optimistic locking
    * @return a new TicketCardEntity populated with data from the message
    * @throws IllegalArgumentException if message is null
     */
    TicketCardEntity toTicketCard(TicketCreatedMessage message, Long version);

    /**
     * Ensures default values are applied after the main mapping process.
     *
     * <p>This callback method is executed after the primary field mapping to
     * handle default value assignments and data validation. It ensures that
     * all required fields have valid values even when the source message
     * contains incomplete data.</p>
     *
     * <p><strong>Default Value Rules:</strong>
     * <ul>
     *   <li>ID: Generates UUID if not present</li>
     *   <li>Requester Name: "Unknown" if null or empty</li>
     *   <li>Requester Email: "unknown@example.com" if null or empty</li>
     *   <li>Title: "Untitled" if null or empty</li>
     *   <li>Priority: "MEDIUM" if null or empty</li>
     *   <li>Status: "OPEN" if null or empty</li>
     *   <li>Timestamps: Current time if not set</li>
     * </ul>
     *
     * @param entity the target entity being mapped (modified in-place)
     */
    @AfterMapping
    default void ensureDefaults(@MappingTarget TicketCardEntity entity) {
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
        }
        if (entity.getRequesterName() == null || entity.getRequesterName().isBlank()) {
            entity.setRequesterName("Unknown");
        }
        if (entity.getRequesterEmail() == null || entity.getRequesterEmail().isBlank()) {
            entity.setRequesterEmail("unknown@example.com");
        }
        if (entity.getTitle() == null || entity.getTitle().isBlank()) {
            entity.setTitle("Untitled");
        }
        if (entity.getPriority() == null || entity.getPriority().isBlank()) {
            entity.setPriority("MEDIUM");
        }
        if (entity.getStatus() == null || entity.getStatus().isBlank()) {
            entity.setStatus("OPEN");
        }
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        if (entity.getUpdatedAt() == null) {
            entity.setUpdatedAt(entity.getCreatedAt());
        }
    }

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "assigneeName", source = "message.assigneeName")
    @Mapping(target = "assigneeEmail", source = "message.assigneeEmail")
    @Mapping(target = "updatedAt", source = "message.assignedAt")
    @Mapping(target = "version", source = "message.version")
    /**
     * Applies assignment updates to an existing TicketCardEntity.
     *
     * <p>This method performs a partial update of an existing ticket card entity
     * to reflect ticket assignment changes. It uses the {@code @BeanMapping}
     * annotation to only update the specified fields while preserving all
     * existing entity data.</p>
     *
     * <p><strong>Updated Fields:</strong>
     * <ul>
     *   <li>Assignee Name: Set to the assigned agent's name</li>
     *   <li>Assignee Email: Set to the assigned agent's email</li>
     *   <li>Updated At: Set to the assignment timestamp</li>
     *   <li>Version: Updated for optimistic locking</li>
     * </ul>
     *
     * <p><strong>Usage Context:</strong>
     * <p>This method is typically called when handling {@link TicketAssignedMessage}
     * events to update the read model without recreating the entire entity.</p>
     *
     * @param entity the existing TicketCardEntity to update (modified in-place)
     * @param message the ticket assignment event containing update data
     */
    void applyAssignment(@MappingTarget TicketCardEntity entity, TicketAssignedMessage message);
}
