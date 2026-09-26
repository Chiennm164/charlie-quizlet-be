package com.charlie.quizlet.auth.dto;

import com.charlie.quizlet.user.UserResponse;

/**
 * @param expiresIn    thời hạn access token (giây)
 * @param refreshToken dùng 1 lần với {@code POST /api/auth/refresh} để lấy cặp token mới
 */
public record AuthResponse(String accessToken, String tokenType, long expiresIn, String refreshToken,
        UserResponse user) {
}
