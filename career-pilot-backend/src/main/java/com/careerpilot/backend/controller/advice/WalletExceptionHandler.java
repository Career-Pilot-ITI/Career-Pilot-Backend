package com.careerpilot.backend.controller.advice;

import com.careerpilot.backend.controller.response.ApiResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Order(0)
public class WalletExceptionHandler {

    @ExceptionHandler(WalletException.InsufficientBalanceException.class)
    public ResponseEntity<ApiResponse> handleInsufficientBalance(WalletException.InsufficientBalanceException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(WalletException.WalletNotFoundException.class)
    public ResponseEntity<ApiResponse> handleWalletNotFound(WalletException.WalletNotFoundException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(WalletException.InvalidCoinPackException.class)
    public ResponseEntity<ApiResponse> handleInvalidCoinPack(WalletException.InvalidCoinPackException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<ApiResponse> buildResponse(String message, HttpStatus status) {
        return new ResponseEntity<>(new ApiResponse(message), status);
    }
}