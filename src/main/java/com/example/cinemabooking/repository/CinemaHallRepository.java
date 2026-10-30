package com.example.cinemabooking.repository;

import com.example.cinemabooking.entity.CinemaHall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface CinemaHallRepository extends JpaRepository<CinemaHall, Long> {
    Optional<CinemaHall> findByName(String name);

    @Query(value = "SELECT COUNT(*) FROM cinema_halls WHERE name = :name", nativeQuery = true)
    long countNameIncludingDeleted(@Param("name") String name);
    default boolean nameExistsIncludingDeleted(String name) { return countNameIncludingDeleted(name) > 0; }
}
