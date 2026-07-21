package com.careerpilot.backend.controller.advice;

public class SessionQuotaException {
    public static class QuotaExceededException extends RuntimeException {
        public QuotaExceededException(String message) { super(message); }
    }
}
