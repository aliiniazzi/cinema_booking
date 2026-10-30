package com.example.cinemabooking.controller;

import com.example.cinemabooking.dto.response.ApiResponse;
import com.example.cinemabooking.dto.response.AvailableSeatsResponse;
import com.example.cinemabooking.dto.response.MovieResponse;
import com.example.cinemabooking.service.MovieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/movies")
@Validated
@Tag(name = "Movies", description = "Public movie and seat information")
public class MovieController {
    private final MovieService movies;
    public MovieController(MovieService movies) { this.movies = movies; }

    @GetMapping
    @Operation(summary = "List available movies")
    public ApiResponse<List<MovieResponse>> list() { return ApiResponse.of(200, "Movies", movies.list(null)); }

    @GetMapping("/search")
    @Operation(summary = "Search by partial, case-insensitive title")
    public ApiResponse<List<MovieResponse>> search(@RequestParam @NotBlank(message = "عنوان جستجو الزامی است") String title) {
        return ApiResponse.of(200, "Movies", movies.list(title));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a movie")
    public ApiResponse<MovieResponse> get(@PathVariable @Positive(message = "شناسه فیلم باید مثبت باشد") Long id) {
        return ApiResponse.of(200, "Movie", movies.get(id));
    }

    @GetMapping("/{id}/available-seats")
    @Operation(summary = "List free seats", description = "Seat numbers run from 1 through hall capacity; only ACTIVE tickets occupy seats.")
    public ApiResponse<AvailableSeatsResponse> availableSeats(@PathVariable @Positive(message = "شناسه فیلم باید مثبت باشد") Long id) {
        return ApiResponse.of(200, "Available seats", movies.availableSeats(id));
    }
}
