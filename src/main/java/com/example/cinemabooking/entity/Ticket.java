package com.example.cinemabooking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import java.time.LocalDateTime;

@Entity
@Table(name = "tickets", indexes = @Index(name = "idx_ticket_movie_seat_status", columnList = "movie_id, seat_number, status"),
        uniqueConstraints = @UniqueConstraint(name = "uk_movie_active_seat", columnNames = {"movie_id", "active_seat_number"}))
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
public class Ticket extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @Column(nullable = false)
    private Integer seatNumber;

    // NULL for cancelled or deleted tickets, allowing historical rows to coexist.
    @Column(name = "active_seat_number", insertable = false, updatable = false,
            columnDefinition = "integer generated always as (case when status = 'ACTIVE' and deleted_at is null then seat_number else null end)")
    private Integer activeSeatNumber;

    @Column(nullable = false)
    private LocalDateTime purchaseDate;

    // The purchase price is retained even if the movie price changes later.
    @Column(nullable = false)
    private Long price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TicketStatus status = TicketStatus.ACTIVE;

    private LocalDateTime cancelledAt;
}
