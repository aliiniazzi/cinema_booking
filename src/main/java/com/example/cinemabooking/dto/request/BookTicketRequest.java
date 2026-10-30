package com.example.cinemabooking.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import io.swagger.v3.oas.annotations.media.Schema;

public record BookTicketRequest(
        @NotNull(message = "شناسه فیلم الزامی است") @Positive(message = "شناسه فیلم باید مثبت باشد") @Schema(example = "1") Long movieId,
        @NotNull(message = "شماره صندلی الزامی است") @Positive(message = "شماره صندلی باید مثبت باشد") @Schema(example = "12") Integer seatNumber) {}
