package com.example.cinemabooking.repository;

import com.example.cinemabooking.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    @Query(value = "SELECT COUNT(*) FROM users WHERE LOWER(username) = LOWER(:username)", nativeQuery = true)
    long countUsernameIncludingDeleted(@Param("username") String username);
    default boolean usernameExistsIncludingDeleted(String username) { return countUsernameIncludingDeleted(username) > 0; }

    @Query(value = "SELECT COUNT(*) FROM users WHERE LOWER(email) = LOWER(:email)", nativeQuery = true)
    long countEmailIncludingDeleted(@Param("email") String email);
    default boolean emailExistsIncludingDeleted(String email) { return countEmailIncludingDeleted(email) > 0; }
}
