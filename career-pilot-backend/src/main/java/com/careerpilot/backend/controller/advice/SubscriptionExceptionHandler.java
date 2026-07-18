package com.careerpilot.backend.controller.advice;

import com.careerpilot.backend.controller.response.ApiResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Order(0)
public class SubscriptionExceptionHandler {

    @ExceptionHandler(SubscriptionException.NoActiveSubscriptionException.class)
    public ResponseEntity<ApiResponse> handleNoActiveSubscription(SubscriptionException.NoActiveSubscriptionException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(SubscriptionException.InvalidTierException.class)
    public ResponseEntity<ApiResponse> handleInvalidTier(SubscriptionException.InvalidTierException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<ApiResponse> buildResponse(String message, HttpStatus status) {
        return new ResponseEntity<>(new ApiResponse(message), status);
    }
}