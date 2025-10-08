package io.monosense.synergyflow.security.api;

/**
 * Exception thrown when JWT token validation fails.
 *
 * <p>This exception is thrown when:
 * <ul>
 *   <li>Token signature is invalid</li>
 *   <li>Token is expired</li>
 *   <li>Token is malformed or cannot be decoded</li>
 *   <li>Required claims are missing</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>{@code
 * try {
 *     JwtClaims claims = jwtValidator.validateAndExtractClaims(token);
 *     // proceed with authentication
 * } catch (InvalidTokenException e) {
 *     // handle invalid token scenario
 *     log.error("Authentication failed: {}", e.getMessage());
 * }
 * }</pre>
 *
 * @author monosense
 * @since 1.0.0
 * @version 1.0.0
 */
public class InvalidTokenException extends RuntimeException {

    /**
     * Constructs a new InvalidTokenException with the specified detail message.
     *
     * @param message the detail message explaining why token validation failed
     */
    public InvalidTokenException(String message) {
        super(message);
    }

    /**
     * Constructs a new InvalidTokenException with the specified detail message and cause.
     *
     * @param message the detail message explaining why token validation failed
     * @param cause the underlying cause of the token validation failure
     */
    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
