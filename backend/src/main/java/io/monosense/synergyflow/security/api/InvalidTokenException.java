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
 */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message) {
        super(message);
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
