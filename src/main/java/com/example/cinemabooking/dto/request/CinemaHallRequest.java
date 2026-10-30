package com.example.cinemabooking.dto.request;

import jakarta.validation.constraints.*;

public record CinemaHallRequest(
        @NotBlank(message = "نام سالن الزامی است") @Size(max = 255, message = "نام سالن نباید بیش از ۲۵۵ کاراکتر باشد") String name,
        @NotNull(message = "ظرفیت سالن الزامی است") @Positive(message = "ظرفیت سالن باید مثبت باشد") Integer capacity) {}
