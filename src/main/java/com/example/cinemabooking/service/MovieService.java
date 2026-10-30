package com.example.cinemabooking.service;

import com.example.cinemabooking.dto.request.MovieRequest;
import com.example.cinemabooking.dto.response.AvailableSeatsResponse;
import com.example.cinemabooking.dto.response.CinemaHallResponse;
import com.example.cinemabooking.dto.response.MovieResponse;
import com.example.cinemabooking.entity.CinemaHall;
import com.example.cinemabooking.entity.Movie;
import com.example.cinemabooking.entity.TicketStatus;
import com.example.cinemabooking.repository.CinemaHallRepository;
import com.example.cinemabooking.repository.MovieRepository;
import com.example.cinemabooking.repository.TicketRepository;
import java.util.Comparator;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class MovieService {
    private final MovieRepository movies;
    private final CinemaHallRepository halls;
    private final TicketRepository tickets;

    public MovieService(MovieRepository movies, CinemaHallRepository halls, TicketRepository tickets) {
        this.movies = movies;
        this.halls = halls;
        this.tickets = tickets;
    }

    public java.util.List<MovieResponse> list(String title) {
        String query = title == null ? "" : title.trim().toLowerCase(Locale.ROOT);
        Predicate<Movie> titleMatches = movie -> movie.getTitle().toLowerCase(Locale.ROOT).contains(query);
        Comparator<Movie> byTitle = Comparator.comparing(movie -> movie.getTitle().toLowerCase(Locale.ROOT));
        return movies.findAll().stream().filter(Movie::isActive).filter(titleMatches)
                .sorted(byTitle.thenComparing(Movie::getId)).map(MovieService::toResponse).toList();
    }

    public MovieResponse get(Long id) { return toResponse(findMovie(id)); }

    public AvailableSeatsResponse availableSeats(Long id) {
        Movie movie = findMovie(id);
        Set<Integer> booked = Set.copyOf(tickets.findSeatNumbersByMovieIdAndStatus(id, TicketStatus.ACTIVE));
        return new AvailableSeatsResponse(id, IntStream.rangeClosed(1, movie.getCinemaHall().getCapacity())
                .filter(seat -> !booked.contains(seat)).boxed().toList());
    }

    @Transactional
    public MovieResponse create(MovieRequest request) {
        Movie movie = new Movie();
        apply(movie, request);
        return toResponse(movies.saveAndFlush(movie));
    }

    @Transactional
    public MovieResponse update(Long id, MovieRequest request) {
        Movie movie = findMovie(id);
        CinemaHall hall = findHall(request.cinemaHallId());
        boolean seatOutsideHall = tickets.findSeatNumbersByMovieIdAndStatus(id, TicketStatus.ACTIVE).stream()
                .anyMatch(seat -> seat > hall.getCapacity());
        if (seatOutsideHall) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "ظرفیت سالن جدید برای صندلی‌های فروخته‌شده کافی نیست");
        }
        applyFields(movie, request, hall);
        return toResponse(movies.saveAndFlush(movie));
    }

    @Transactional
    public void delete(Long id) {
        Movie movie = findMovie(id);
        movie.setActive(false);
        movie.markDeleted();
        movies.saveAndFlush(movie);
    }

    private Movie findMovie(Long id) {
        return movies.findById(id).filter(Movie::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "فیلم پیدا نشد"));
    }

    private CinemaHall findHall(Long id) {
        return halls.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "سالن سینما پیدا نشد"));
    }

    private void apply(Movie movie, MovieRequest request) {
        applyFields(movie, request, findHall(request.cinemaHallId()));
    }

    private void applyFields(Movie movie, MovieRequest request, CinemaHall hall) {
        movie.setTitle(request.title().trim());
        movie.setGenre(request.genre().trim());
        movie.setDurationMinutes(request.durationMinutes());
        movie.setReleaseDate(request.releaseDate());
        movie.setDescription(request.description().trim());
        movie.setPrice(request.price());
        movie.setCinemaHall(hall);
    }

    private static MovieResponse toResponse(Movie movie) {
        CinemaHall hall = movie.getCinemaHall();
        return new MovieResponse(movie.getId(), movie.getTitle(), movie.getGenre(), movie.getDurationMinutes(),
                movie.getReleaseDate(), movie.getDescription(), movie.getPrice(),
                new CinemaHallResponse(hall.getId(), hall.getName(), hall.getCapacity()));
    }
}
