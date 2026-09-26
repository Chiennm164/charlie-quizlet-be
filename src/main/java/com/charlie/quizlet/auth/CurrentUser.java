package com.charlie.quizlet.auth;

import org.springframework.security.oauth2.jwt.Jwt;

import com.charlie.quizlet.user.Role;

/** Người đang gọi API, lấy từ access token (id + role) — không cần đọc lại DB chỉ để kiểm tra quyền. */
public record CurrentUser(Long id, Role role) {

    public static CurrentUser from(Jwt jwt) {
        return new CurrentUser(Long.valueOf(jwt.getSubject()), Role.valueOf(jwt.getClaimAsString(JwtService.ROLE_CLAIM)));
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
