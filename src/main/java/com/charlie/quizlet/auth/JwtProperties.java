package com.charlie.quizlet.auth;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Token đăng nhập (prefix {@code app.jwt}).
 *
 * @param expiration        thời hạn access token (JWT) — để ngắn, vì JWT không thu hồi được trước hạn
 * @param refreshExpiration thời hạn refresh token (chuỗi ngẫu nhiên lưu DB, thu hồi được). Mỗi lần làm mới
 *                          được token mới với thời hạn tính lại từ đầu, nên người dùng còn hoạt động thì
 *                          không phải đăng nhập lại
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, String issuer, Duration expiration, Duration refreshExpiration) {

    public JwtProperties {
        // HS256 cần khoá tối thiểu 256 bit.
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("app.jwt.secret must be at least 32 bytes");
        }
        Objects.requireNonNull(expiration, "app.jwt.expiration is required");
        Objects.requireNonNull(refreshExpiration, "app.jwt.refresh-expiration is required");
    }
}
