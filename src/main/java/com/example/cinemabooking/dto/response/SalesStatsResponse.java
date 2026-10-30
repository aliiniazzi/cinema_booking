package com.example.cinemabooking.dto.response;

import java.util.List;

public record SalesStatsResponse(long totalTickets, long totalRevenue, List<MovieSalesStatsResponse> movies) {
    public SalesStatsResponse { movies = List.copyOf(movies); }
}
