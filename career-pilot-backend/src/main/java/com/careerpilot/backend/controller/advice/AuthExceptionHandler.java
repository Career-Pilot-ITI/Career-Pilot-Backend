package com.careerpilot.backend.controller.advice;

import com.careerpilot.backend.aspect.RateLimitExceededException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;
import com.careerpilot.backend.controller.response.ApiResponse;

@ControllerAdvice
@Order(0)
public class AuthExceptionHandler {

  @ExceptionHandler(AuthException.UserAlreadyExistsException.class)
  public ResponseEntity<ApiResponse> handleUserAlreadyExistsException(AuthException.UserAlreadyExistsException ex) {
    return buildResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(AuthException.UsernameAlreadyExistsException.class)
  public ResponseEntity<ApiResponse> handleUsernameAlreadyExistsException(
      AuthException.UsernameAlreadyExistsException ex) {
    return buildResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(AuthException.UserNotFoundException.class)
  public ResponseEntity<ApiResponse> handleUserNotFoundException(AuthException.UserNotFoundException ex) {
    return buildResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(AuthException.AccountNotVerifiedException.class)
  public ResponseEntity<ApiResponse> handleAccountNotVerifiedException(AuthException.AccountNotVerifiedException ex) {
    return buildResponse(ex.getMessage(), HttpStatus.FORBIDDEN);
  }

  @ExceptionHandler(AuthException.VerificationCodeExpiredException.class)
  public ResponseEntity<ApiResponse> handleVerificationCodeExpiredException(
      AuthException.VerificationCodeExpiredException ex) {
    return buildResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(AuthException.InvalidVerificationCodeException.class)
  public ResponseEntity<ApiResponse> handleInvalidVerificationCodeException(
      AuthException.InvalidVerificationCodeException ex) {
    return buildResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(RateLimitExceededException.class)
  public ResponseEntity<ApiResponse> handleRateLimitExceededException(RateLimitExceededException ex) {
    return buildResponse(ex.getMessage(), HttpStatus.TOO_MANY_REQUESTS);
  }

  @ExceptionHandler(AuthException.OtpExpiredException.class)
  public ResponseEntity<ApiResponse> handleOtpExpired(AuthException.OtpExpiredException ex) {
    return buildResponse(ex.getMessage(), HttpStatus.GONE);
  }

  @ExceptionHandler(AuthException.InvalidOtpException.class)
  public ResponseEntity<ApiResponse> handleInvalidOtp(AuthException.InvalidOtpException ex) {
    return buildResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(AuthException.OtpCooldownException.class)
  public ResponseEntity<ApiResponse> handleOtpCooldown(AuthException.OtpCooldownException ex) {
    return buildResponse(ex.getMessage(), HttpStatus.TOO_MANY_REQUESTS);
  }

  @ExceptionHandler(AuthException.OtpLockoutException.class)
  public ResponseEntity<ApiResponse> handleOtpLockout(AuthException.OtpLockoutException ex) {
    return buildResponse(ex.getMessage(), HttpStatus.TOO_MANY_REQUESTS);
  }

  @ExceptionHandler(AuthException.OtpResendLimitException.class)
  public ResponseEntity<ApiResponse> handleOtpResendLimit(AuthException.OtpResendLimitException ex) {
    return buildResponse(ex.getMessage(), HttpStatus.TOO_MANY_REQUESTS);
  }

  private ResponseEntity<ApiResponse> buildResponse(String message, HttpStatus status) {
    ApiResponse apiResponse = new ApiResponse(message);
    return new ResponseEntity<>(apiResponse, status);
  }
}
