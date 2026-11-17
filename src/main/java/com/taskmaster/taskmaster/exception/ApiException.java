package com.taskmaster.taskmaster.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base class for all application-specific exceptions.
 * Each subclass defines the HTTP status returned to the client.
 * Handled centrally by GlobalExceptionHandler.
 */
@Getter
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}