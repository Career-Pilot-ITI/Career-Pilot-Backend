package com.careerpilot.backend.controller.advice;

public class AuthException {

  public static class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String message) {
      super(message);
    }
  }

  public static class UsernameAlreadyExistsException extends RuntimeException {
    public UsernameAlreadyExistsException(String message) {
      super(message);
    }
  }

  public static class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
      super(message);
    }
  }

  public static class VerificationCodeExpiredException extends RuntimeException {
    public VerificationCodeExpiredException(String message) {
      super(message);
    }
  }

  public static class InvalidVerificationCodeException extends RuntimeException {
    public InvalidVerificationCodeException(String message) {
      super(message);
    }
  }

  public static class AccountNotVerifiedException extends RuntimeException {
    public AccountNotVerifiedException(String message) {
      super(message);
    }
  }

  public static class OtpExpiredException extends RuntimeException {
    public OtpExpiredException(String message) {
      super(message);
    }
  }

  public static class InvalidOtpException extends RuntimeException {
    public InvalidOtpException(String message) {
      super(message);
    }
  }

  public static class OtpCooldownException extends RuntimeException {
    public OtpCooldownException(String message) {
      super(message);
    }
  }

  public static class OtpLockoutException extends RuntimeException {
    public OtpLockoutException(String message) {
      super(message);
    }
  }

  public static class OtpResendLimitException extends RuntimeException {
    public OtpResendLimitException(String message) {
      super(message);
    }
  }
}
