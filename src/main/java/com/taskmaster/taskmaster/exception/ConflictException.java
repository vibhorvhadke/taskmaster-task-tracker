package com.taskmaster.taskmaster.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a request conflicts with existing data,
 * e.g. registering with an email that is already in use.
 * Results in HTTP 409 Conflict.
 */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}