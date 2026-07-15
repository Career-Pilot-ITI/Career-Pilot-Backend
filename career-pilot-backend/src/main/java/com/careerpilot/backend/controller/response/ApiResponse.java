package com.careerpilot.backend.controller.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse {
    private String message;
    private Boolean success;
    private LocalDateTime timestamp;
    private Object data;

    public ApiResponse(String message) {
        this.message = message;
        this.success = true;
        this.timestamp = LocalDateTime.now();
    }
}