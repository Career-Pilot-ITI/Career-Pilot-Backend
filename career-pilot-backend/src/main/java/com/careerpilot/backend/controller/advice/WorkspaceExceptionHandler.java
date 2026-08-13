package com.careerpilot.backend.controller.advice;

import com.careerpilot.backend.controller.response.ApiResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Order(1)
public class WorkspaceExceptionHandler {

  @ExceptionHandler(SessionQuotaException.QuotaExceededException.class)
  public ResponseEntity<ApiResponse<Void>> handleQuotaExceeded(SessionQuotaException.QuotaExceededException ex) {
    return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.TOO_MANY_REQUESTS);
  }

  @ExceptionHandler(WorkspaceException.WorkspaceNotFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleWorkspaceNotFound(WorkspaceException.WorkspaceNotFoundException ex) {
    return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(WorkspaceException.CvNotFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleCvNotFound(WorkspaceException.CvNotFoundException ex) {
    return new ResponseEntity<>(ApiResponse.error(ex.getMessage()), HttpStatus.BAD_REQUEST);
  }
}
