package com.careerpilot.backend.controller.advice;

import com.careerpilot.backend.controller.response.ApiResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Order(1)
public class AiJobExceptionHandler {

  @ExceptionHandler(AiJobException.AiJobNotFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleAiJobNotFound(AiJobException.AiJobNotFoundException ex) {
    return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.NOT_FOUND);
  }
}
