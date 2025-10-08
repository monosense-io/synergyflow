package io.monosense.synergyflow.itsm.internal.domain;

/**
 * Enumeration of approval statuses for service requests.
 *
 * <p>Defines the approval workflow states for service request tickets:</p>
 * <ul>
 *   <li>{@link #PENDING} - Awaiting approval decision</li>
 *   <li>{@link #APPROVED} - Request has been approved and can proceed to fulfillment</li>
 *   <li>{@link #REJECTED} - Request has been denied</li>
 * </ul>
 *
 * @since 2.1
 */
public enum ApprovalStatus {
    /**
     * Request is awaiting approval decision.
     */
    PENDING,

    /**
     * Request has been approved and can proceed.
     */
    APPROVED,

    /**
     * Request has been denied.
     */
    REJECTED
}
