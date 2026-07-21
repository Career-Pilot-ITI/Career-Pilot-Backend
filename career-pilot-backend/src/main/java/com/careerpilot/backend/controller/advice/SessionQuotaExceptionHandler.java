package com.careerpilot.backend.controller.advice;

import com.careerpilot.backend.controller.response.ApiResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Order(1)
public class SessionQuotaExceptionHandler {

    @ExceptionHandler(SessionQuotaException.QuotaExceededException.class)
    public ResponseEntity<ApiResponse> handleQuotaExceeded(SessionQuotaException.QuotaExceededException ex) {
        return new ResponseEntity<>(new ApiResponse(ex.getMessage()), HttpStatus.FORBIDDEN);
    }
}
