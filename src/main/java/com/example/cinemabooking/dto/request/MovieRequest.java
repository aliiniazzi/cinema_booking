package com.example.cinemabooking.dto.request;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import io.swagger.v3.oas.annotations.media.Schema;

public record MovieRequest(
        @NotBlank(message = "عنوان فیلم الزامی است") @Size(max = 255, message = "عنوان نباید بیش از ۲۵۵ کاراکتر باشد") @Schema(example = "A New Film") String title,
        @NotBlank(message = "ژانر الزامی است") @Size(max = 255, message = "ژانر نباید بیش از ۲۵۵ کاراکتر باشد") @Schema(example = "Drama") String genre,
        @NotNull(message = "مدت فیلم الزامی است") @Positive(message = "مدت فیلم باید مثبت باشد") @Schema(example = "120") Integer durationMinutes,
        @NotNull(message = "تاریخ اکران الزامی است") @Schema(example = "2026-01-01") LocalDate releaseDate,
        @NotBlank(message = "توضیحات الزامی است") @Size(max = 2000, message = "توضیحات نباید بیش از ۲۰۰۰ کاراکتر باشد") @Schema(example = "A sample film") String description,
        @NotNull(message = "قیمت الزامی است") @PositiveOrZero(message = "قیمت نمی‌تواند منفی باشد") @Schema(example = "120000", description = "قیمت به تومان") Long price,
        @NotNull(message = "شناسه سالن الزامی است") @Positive(message = "شناسه سالن باید مثبت باشد") @Schema(example = "1") Long cinemaHallId) {}
