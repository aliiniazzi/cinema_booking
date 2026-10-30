package com.example.cinemabooking.controller;

import com.example.cinemabooking.dto.response.ApiResponse;
import com.example.cinemabooking.dto.response.SalesStatsResponse;
import com.example.cinemabooking.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/tickets")
@Tag(name = "آمار فروش", description = "فقط مدیر")
public class AdminTicketController {
    private final TicketService tickets;
    public AdminTicketController(TicketService tickets) { this.tickets = tickets; }

    @GetMapping("/stats")
    @Operation(summary = "آمار فروش کل و به تفکیک فیلم", description = "فقط بلیط‌های فعال شمرده می‌شوند؛ درآمد به تومان است.")
    public ApiResponse<SalesStatsResponse> stats() {
        return ApiResponse.of(200, "آمار فروش", tickets.salesStats());
    }
}
