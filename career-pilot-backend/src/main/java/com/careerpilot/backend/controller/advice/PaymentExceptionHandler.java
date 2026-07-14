package com.careerpilot.backend.controller.advice;

import com.careerpilot.backend.controller.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class PaymentExceptionHandler {

    @ExceptionHandler(PaymentException.InvalidWebhookSignatureException.class)
    public ResponseEntity<ApiResponse> handleInvalidSignature(PaymentException.InvalidWebhookSignatureException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(PaymentException.UnknownTransactionException.class)
    public ResponseEntity<ApiResponse> handleUnknownTransaction(PaymentException.UnknownTransactionException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(PaymentException.UnsupportedPaymentProviderException.class)
    public ResponseEntity<ApiResponse> handleUnsupportedProvider(PaymentException.UnsupportedPaymentProviderException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(PaymentException.PaymentProviderException.class)
    public ResponseEntity<ApiResponse> handleProviderFailure(PaymentException.PaymentProviderException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.BAD_GATEWAY);
    }

    private ResponseEntity<ApiResponse> buildResponse(String message, HttpStatus status) {
        return new ResponseEntity<>(new ApiResponse(message), status);
    }
}