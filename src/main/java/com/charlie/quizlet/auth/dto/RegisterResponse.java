package com.charlie.quizlet.auth.dto;

import com.charlie.quizlet.user.UserResponse;

/**
 * @param session token của phiên đăng nhập; {@code null} khi tài khoản chờ Admin duyệt (Teacher) — FE báo chờ duyệt
 *                thay vì tự đăng nhập
 */
public record RegisterResponse(UserResponse user, AuthResponse session) {
}
