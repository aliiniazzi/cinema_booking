package com.example.cinemabooking.dto.response;

public record MovieSalesStatsResponse(Long movieId, String movieTitle,
                                      long activeTicketCount, long totalRevenue) {}
