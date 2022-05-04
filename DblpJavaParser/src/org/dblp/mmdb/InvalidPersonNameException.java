package org.dblp.mmdb;


/**
 * A custom exception that signals that a given name is not valid to given dblp naming conventions.
 *
 * @author Marcel R. Ackermann
 * @version 2017-02-22
 */
// TODO: make this a checked exception
public class InvalidPersonNameException extends RuntimeException {

    /** Generated serial version UID */
    private static final long serialVersionUID = -6161481453628946839L;

    /**
     * Constructs a new InvalidPersonNameException with the specified detail message.
     *
     * @param message The detail message.
     */
    public InvalidPersonNameException(String message) {
        super(message);
    }

    /**
     * Constructs a new InvalidPersonNameException with the specified detail message and cause.
     *
     * @param message The detail message.
     * @param cause The cause.
     */
    public InvalidPersonNameException(String message, Throwable cause) {
        super(message, cause);
    }
}
