package com.taskmaster.taskmaster.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a request is not authenticated (missing, invalid, expired or revoked token).
 * Results in HTTP 401 Unauthorized.
 */
public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}