package com.careerpilot.backend.controller.advice;

public class WalletException {
    public static class InsufficientBalanceException extends RuntimeException {
        public InsufficientBalanceException(String message) { super(message); }
    }

    public static class WalletNotFoundException extends RuntimeException {
        public WalletNotFoundException(String message) { super(message); }
    }

    public static class InvalidCoinPackException extends RuntimeException {
        public InvalidCoinPackException(String message) { super(message); }
    }
}