package com.example.cinemabooking.service;

import com.example.cinemabooking.entity.Movie;

/** A composable business rule that rejects an invalid booking by throwing an exception. */
@FunctionalInterface
public interface BookingRule {
    void validate(Movie movie, int seatNumber);
}
