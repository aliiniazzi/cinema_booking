package com.example.cinemabooking.config;

import com.example.cinemabooking.entity.*;
import com.example.cinemabooking.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Configuration
public class DataLoader {
    @Bean
    CommandLineRunner seedData(SeedService seedService) {
        return args -> seedService.seed();
    }

    @Bean
    SeedService seedService(UserRepository users, CinemaHallRepository halls,
                            MovieRepository movies, TicketRepository tickets, JdbcTemplate jdbc) {
        return new SeedService(users, halls, movies, tickets, jdbc);
    }

    public static class SeedService {
        private final UserRepository users;
        private final CinemaHallRepository halls;
        private final MovieRepository movies;
        private final TicketRepository tickets;
        private final JdbcTemplate jdbc;
        private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        public SeedService(UserRepository users, CinemaHallRepository halls,
                           MovieRepository movies, TicketRepository tickets, JdbcTemplate jdbc) {
            this.users = users;
            this.halls = halls;
            this.movies = movies;
            this.tickets = tickets;
            this.jdbc = jdbc;
        }

        @Transactional
        public void seed() {
            // Only seed an entirely empty database, including soft-deleted rows.
            Long existing = jdbc.queryForObject("select (select count(*) from users) + "
                    + "(select count(*) from cinema_halls) + (select count(*) from movies) + "
                    + "(select count(*) from tickets)", Long.class);
            if (existing != null && existing > 0) return;
            createUser("admin", "admin@example.com", "admin123", Role.ADMIN);
            User user = createUser("user1", "user1@example.com", "user123", Role.USER);
            CinemaHall hall = halls.findByName("Hall 1").orElseGet(() -> {
                if (halls.nameExistsIncludingDeleted("Hall 1")) return null;
                CinemaHall newHall = new CinemaHall();
                newHall.setName("Hall 1");
                newHall.setCapacity(50);
                return halls.save(newHall);
            });
            if (hall == null) return;
            Movie first = createMovie("The Shawshank Redemption", "Drama", 142,
                    LocalDate.of(1994, 9, 23), "A story of hope and friendship in prison.", 120000L, hall);
            createMovie("Interstellar", "Science Fiction", 169,
                    LocalDate.of(2014, 11, 7), "Explorers travel beyond Earth to find a future for humanity.", 150000L, hall);
            createMovie("Inception", "Science Fiction", 148,
                    LocalDate.of(2010, 7, 16), "A team enters dreams to plant an idea.", 140000L, hall);
            if (first != null && user != null && !tickets.existsIncludingDeleted(first.getId(), user.getId(), 1)) {
                Ticket ticket = new Ticket();
                ticket.setUser(user);
                ticket.setMovie(first);
                ticket.setSeatNumber(1);
                ticket.setPurchaseDate(LocalDateTime.now());
                ticket.setPrice(first.getPrice());
                ticket.setStatus(TicketStatus.ACTIVE);
                tickets.save(ticket);
            }
        }

        private User createUser(String username, String email, String password, Role role) {
            return users.findByUsername(username).orElseGet(() -> {
                if (users.usernameExistsIncludingDeleted(username) || users.emailExistsIncludingDeleted(email)) return null;
                User user = new User();
                user.setUsername(username);
                user.setEmail(email);
                user.setPassword(encoder.encode(password));
                user.setRole(role);
                return users.save(user);
            });
        }

        private Movie createMovie(String title, String genre, int duration, LocalDate date,
                                  String description, long price, CinemaHall hall) {
            return movies.findFirstByTitle(title).orElseGet(() -> {
                if (movies.titleExistsIncludingDeleted(title)) return null;
                Movie movie = new Movie();
                movie.setTitle(title);
                movie.setGenre(genre);
                movie.setDurationMinutes(duration);
                movie.setReleaseDate(date);
                movie.setDescription(description);
                movie.setPrice(price);
                movie.setCinemaHall(hall);
                return movies.save(movie);
            });
        }
    }
}
