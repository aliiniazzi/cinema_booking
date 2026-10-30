package com.example.cinemabooking.dto.response;

import com.example.cinemabooking.entity.Role;

public record UserResponse(Long id, String username, String email, Role role) {}
