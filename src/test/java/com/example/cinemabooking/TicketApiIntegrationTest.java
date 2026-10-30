package com.example.cinemabooking;

import com.example.cinemabooking.entity.Role;
import com.example.cinemabooking.entity.User;
import com.example.cinemabooking.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TicketApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;

    @Test
    void bookingCancelRebookAndStatsReflectActiveTickets() throws Exception {
        String request = "{\"movieId\":1,\"seatNumber\":50}";
        MvcResult booked = mvc.perform(post("/api/tickets/book").with(user("user1").roles("USER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.price").value(120000))
                .andExpect(jsonPath("$.data.status").value("ACTIVE")).andReturn();
        long id = json.readTree(booked.getResponse().getContentAsString()).path("data").path("id").asLong();
        mvc.perform(post("/api/tickets/book").with(user("user1").roles("USER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message", containsString("قبلاً فروخته")));
        mvc.perform(get("/api/admin/tickets/stats").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalTickets").value(2))
                .andExpect(jsonPath("$.data.totalRevenue").value(240000));
        mvc.perform(delete("/api/tickets/" + id).with(user("user1").roles("USER")).with(csrf()))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/tickets/" + id).with(user("user1").roles("USER")).with(csrf()))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/tickets/my-tickets").with(user("user1").roles("USER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].status").value("CANCELLED"))
                .andExpect(jsonPath("$.data[0].cancelledAt").exists());
        mvc.perform(get("/api/admin/tickets/stats").with(user("admin").roles("ADMIN")))
                .andExpect(jsonPath("$.data.totalTickets").value(1)).andExpect(jsonPath("$.data.totalRevenue").value(120000));
        mvc.perform(post("/api/tickets/book").with(user("user1").roles("USER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(request)).andExpect(status().isCreated());
    }

    @Test
    void validatesCapacityRolesOwnershipAndCsrf() throws Exception {
        User other = new User(); other.setUsername("otheruser"); other.setEmail("other@example.com");
        other.setPassword("unused"); other.setRole(Role.USER); users.saveAndFlush(other);
        mvc.perform(post("/api/tickets/book").with(user("user1").roles("USER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"movieId\":1,\"seatNumber\":51}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message", containsString("ظرفیت سالن")));
        mvc.perform(post("/api/tickets/book").with(user("user1").roles("USER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"movieId\":1,\"seatNumber\":0}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message", containsString("مثبت")));
        mvc.perform(post("/api/tickets/book").with(user("user1").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"movieId\":1,\"seatNumber\":2}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/tickets/my-tickets")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/tickets/my-tickets").with(user("admin").roles("ADMIN"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/tickets/stats").with(user("user1").roles("USER"))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/tickets/1").with(user("otheruser").roles("USER")).with(csrf()))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.message", containsString("فقط خریدار")));
    }

    @Test
    void historyAndStatisticsKeepTicketsOfDeletedMovie() throws Exception {
        mvc.perform(delete("/api/admin/movies/1").with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/tickets/my-tickets").with(user("user1").roles("USER")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].movieTitle").value("The Shawshank Redemption"));
        mvc.perform(get("/api/admin/tickets/stats").with(user("admin").roles("ADMIN")))
                .andExpect(jsonPath("$.data.totalTickets").value(1));
        mvc.perform(delete("/api/tickets/1").with(user("user1").roles("USER")).with(csrf()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/admin/tickets/stats").with(user("admin").roles("ADMIN")))
                .andExpect(jsonPath("$.data.totalTickets").value(0));
    }

    @Test
    void swaggerDocumentsTicketsAndStats() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/tickets/book']").exists())
                .andExpect(jsonPath("$.paths['/api/admin/tickets/stats']").exists());
    }
}
