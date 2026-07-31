package com.careerpilot.backend.controller.advice;

import com.careerpilot.backend.controller.response.ApiResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Order(1)
public class JobScrapeExceptionHandler {

    @ExceptionHandler(JobScrapeException.NotConfiguredException.class)
    public ResponseEntity<ApiResponse> handleNotConfigured(JobScrapeException.NotConfiguredException ex) {
        return new ResponseEntity<>(new ApiResponse(ex.getMessage()), HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(JobScrapeException.InvalidParamsException.class)
    public ResponseEntity<ApiResponse> handleInvalidParams(JobScrapeException.InvalidParamsException ex) {
        return new ResponseEntity<>(new ApiResponse(ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(JobScrapeException.InvalidKeyException.class)
    public ResponseEntity<ApiResponse> handleInvalidKey(JobScrapeException.InvalidKeyException ex) {
        return new ResponseEntity<>(new ApiResponse(ex.getMessage()), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(JobScrapeException.InsufficientCreditsException.class)
    public ResponseEntity<ApiResponse> handleInsufficientCredits(JobScrapeException.InsufficientCreditsException ex) {
        return new ResponseEntity<>(new ApiResponse(ex.getMessage()), HttpStatus.PAYMENT_REQUIRED);
    }

    @ExceptionHandler(JobScrapeException.RateLimitedException.class)
    public ResponseEntity<ApiResponse> handleRateLimited(JobScrapeException.RateLimitedException ex) {
        return new ResponseEntity<>(new ApiResponse(ex.getMessage()), HttpStatus.TOO_MANY_REQUESTS);
    }

    @ExceptionHandler(JobScrapeException.JobNotFoundException.class)
    public ResponseEntity<ApiResponse> handleJobNotFound(JobScrapeException.JobNotFoundException ex) {
        return new ResponseEntity<>(new ApiResponse(ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(JobScrapeException.UnreachableException.class)
    public ResponseEntity<ApiResponse> handleUnreachable(JobScrapeException.UnreachableException ex) {
        return new ResponseEntity<>(new ApiResponse(ex.getMessage()), HttpStatus.BAD_GATEWAY);
    }
}
