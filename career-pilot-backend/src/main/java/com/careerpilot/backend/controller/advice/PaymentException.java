package com.careerpilot.backend.controller.advice;

public class PaymentException {

    public static class InvalidWebhookSignatureException extends RuntimeException {
        public InvalidWebhookSignatureException(String message) { super(message); }
    }

    public static class UnknownTransactionException extends RuntimeException {
        public UnknownTransactionException(String message) { super(message); }
    }

    public static class UnsupportedPaymentProviderException extends RuntimeException {
        public UnsupportedPaymentProviderException(String message) { super(message); }
    }

    public static class PaymentProviderException extends RuntimeException {
        public PaymentProviderException(String message) { super(message); }
        public PaymentProviderException(String message, Throwable cause) { super(message, cause); }
    }
}