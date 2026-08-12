package com.careerpilot.backend.controller.advice;

/**
 * Thrown when a requested resource (track, question, session, workspace, ...)
 * does not exist. Mapped to HTTP 404 by {@link ResourceNotFoundExceptionHandler}.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}