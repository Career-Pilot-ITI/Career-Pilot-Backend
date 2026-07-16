package com.careerpilot.backend.controller.advice;

import com.careerpilot.backend.controller.response.ApiResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalFallbackExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleUnexpected(Exception ex) {
        return new ResponseEntity<>(
                new ApiResponse("An unexpected error occurred. Please try again later."),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }
}