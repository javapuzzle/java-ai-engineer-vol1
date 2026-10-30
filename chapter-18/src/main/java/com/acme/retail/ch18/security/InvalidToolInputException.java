package com.acme.retail.ch18.security;

/**
 * Thrown when something asks {@code OrderStatusLookupTool} to run with an
 * argument that does not look like a real order code.
 *
 * <p>This is deliberately an unchecked exception with a message that
 * does not echo the raw input back -- so a caller cannot use the
 * exception itself as a way to smuggle attacker-controlled text into a
 * log line or a downstream prompt.
 */
public class InvalidToolInputException extends RuntimeException {

    public InvalidToolInputException(String message) {
        super(message);
    }
}
