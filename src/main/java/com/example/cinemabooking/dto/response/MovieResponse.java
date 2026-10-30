package com.example.cinemabooking.dto.response;

import java.time.LocalDate;

public record MovieResponse(Long id, String title, String genre, Integer durationMinutes,
                            LocalDate releaseDate, String description, Long price,
                            CinemaHallResponse cinemaHall) {}
