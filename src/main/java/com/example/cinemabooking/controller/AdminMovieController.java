package com.example.cinemabooking.controller;

import com.example.cinemabooking.dto.request.MovieRequest;
import com.example.cinemabooking.dto.response.ApiResponse;
import com.example.cinemabooking.dto.response.MovieResponse;
import com.example.cinemabooking.service.MovieService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/movies")
@Validated
@Tag(name = "Admin movies", description = "Requires ADMIN session")
public class AdminMovieController {
    private final MovieService movies;
    public AdminMovieController(MovieService movies) { this.movies = movies; }

    @PostMapping
    @Operation(summary = "Create a movie (ADMIN)")
    public ResponseEntity<ApiResponse<MovieResponse>> create(@Valid @RequestBody MovieRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(201, "Movie created", movies.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a movie (ADMIN)")
    public ApiResponse<MovieResponse> update(@PathVariable @Positive(message = "شناسه فیلم باید مثبت باشد") Long id, @Valid @RequestBody MovieRequest request) {
        return ApiResponse.of(200, "Movie updated", movies.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a movie (ADMIN)")
    public ApiResponse<Void> delete(@PathVariable @Positive(message = "شناسه فیلم باید مثبت باشد") Long id) {
        movies.delete(id);
        return ApiResponse.of(200, "Movie deleted", null);
    }
}
