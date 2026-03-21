package com.whiteboard.app.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a requested resource does not exist in the database.
 * Automatically maps to HTTP 404 Not Found responses via @ResponseStatus.
 * Used throughout the service layer when entity lookups return empty Optionals.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Construct with a descriptive message identifying the missing resource.
     *
     * @param message description of what resource was not found
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Convenience constructor to build a standard "Entity with id X not found" message.
     *
     * @param resourceName the entity type (e.g., "Board", "User")
     * @param fieldName    the field used to look up (e.g., "id", "username")
     * @param fieldValue   the value that was searched for
     */
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
    }
}
