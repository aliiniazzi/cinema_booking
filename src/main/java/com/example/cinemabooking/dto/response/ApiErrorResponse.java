package com.example.cinemabooking.dto.response;

import java.time.LocalDateTime;

/** Error format specified in the project assignment. */
public record ApiErrorResponse(LocalDateTime timestamp, int status, String error,
                               String message, String path) {}
