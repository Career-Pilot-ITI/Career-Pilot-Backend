package com.careerpilot.backend.controller.advice;

public class AiJobException {
  public static class AiJobNotFoundException extends RuntimeException {
    public AiJobNotFoundException(String message) {
      super(message);
    }
  }
}
