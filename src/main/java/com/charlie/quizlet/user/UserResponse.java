package com.charlie.quizlet.user;

import java.time.Instant;

public record UserResponse(Long id, String email, String fullName, Role role, UserStatus status, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole(),
                user.getStatus(), user.getCreatedAt());
    }
}
