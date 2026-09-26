package com.charlie.quizlet.auth.dto;

import com.charlie.quizlet.user.UserResponse;

public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {
}
