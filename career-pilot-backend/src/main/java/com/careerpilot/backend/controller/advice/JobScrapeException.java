package com.careerpilot.backend.controller.advice;

import org.springframework.http.HttpStatus;

/**
 * Typed failure for LinkedIn/ChocoData scraping. Each subclass carries its own
 * HTTP status, so the exception handler maps every variant with a single method
 * and callers (issue #98) can catch specific subclasses to steer the user
 * (e.g. JobNotFoundException -> suggest paste-text).
 */
public abstract class JobScrapeException extends RuntimeException {

    private final HttpStatus httpStatus;

    protected JobScrapeException(HttpStatus httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    /**
     * Return a new exception of the same concrete type (and therefore the same
     * HTTP status) with {@code suggestion} appended to the message. Used by the
     * import flow to steer the user toward paste-text when scraping fails.
     */
    public JobScrapeException suggest(String suggestion) {
        String amended = (getMessage() == null ? "" : getMessage()) + " " + suggestion;
        try {
            return getClass().getConstructor(String.class).newInstance(amended);
        } catch (Exception e) {
            return new UnreachableException(amended);
        }
    }

    public static class NotConfiguredException extends JobScrapeException {
        public NotConfiguredException(String message) {
            super(HttpStatus.SERVICE_UNAVAILABLE, message);
        }
    }

    public static class InvalidParamsException extends JobScrapeException {
        public InvalidParamsException(String message) {
            super(HttpStatus.BAD_REQUEST, message);
        }
    }

    public static class InvalidKeyException extends JobScrapeException {
        public InvalidKeyException(String message) {
            super(HttpStatus.UNAUTHORIZED, message);
        }
    }

    public static class InsufficientCreditsException extends JobScrapeException {
        public InsufficientCreditsException(String message) {
            super(HttpStatus.PAYMENT_REQUIRED, message);
        }
    }

    public static class RateLimitedException extends JobScrapeException {
        public RateLimitedException(String message) {
            super(HttpStatus.TOO_MANY_REQUESTS, message);
        }
    }

    public static class JobNotFoundException extends JobScrapeException {
        public JobNotFoundException(String message) {
            super(HttpStatus.NOT_FOUND, message);
        }
    }

    public static class UnreachableException extends JobScrapeException {
        public UnreachableException(String message) {
            super(HttpStatus.BAD_GATEWAY, message);
        }
    }
}
