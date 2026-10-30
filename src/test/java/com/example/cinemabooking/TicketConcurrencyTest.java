package com.example.cinemabooking;

import com.example.cinemabooking.dto.request.BookTicketRequest;
import com.example.cinemabooking.entity.CinemaHall;
import com.example.cinemabooking.entity.Movie;
import com.example.cinemabooking.entity.Role;
import com.example.cinemabooking.entity.User;
import com.example.cinemabooking.exception.SeatAlreadyBookedException;
import com.example.cinemabooking.repository.CinemaHallRepository;
import com.example.cinemabooking.repository.MovieRepository;
import com.example.cinemabooking.repository.UserRepository;
import com.example.cinemabooking.service.TicketService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
class TicketConcurrencyTest {
    @Autowired TicketService service;
    @Autowired MovieRepository movies;
    @Autowired CinemaHallRepository halls;
    @Autowired UserRepository users;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    void twoConcurrentTransactionsForOneSeatYieldOnlyOneSale() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String first = "a" + suffix;
        String second = "b" + suffix;
        long[] ids = tx.execute(status -> {
            CinemaHall hall = halls.findByName("Hall 1").orElseThrow();
            Movie movie = new Movie(); movie.setTitle("Concurrency " + suffix); movie.setGenre("Drama");
            movie.setDurationMinutes(100); movie.setReleaseDate(LocalDate.of(2025, 1, 1));
            movie.setDescription("Test"); movie.setPrice(100000L); movie.setCinemaHall(hall);
            movies.saveAndFlush(movie);
            User a = createUser(first); User b = createUser(second);
            return new long[]{movie.getId(), a.getId(), b.getId()};
        });
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CyclicBarrier bothObservedFree = new CyclicBarrier(2);
        try {
            Callable<Object> makeA = bookingAttempt(tx, bothObservedFree, first, ids[0]);
            Callable<Object> makeB = bookingAttempt(tx, bothObservedFree, second, ids[0]);
            Future<Object> a = executor.submit(makeA);
            Future<Object> b = executor.submit(makeB);
            List<Object> outcomes = List.of(a.get(60, TimeUnit.SECONDS), b.get(60, TimeUnit.SECONDS));
            assertThat(outcomes.stream().filter(value -> value instanceof Long).count()).isEqualTo(1);
            assertThat(outcomes.stream().filter(value -> value instanceof SeatAlreadyBookedException).count()).isEqualTo(1);
            assertThat(jdbc.queryForObject("select count(*) from tickets where movie_id=? and seat_number=12 and status='ACTIVE' and deleted_at is null", Integer.class, ids[0])).isEqualTo(1);
            assertThatThrownBy(() -> jdbc.update("insert into tickets (movie_id,user_id,seat_number,purchase_date,price,status,version) values (?,?,12,current_timestamp,100000,'ACTIVE',0)", ids[0], ids[2]))
                    .isInstanceOf(DataIntegrityViolationException.class);
            System.out.println("CONCURRENCY RESULT: 2 attempts, 1 successful booking, 1 rejected, 1 active ticket; duplicate DB insert rejected.");
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(30, TimeUnit.SECONDS);
            tx.executeWithoutResult(status -> {
                jdbc.update("delete from tickets where movie_id = ?", ids[0]);
                jdbc.update("delete from movies where id = ?", ids[0]);
                jdbc.update("delete from users where id in (?,?)", ids[1], ids[2]);
            });
        }
    }

    private Callable<Object> bookingAttempt(TransactionTemplate tx, CyclicBarrier barrier, String username, long movieId) {
        return () -> {
            try {
                return tx.execute(status -> {
                    Integer before = jdbc.queryForObject("select count(*) from tickets where movie_id=? and seat_number=12 and status='ACTIVE'", Integer.class, movieId);
                    if (before == null || before != 0) throw new AssertionError("Seat was not initially free");
                    try { barrier.await(30, TimeUnit.SECONDS); }
                    catch (Exception ex) { throw new IllegalStateException(ex); }
                    return service.book(username, new BookTicketRequest(movieId, 12)).id();
                });
            } catch (RuntimeException ex) {
                return ex;
            }
        };
    }

    private User createUser(String username) {
        User user = new User(); user.setUsername(username); user.setEmail(username + "@example.com");
        user.setPassword("unused"); user.setRole(Role.USER);
        return users.saveAndFlush(user);
    }
}
