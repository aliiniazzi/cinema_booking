package com.example.cinemabooking.dto.response;

import java.time.LocalDateTime;

/** Common response body for failed requests. */
public record ApiErrorResponse(LocalDateTime timestamp, int status, String error,
                               String message, String path) {}
