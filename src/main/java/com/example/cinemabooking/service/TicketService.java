package com.example.cinemabooking.service;

import com.example.cinemabooking.dto.request.BookTicketRequest;
import com.example.cinemabooking.dto.response.MovieSalesStatsResponse;
import com.example.cinemabooking.dto.response.SalesStatsResponse;
import com.example.cinemabooking.dto.response.TicketResponse;
import com.example.cinemabooking.entity.Movie;
import com.example.cinemabooking.entity.Ticket;
import com.example.cinemabooking.entity.TicketStatus;
import com.example.cinemabooking.entity.User;
import com.example.cinemabooking.exception.SeatAlreadyBookedException;
import com.example.cinemabooking.repository.MovieRepository;
import com.example.cinemabooking.repository.TicketRepository;
import com.example.cinemabooking.repository.UserRepository;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TicketService {
    private final TicketRepository tickets;
    private final MovieRepository movies;
    private final UserRepository users;
    private final JdbcTemplate jdbc;
    private final List<BookingRule> rules;

    public TicketService(TicketRepository tickets, MovieRepository movies, UserRepository users, JdbcTemplate jdbc) {
        this.tickets = tickets;
        this.movies = movies;
        this.users = users;
        this.jdbc = jdbc;
        BookingRule capacity = (movie, seat) -> {
            if (seat < 1 || seat > movie.getCinemaHall().getCapacity())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "شماره صندلی باید بین ۱ و ظرفیت سالن باشد");
        };
        BookingRule available = (movie, seat) -> {
            if (tickets.existsByMovieIdAndSeatNumberAndStatus(movie.getId(), seat, TicketStatus.ACTIVE))
                throw new SeatAlreadyBookedException(seat);
        };
        this.rules = List.of(capacity, available);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TicketResponse book(String username, BookTicketRequest request) {
        User buyer = findUser(username);
        Movie movie = movies.findForBooking(request.movieId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "فیلم پیدا نشد"));
        rules.forEach(rule -> rule.validate(movie, request.seatNumber()));
        Ticket ticket = new Ticket();
        ticket.setUser(buyer);
        ticket.setMovie(movie);
        ticket.setSeatNumber(request.seatNumber());
        ticket.setPurchaseDate(LocalDateTime.now());
        ticket.setPrice(movie.getPrice());
        ticket.setStatus(TicketStatus.ACTIVE);
        try {
            tickets.saveAndFlush(ticket);
        } catch (DataIntegrityViolationException ex) {
            String details = String.valueOf(ex.getMostSpecificCause().getMessage()).toLowerCase(Locale.ROOT);
            if (details.contains("uk_movie_active_seat")) throw new SeatAlreadyBookedException(request.seatNumber());
            throw ex;
        }
        return new TicketResponse(ticket.getId(), movie.getId(), movie.getTitle(), ticket.getSeatNumber(),
                ticket.getPurchaseDate(), ticket.getPrice(), ticket.getStatus(), ticket.getCancelledAt());
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> myTickets(String username) {
        Long userId = findUser(username).getId();
        // Native join deliberately includes soft-deleted movies so old purchases remain visible.
        return jdbc.query("select t.id, t.movie_id, m.title, t.seat_number, t.purchase_date, "
                        + "t.price, t.status, t.cancelled_at from tickets t join movies m on m.id = t.movie_id "
                        + "where t.user_id = ? and t.deleted_at is null order by t.purchase_date desc, t.id desc",
                (rs, row) -> {
                    Timestamp cancelled = rs.getTimestamp("cancelled_at");
                    return new TicketResponse(rs.getLong("id"), rs.getLong("movie_id"), rs.getString("title"),
                            rs.getInt("seat_number"), rs.getTimestamp("purchase_date").toLocalDateTime(),
                            rs.getLong("price"), TicketStatus.valueOf(rs.getString("status")),
                            cancelled == null ? null : cancelled.toLocalDateTime());
                }, userId);
    }

    @Transactional
    public void cancel(String username, Long ticketId) {
        User user = findUser(username);
        Ticket ticket = tickets.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "بلیط پیدا نشد"));
        if (!ticket.getUser().getId().equals(user.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "فقط خریدار می‌تواند بلیط خود را لغو کند");
        if (ticket.getStatus() != TicketStatus.ACTIVE)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "این بلیط قبلاً لغو شده است");
        ticket.setStatus(TicketStatus.CANCELLED);
        ticket.setCancelledAt(LocalDateTime.now());
        tickets.saveAndFlush(ticket);
    }

    @Transactional(readOnly = true)
    public SalesStatsResponse salesStats() {
        // Deleted movies are retained in the report; cancelled/deleted tickets are not sales.
        List<MovieSalesStatsResponse> perMovie = jdbc.query("select m.id, m.title, count(t.id) as ticket_count, "
                        + "coalesce(sum(t.price), 0) as revenue from movies m left join tickets t on t.movie_id = m.id "
                        + "and t.status = 'ACTIVE' and t.deleted_at is null group by m.id, m.title order by m.id",
                (rs, row) -> new MovieSalesStatsResponse(rs.getLong("id"), rs.getString("title"),
                        rs.getLong("ticket_count"), rs.getLong("revenue")));
        return new SalesStatsResponse(perMovie.stream().mapToLong(MovieSalesStatsResponse::activeTicketCount).sum(),
                perMovie.stream().mapToLong(MovieSalesStatsResponse::totalRevenue).sum(), perMovie);
    }

    private User findUser(String username) {
        return users.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "کاربر پیدا نشد؛ دوباره وارد شوید"));
    }
}
