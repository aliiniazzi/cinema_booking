package com.example.cinemabooking.service;

import com.example.cinemabooking.dto.request.RegisterRequest;
import com.example.cinemabooking.dto.response.UserResponse;
import com.example.cinemabooking.entity.Role;
import com.example.cinemabooking.entity.User;
import com.example.cinemabooking.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwords;

    public AuthService(UserRepository users, PasswordEncoder passwords) {
        this.users = users;
        this.passwords = passwords;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "رمز عبور نباید بیشتر از ۷۲ بایت UTF-8 باشد");
        }
        if (users.usernameExistsIncludingDeleted(request.username()) || users.emailExistsIncludingDeleted(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "نام کاربری یا ایمیل قبلاً ثبت شده است");
        }
        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwords.encode(request.password()));
        user.setRole(Role.USER);
        return toResponse(users.saveAndFlush(user));
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(String username) {
        return users.findByUsername(username).map(AuthService::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "کاربر وجود ندارد؛ دوباره وارد شوید"));
    }

    private static UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }
}
