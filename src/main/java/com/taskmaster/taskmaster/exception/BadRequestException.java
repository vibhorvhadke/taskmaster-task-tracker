package com.taskmaster.taskmaster.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a request is well-formed but breaks a business rule,
 * e.g. assigning a task to a user who is not a team member.
 * Results in HTTP 400 Bad Request.
 */
public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}