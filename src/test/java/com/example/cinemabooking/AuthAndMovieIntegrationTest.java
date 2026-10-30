package com.example.cinemabooking;

import com.example.cinemabooking.entity.User;
import com.example.cinemabooking.repository.UserRepository;
import com.example.cinemabooking.config.DataLoader;
import com.example.cinemabooking.repository.MovieRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import jakarta.persistence.EntityManager;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthAndMovieIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwords;
    @Autowired DataLoader.SeedService seedService;
    @Autowired MovieRepository movies;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;

    @Test
    void registerIsValidatedHasUserRoleAndHashesPassword() throws Exception {
        String username = "u" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String body = "{\"username\":\"" + username + "\",\"email\":\"" + username
                + "@example.com\",\"password\":\"password123\"}";
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
        User user = users.findByUsername(username).orElseThrow();
        assertThat(user.getPassword()).isNotEqualTo("password123");
        assertThat(passwords.matches("password123", user.getPassword())).isTrue();
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("CONFLICT"));
        mvc.perform(post("/api/auth/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"ab\",\"email\":\"bad\",\"password\":\"1\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.path").value("/api/auth/register"));
    }

    @Test
    void realCsrfCookieSessionLoginMeLogoutFlow() throws Exception {
        MvcResult tokenResult = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        Cookie token = tokenResult.getResponse().getCookie("XSRF-TOKEN");
        assertThat(token).isNotNull();
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"user1\",\"password\":\"user123\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("FORBIDDEN"));
        mvc.perform(post("/api/auth/login").cookie(token).header("X-XSRF-TOKEN", token.getValue())
                .contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"user1\",\"password\":\"wrongpass\"}"))
                .andExpect(status().isUnauthorized());
        MvcResult login = mvc.perform(post("/api/auth/login").cookie(token).header("X-XSRF-TOKEN", token.getValue())
                .contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"user1\",\"password\":\"user123\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.username").value("user1")).andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(session).isNotNull();
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("user1"));
        MvcResult refreshed = mvc.perform(get("/api/auth/csrf").session(session))
                .andExpect(status().isOk()).andReturn();
        Cookie newToken = refreshed.getResponse().getCookie("XSRF-TOKEN");
        assertThat(newToken).isNotNull();
        mvc.perform(post("/api/auth/logout").session(session).cookie(newToken)
                .header("X-XSRF-TOKEN", newToken.getValue())).andExpect(status().isOk());
        assertThat(session.isInvalid()).isTrue();
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void rolesPublicReadsAndAdminCrudAndSoftDelete() throws Exception {
        mvc.perform(get("/api/movies")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3));
        mvc.perform(get("/api/movies/search").param("title", "INTER"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].title").value("Interstellar"));
        mvc.perform(get("/api/movies/search")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
        mvc.perform(get("/api/movies/not-a-number")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.path").value("/api/movies/not-a-number"));
        mvc.perform(get("/api/movies/1/available-seats")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.availableSeats.length()").value(49))
                .andExpect(jsonPath("$.data.availableSeats[0]").value(2));
        String movie = "{\"title\":\"Integration Test Movie\",\"genre\":\"Drama\",\"durationMinutes\":120,"
                + "\"releaseDate\":\"2025-01-01\",\"description\":\"Sample\",\"price\":100000,\"cinemaHallId\":1}";
        mvc.perform(post("/api/admin/movies").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(movie))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/admin/movies").with(user("user1").roles("USER")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(movie)).andExpect(status().isForbidden());
        MvcResult created = mvc.perform(post("/api/admin/movies").with(user("admin").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(movie)).andExpect(status().isCreated()).andReturn();
        long id = mapper.readTree(created.getResponse().getContentAsString()).path("data").path("id").asLong();
        mvc.perform(put("/api/admin/movies/" + id).with(user("admin").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(movie.replace("Integration Test Movie", "Updated Film")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.title").value("Updated Film"));
        mvc.perform(delete("/api/admin/movies/" + id).with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/movies/" + id)).andExpect(status().isNotFound());
        mvc.perform(get("/api/movies/search").param("title", "Updated Film"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void openApiIsPublic() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/auth/login']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/csrf'].get.parameters").doesNotExist())
                .andExpect(jsonPath("$.paths['/api/admin/movies']").exists());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @Test
    void deletedMovieIsHiddenButTicketHistoryRemainsAndSeederDoesNotRestoreIt() throws Exception {
        mvc.perform(delete("/api/admin/movies/1").with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isOk());
        entityManager.flush();
        entityManager.clear();
        assertThat(movies.findById(1L)).isEmpty();
        assertThat(jdbc.queryForObject("select count(*) from tickets where movie_id=1", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select deleted_by from movies where id=1", String.class)).isEqualTo("admin");
        seedService.seed();
        assertThat(jdbc.queryForObject("select count(*) from movies", Integer.class)).isEqualTo(3);
        assertThat(movies.findById(1L)).isEmpty();
    }

    @Test
    void adminSessionCanCreateMovieAndAuditingUsesLoggedInUser() throws Exception {
        MvcResult csrf = mvc.perform(get("/api/auth/csrf")).andReturn();
        Cookie token = csrf.getResponse().getCookie("XSRF-TOKEN");
        MvcResult login = mvc.perform(post("/api/auth/login").cookie(token).header("X-XSRF-TOKEN", token.getValue())
                .contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk()).andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        Cookie afterLogin = mvc.perform(get("/api/auth/csrf").session(session)).andReturn().getResponse().getCookie("XSRF-TOKEN");
        String body = "{\"title\":\"Admin Session Movie\",\"genre\":\"Drama\",\"durationMinutes\":100,"
                + "\"releaseDate\":\"2025-01-01\",\"description\":\"Sample\",\"price\":100000,\"cinemaHallId\":1}";
        MvcResult result = mvc.perform(post("/api/admin/movies").session(session).cookie(afterLogin)
                        .header("X-XSRF-TOKEN", afterLogin.getValue()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        long id = mapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
        assertThat(jdbc.queryForObject("select created_by from movies where id=?", String.class, id)).isEqualTo("admin");
    }
}
