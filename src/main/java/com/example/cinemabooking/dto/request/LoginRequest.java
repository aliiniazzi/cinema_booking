package com.example.cinemabooking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

public record LoginRequest(@NotBlank(message = "نام کاربری الزامی است") @Size(max = 20, message = "نام کاربری نباید بیش از ۲۰ کاراکتر باشد") @Schema(example = "admin") String username,
                           @NotBlank(message = "رمز عبور الزامی است") @Size(max = 72, message = "رمز عبور نباید بیش از ۷۲ کاراکتر باشد") @Schema(example = "admin123", accessMode = Schema.AccessMode.WRITE_ONLY) String password) {}
