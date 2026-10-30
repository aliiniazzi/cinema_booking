package com.example.cinemabooking.dto.response;

import java.util.List;

public record AvailableSeatsResponse(Long movieId, List<Integer> availableSeats) {
    public AvailableSeatsResponse {
        availableSeats = List.copyOf(availableSeats);
    }
}
