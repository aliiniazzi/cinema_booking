package com.example.cinemabooking;

import com.example.cinemabooking.entity.CinemaHall;
import com.example.cinemabooking.repository.CinemaHallRepository;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuditingIntegrationTest {
    @Autowired CinemaHallRepository halls;
    @Autowired EntityManager entityManager;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recordsAuthenticatedCreatorUpdaterAndDeleterAndIncrementsVersion() {
        authenticate("creator");
        CinemaHall hall = new CinemaHall();
        hall.setName("test-" + UUID.randomUUID());
        hall.setCapacity(20);
        halls.saveAndFlush(hall);
        assertThat(hall.getCreatedAt()).isNotNull();
        assertThat(hall.getCreatedBy()).isEqualTo("creator");
        Long initialVersion = hall.getVersion();

        authenticate("editor");
        hall.setCapacity(25);
        halls.flush();
        assertThat(hall.getUpdatedAt()).isNotNull();
        assertThat(hall.getUpdatedBy()).isEqualTo("editor");
        assertThat(hall.getVersion()).isGreaterThan(initialVersion);

        authenticate("deleter");
        hall.markDeleted();
        halls.flush();
        Long id = hall.getId();
        entityManager.clear();
        assertThat(halls.findById(id)).isEmpty();
        Object[] persisted = (Object[]) entityManager.createNativeQuery(
                "select deleted_at, deleted_by, updated_by from cinema_halls where id = :id")
                .setParameter("id", id).getSingleResult();
        assertThat(persisted[0]).isNotNull();
        assertThat(persisted[1]).isEqualTo("deleter");
        assertThat(persisted[2]).isEqualTo("deleter");
    }

    @Test
    void recordsSystemWhenThereIsNoAuthenticatedUser() {
        CinemaHall hall = new CinemaHall();
        hall.setName("test-" + UUID.randomUUID());
        hall.setCapacity(10);
        halls.saveAndFlush(hall);
        assertThat(hall.getCreatedBy()).isEqualTo("system");
    }

    private void authenticate(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(username, null, java.util.List.of()));
    }
}
