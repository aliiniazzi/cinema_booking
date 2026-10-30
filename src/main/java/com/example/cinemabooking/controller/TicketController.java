package com.example.cinemabooking.controller;

import com.example.cinemabooking.dto.request.BookTicketRequest;
import com.example.cinemabooking.dto.response.ApiResponse;
import com.example.cinemabooking.dto.response.TicketResponse;
import com.example.cinemabooking.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
@Validated
@Tag(name = "بلیط‌ها", description = "عملیات کاربر با نقش USER")
public class TicketController {
    private final TicketService tickets;
    public TicketController(TicketService tickets) { this.tickets = tickets; }

    @PostMapping("/book")
    @Operation(summary = "خرید بلیط", description = "برای صندلی اشغال‌شده پاسخ ۴۰۹ دریافت می‌کنید. قیمت از فیلم گرفته می‌شود.")
    public ResponseEntity<ApiResponse<TicketResponse>> book(@Valid @RequestBody BookTicketRequest request,
                                                             Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(201, "بلیط با موفقیت خریداری شد",
                tickets.book(authentication.getName(), request)));
    }

    @GetMapping("/my-tickets")
    @Operation(summary = "بلیط‌های من", description = "بلیط‌های فعال و لغوشده، حتی در صورت حذف فیلم، نمایش داده می‌شوند.")
    public ApiResponse<List<TicketResponse>> myTickets(Authentication authentication) {
        return ApiResponse.of(200, "فهرست بلیط‌های شما", tickets.myTickets(authentication.getName()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "لغو بلیط متعلق به کاربر", description = "صندلی آزاد می‌شود و سابقه بلیط حفظ می‌شود.")
    public ApiResponse<Void> cancel(@PathVariable @Positive(message = "شناسه بلیط باید عددی مثبت باشد") Long id,
                                    Authentication authentication) {
        tickets.cancel(authentication.getName(), id);
        return ApiResponse.of(200, "بلیط با موفقیت لغو شد", null);
    }
}
