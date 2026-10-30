package com.example.cinemabooking.repository;

import com.example.cinemabooking.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface MovieRepository extends JpaRepository<Movie, Long> {
    Optional<Movie> findFirstByTitle(String title);

    @Lock(LockModeType.OPTIMISTIC)
    @Query("select m from Movie m where m.id = :id and m.active = true")
    Optional<Movie> findForBooking(@Param("id") Long id);

    @Query(value = "SELECT COUNT(*) FROM movies WHERE title = :title", nativeQuery = true)
    long countTitleIncludingDeleted(@Param("title") String title);
    default boolean titleExistsIncludingDeleted(String title) { return countTitleIncludingDeleted(title) > 0; }
}
