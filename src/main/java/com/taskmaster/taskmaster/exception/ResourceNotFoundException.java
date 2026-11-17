package com.taskmaster.taskmaster.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a requested resource (task, team, user, ...) does not exist.
 * Results in HTTP 404 Not Found.
 */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }

    /** Convenience constructor, e.g. new ResourceNotFoundException("Task", 15) -> "Task not found with id: 15" */
    public ResourceNotFoundException(String resourceName, Long id) {
        this(resourceName + " not found with id: " + id);
    }
}