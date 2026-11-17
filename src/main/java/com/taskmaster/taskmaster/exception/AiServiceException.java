package com.taskmaster.taskmaster.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when the external AI service (OpenRouter) cannot produce a result:
 * not configured (503), rate limited (429), timed out (504) or returned an error (502).
 */
public class AiServiceException extends ApiException {

    public AiServiceException(HttpStatus status, String message) {
        super(status, message);
    }
}