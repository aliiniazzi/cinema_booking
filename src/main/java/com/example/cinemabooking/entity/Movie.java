package com.example.cinemabooking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "movies")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
public class Movie extends BaseEntity {
    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String genre;

    @Column(nullable = false)
    private Integer durationMinutes;

    @Column(nullable = false)
    private LocalDate releaseDate;

    @Column(nullable = false, length = 2000)
    private String description;

    // Ticket prices are stored in tomans.
    @Column(nullable = false)
    private Long price;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cinema_hall_id", nullable = false)
    private CinemaHall cinemaHall;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "movie", fetch = FetchType.LAZY)
    @Setter(AccessLevel.NONE)
    private List<Ticket> tickets = new ArrayList<>();
}
