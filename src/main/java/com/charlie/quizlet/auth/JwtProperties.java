package com.charlie.quizlet.auth;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, String issuer, Duration expiration) {

    public JwtProperties {
        // HS256 requires a key of at least 256 bits.
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("app.jwt.secret must be at least 32 bytes");
        }
    }
}
