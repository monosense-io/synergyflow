package io.monosense.synergyflow.itsm.internal.exception;

/**
 * Exception thrown when a required field is missing or invalid for a ticket operation.
 *
 * <p>This exception is thrown by TicketService methods when required fields are null, empty,
 * or do not meet minimum length requirements. Examples include:
 * <ul>
 *   <li>assigneeId is null when assigning a ticket</li>
 *   <li>resolutionNotes is null or length &lt; 10 characters when resolving a ticket</li>
 *   <li>reopenReason is null or length &lt; 10 characters when reopening a ticket</li>
 * </ul>
 *
 * <p>Public visibility within internal package to allow use across internal.* subpackages
 * (service, repository, etc.), but still maintains module boundary - only used within ITSM module.</p>
 *
 * @since 2.2
 */
public class MissingRequiredFieldException extends RuntimeException {

    private final String field;
    private final String operation;

    /**
     * Constructs a new MissingRequiredFieldException with the specified field name and operation.
     *
     * @param field     the name of the missing or invalid field
     * @param operation the operation that was being attempted
     */
    public MissingRequiredFieldException(String field, String operation) {
        super("Required field '" + field + "' missing for operation: " + operation);
        this.field = field;
        this.operation = operation;
    }

    /**
     * Returns the name of the missing or invalid field.
     *
     * @return the field name
     */
    public String getField() {
        return field;
    }

    /**
     * Returns the operation that was being attempted.
     *
     * @return the operation name
     */
    public String getOperation() {
        return operation;
    }
}
