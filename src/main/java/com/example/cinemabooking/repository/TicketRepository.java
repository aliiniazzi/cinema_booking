package com.example.cinemabooking.repository;

import com.example.cinemabooking.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.example.cinemabooking.entity.TicketStatus;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    boolean existsByMovieIdAndUserIdAndSeatNumber(Long movieId, Long userId, Integer seatNumber);

    boolean existsByMovieIdAndSeatNumberAndStatus(Long movieId, Integer seatNumber, TicketStatus status);

    @Query("select t.seatNumber from Ticket t where t.movie.id = :movieId and t.status = :status")
    List<Integer> findSeatNumbersByMovieIdAndStatus(@Param("movieId") Long movieId, @Param("status") TicketStatus status);

    @Query(value = "SELECT COUNT(*) FROM tickets WHERE movie_id = :movieId AND user_id = :userId AND seat_number = :seat", nativeQuery = true)
    long countIncludingDeleted(@Param("movieId") Long movieId, @Param("userId") Long userId, @Param("seat") Integer seat);
    default boolean existsIncludingDeleted(Long movieId, Long userId, Integer seat) { return countIncludingDeleted(movieId, userId, seat) > 0; }
}
