package com.careerpilot.backend.controller.advice;

public class JobScrapeException extends RuntimeException {

    public JobScrapeException(String message) { super(message); }

    public static class NotConfiguredException extends JobScrapeException {
        public NotConfiguredException(String message) { super(message); }
    }

    public static class InvalidParamsException extends JobScrapeException {
        public InvalidParamsException(String message) { super(message); }
    }

    public static class InvalidKeyException extends JobScrapeException {
        public InvalidKeyException(String message) { super(message); }
    }

    public static class InsufficientCreditsException extends JobScrapeException {
        public InsufficientCreditsException(String message) { super(message); }
    }

    public static class RateLimitedException extends JobScrapeException {
        public RateLimitedException(String message) { super(message); }
    }

    public static class JobNotFoundException extends JobScrapeException {
        public JobNotFoundException(String message) { super(message); }
    }

    public static class UnreachableException extends JobScrapeException {
        public UnreachableException(String message) { super(message); }
    }
}
