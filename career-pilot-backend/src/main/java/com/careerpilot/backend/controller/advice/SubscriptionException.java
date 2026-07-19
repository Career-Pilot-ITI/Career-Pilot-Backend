package com.careerpilot.backend.controller.advice;

public class SubscriptionException {
    public static class NoActiveSubscriptionException extends RuntimeException {
        public NoActiveSubscriptionException(String message) { super(message); }
    }

    public static class InvalidTierException extends RuntimeException {
        public InvalidTierException(String message) { super(message); }
    }
}