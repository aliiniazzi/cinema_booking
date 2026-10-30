package com.example.cinemabooking;

import com.example.cinemabooking.dto.request.BookTicketRequest;
import com.example.cinemabooking.exception.GlobalExceptionHandler;
import jakarta.validation.Valid;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ApiErrorResponseTest {
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new TestController())
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    @Test
    void validationErrorsUseTheAssignmentFormat() throws Exception {
        mvc.perform(post("/api/tickets/book").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"movieId\": 1, \"seatNumber\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("seatNumber")))
                .andExpect(jsonPath("$.path").value("/api/tickets/book"));
    }

    @Test
    void conflictUsesTheAssignmentFormat() throws Exception {
        mvc.perform(post("/api/tickets/book").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"movieId\": 1, \"seatNumber\": 12}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.path").value("/api/tickets/book"));
    }

    @RestController
    static class TestController {
        @PostMapping("/api/tickets/book")
        void book(@Valid @RequestBody BookTicketRequest request) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Seat already booked");
        }
    }
}
