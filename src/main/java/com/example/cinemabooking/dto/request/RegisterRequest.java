package com.example.cinemabooking.dto.request;

import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;

public record RegisterRequest(
        @NotBlank(message = "نام کاربری الزامی است") @Size(min = 3, max = 20, message = "نام کاربری باید بین ۳ تا ۲۰ کاراکتر باشد") @Schema(example = "newuser") String username,
        @NotBlank(message = "ایمیل الزامی است") @Email(message = "قالب ایمیل معتبر نیست") @Size(max = 255, message = "ایمیل نباید بیش از ۲۵۵ کاراکتر باشد") @Schema(example = "newuser@example.com") String email,
        @NotBlank(message = "رمز عبور الزامی است") @Size(min = 6, max = 72, message = "رمز عبور باید بین ۶ تا ۷۲ کاراکتر باشد") @Schema(example = "password123", accessMode = Schema.AccessMode.WRITE_ONLY) String password) {}
