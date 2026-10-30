package com.example.cinemabooking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cinema_halls")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
public class CinemaHall extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private Integer capacity;

    @OneToMany(mappedBy = "cinemaHall", fetch = FetchType.LAZY)
    @Setter(AccessLevel.NONE)
    private List<Movie> movies = new ArrayList<>();
}
