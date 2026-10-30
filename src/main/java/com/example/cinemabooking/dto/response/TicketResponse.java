package com.example.cinemabooking.dto.response;

import com.example.cinemabooking.entity.TicketStatus;
import java.time.LocalDateTime;

public record TicketResponse(Long id, Long movieId, String movieTitle, Integer seatNumber,
                             LocalDateTime purchaseDate, Long price, TicketStatus status,
                             LocalDateTime cancelledAt) {}
