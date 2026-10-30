package com.example.cinemabooking.controller;

import com.example.cinemabooking.dto.request.LoginRequest;
import com.example.cinemabooking.dto.request.RegisterRequest;
import com.example.cinemabooking.dto.response.ApiResponse;
import com.example.cinemabooking.dto.response.UserResponse;
import com.example.cinemabooking.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Session login and CSRF protection")
public class AuthController {
    private final AuthService auth;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contexts;
    private final SessionAuthenticationStrategy sessionStrategy;
    private final CsrfTokenRepository csrfRepository;

    public AuthController(AuthService auth, AuthenticationManager authenticationManager,
                          SecurityContextRepository contexts, SessionAuthenticationStrategy sessionStrategy,
                          CsrfTokenRepository csrfRepository) {
        this.auth = auth;
        this.authenticationManager = authenticationManager;
        this.contexts = contexts;
        this.sessionStrategy = sessionStrategy;
        this.csrfRepository = csrfRepository;
    }

    @GetMapping("/csrf")
    @Operation(summary = "Get a CSRF token", description = "Call before the first POST/PUT/DELETE and again after login or logout. Swagger sends the XSRF-TOKEN cookie automatically.")
    public ApiResponse<Map<String, String>> csrf(@Parameter(hidden = true) CsrfToken token) {
        return ApiResponse.of(200, "CSRF token", Map.of("headerName", token.getHeaderName(), "token", token.getToken()));
    }

    @PostMapping("/register")
    @Operation(summary = "Register a USER account")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(201, "Registered", auth.register(request)));
    }

    @PostMapping("/login")
    @Operation(summary = "Log in", description = "Creates a session cookie. Fetch a new CSRF token after login.")
    public ApiResponse<UserResponse> login(@Valid @RequestBody LoginRequest request,
                                         HttpServletRequest servletRequest, HttpServletResponse response) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
            sessionStrategy.onAuthentication(authentication, servletRequest, response);
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            contexts.saveContext(context, servletRequest, response);
            return ApiResponse.of(200, "Logged in", auth.currentUser(authentication.getName()));
        } catch (AuthenticationException ex) {
            SecurityContextHolder.clearContext();
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "نام کاربری یا رمز عبور نادرست است");
        }
    }

    @GetMapping("/me")
    @Operation(summary = "Get the current user")
    public ApiResponse<UserResponse> me(Authentication authentication) {
        return ApiResponse.of(200, "Current user", auth.currentUser(authentication.getName()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Log out", description = "Invalidates the current session and CSRF token.")
    public ApiResponse<Void> logout(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        csrfRepository.saveToken(null, request, response);
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        return ApiResponse.of(200, "Logged out", null);
    }
}
