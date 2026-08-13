package com.careerpilot.backend.controller.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private String message;
    private Boolean success;
    private LocalDateTime timestamp;
    private T data;

    public ApiResponse(String message) {
        this.message = message;
        this.success = true;
        this.timestamp = LocalDateTime.now();
    }

    public static <T> ApiResponse<T> error(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setMessage(message);
        response.setSuccess(false);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }

    public static ApiResponseBuilder builder() {
        return new ApiResponseBuilder();
    }

    public static class ApiResponseBuilder {
        private String message;
        private Boolean success;
        private LocalDateTime timestamp;
        private Object data;

        public ApiResponseBuilder message(String message) {
            this.message = message;
            return this;
        }

        public ApiResponseBuilder success(Boolean success) {
            this.success = success;
            return this;
        }

        public ApiResponseBuilder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public ApiResponseBuilder data(Object data) {
            this.data = data;
            return this;
        }

        @SuppressWarnings("unchecked")
        public <R> ApiResponse<R> build() {
            ApiResponse<R> response = new ApiResponse<>();
            response.setMessage(message);
            response.setSuccess(success);
            response.setTimestamp(timestamp);
            response.setData((R) data);
            return response;
        }
    }
}
