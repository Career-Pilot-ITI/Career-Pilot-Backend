package com.careerpilot.backend.controller.advice;

public class WorkspaceException {
  public static class WorkspaceNotFoundException extends RuntimeException {
    public WorkspaceNotFoundException(String message) {
      super(message);
    }
  }

}
