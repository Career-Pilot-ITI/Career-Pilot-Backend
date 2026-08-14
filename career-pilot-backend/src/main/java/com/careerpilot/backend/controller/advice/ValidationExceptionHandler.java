package com.careerpilot.backend.controller.advice;

import com.careerpilot.backend.controller.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * Maps malformed request inputs to HTTP 400 with a human-readable message.
 * Covers body validation failures, bad path/query conversions, missing
 * parameters, and unreadable JSON bodies.
 */
@ControllerAdvice
@Order(1)
public class ValidationExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleBodyValidation(MethodArgumentNotValidException ex) {
    String message = ex.getBindingResult().getFieldErrors().stream()
        .map(err -> err.getField() + ": " + err.getDefaultMessage())
        .distinct()
        .collect(Collectors.joining("; "));
    if (message.isBlank()) {
      message = "Validation failed";
    }
    return build(message);
  }

  @ExceptionHandler(HandlerMethodValidationException.class)
  public ResponseEntity<ApiResponse<Void>> handleParamValidation(HandlerMethodValidationException ex) {
    String message = ex.getAllErrors().stream()
        .map(err -> err.getDefaultMessage())
        .distinct()
        .collect(Collectors.joining("; "));
    return build(message.isBlank() ? "Validation failed" : message);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
    String message = ex.getConstraintViolations().stream()
        .map(v -> v.getPropertyPath() + ": " + v.getMessage())
        .distinct()
        .collect(Collectors.joining("; "));
    return build(message.isBlank() ? "Validation failed" : message);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
    String name = ex.getName() != null ? ex.getName() : "parameter";
    return build("Incorrect type for " + name + ". Expected " + targetType(ex));
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ApiResponse<Void>> handleMissingParameter(MissingServletRequestParameterException ex) {
    return build("Required request parameter '" + ex.getParameterName() + "' is missing");
  }

  @ExceptionHandler(MissingServletRequestPartException.class)
  public ResponseEntity<ApiResponse<Void>> handleMissingPart(MissingServletRequestPartException ex) {
    return build("Required part '" + ex.getRequestPartName() + "' is missing");
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiResponse<Void>> handleUnreadableBody(HttpMessageNotReadableException ex) {
    return build("Malformed or missing request body");
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException ex) {
    return build("Invalid resource path: " + ex.getResourcePath());
  }

  @ExceptionHandler(PropertyReferenceException.class)
  public ResponseEntity<ApiResponse<Void>> handlePropertyReference(PropertyReferenceException ex) {
    return build("Invalid sort property '" + ex.getPropertyName() + "' for type " + ex.getType());
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ApiResponse<Void>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
    return new ResponseEntity<>(
        ApiResponse.error("Unsupported Content-Type. Supported: " + ex.getSupportedMediaTypes()),
        HttpStatus.UNSUPPORTED_MEDIA_TYPE);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
    return build(ex.getMessage() != null ? ex.getMessage() : "Invalid request argument");
  }

  private String targetType(MethodArgumentTypeMismatchException ex) {
    Class<?> required = ex.getRequiredType();
    if (required == null) {
      return "a different value";
    }
    if (required.isEnum()) {
      return "one of " + java.util.Arrays.toString(required.getEnumConstants());
    }
    return required.getSimpleName().toLowerCase();
  }

  private ResponseEntity<ApiResponse<Void>> build(String message) {
    return new ResponseEntity<>(ApiResponse.error(message), HttpStatus.BAD_REQUEST);
  }
}