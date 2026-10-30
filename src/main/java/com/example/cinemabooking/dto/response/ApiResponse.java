package com.example.cinemabooking.dto.response;

import java.time.LocalDateTime;

/** Common success envelope. The assignment specifies an error format only. */
public record ApiResponse<T>(LocalDateTime timestamp, int status, String message, T data) {
    public static <T> ApiResponse<T> of(int status, String message, T data) {
        return new ApiResponse<>(LocalDateTime.now(), status, message, data);
    }
}
