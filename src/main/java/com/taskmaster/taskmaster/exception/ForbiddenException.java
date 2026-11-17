package com.taskmaster.taskmaster.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an authenticated user is not allowed to perform an action,
 * e.g. viewing tasks of a team they do not belong to.
 * Results in HTTP 403 Forbidden.
 */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}