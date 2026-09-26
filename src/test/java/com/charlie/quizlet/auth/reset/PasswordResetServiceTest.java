package com.charlie.quizlet.auth.reset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.charlie.quizlet.auth.RefreshTokenRepository;
import com.charlie.quizlet.common.error.BusinessException;
import com.charlie.quizlet.config.AppProperties;
import com.charlie.quizlet.user.User;
import com.charlie.quizlet.user.UserRepository;
import com.charlie.quizlet.user.UserStatus;

class PasswordResetServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordResetTokenRepository tokenRepository = mock(PasswordResetTokenRepository.class);
    private final RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
    private final PasswordResetNotifier notifier = mock(PasswordResetNotifier.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

    private PasswordResetService service;
    private User alice;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties(
                new AppProperties.Frontend("http://localhost:4200/", "/reset-password"), null,
                new AppProperties.PasswordReset(Duration.ofMinutes(30)), null);
        service = new PasswordResetService(userRepository, tokenRepository, refreshTokenRepository, notifier,
                passwordEncoder, Clock.fixed(NOW, ZoneOffset.UTC), props);
        alice = new User();
        alice.setId(42L);
        alice.setEmail("alice@example.com");
        alice.setStatus(UserStatus.ACTIVE);
    }

    @Test
    void requestResetStoresHashedTokenAndSendsLink() {
        given(userRepository.findByEmail("alice@example.com")).willReturn(Optional.of(alice));

        service.requestReset("  Alice@Example.com ");

        ArgumentCaptor<PasswordResetToken> saved = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(saved.capture());
        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(notifier).sendResetLink(any(User.class), link.capture());

        String rawToken = link.getValue().substring("http://localhost:4200/reset-password?token=".length());
        assertThat(link.getValue()).startsWith("http://localhost:4200/reset-password?token=");
        assertThat(saved.getValue().getTokenHash()).hasSize(64).isNotEqualTo(rawToken);
        assertThat(saved.getValue().getExpiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(30)));
        verify(tokenRepository).invalidateAllForUser(42L, NOW);
    }

    @Test
    void requestResetForUnknownOrInactiveUserDoesNothing() {
        given(userRepository.findByEmail("ghost@example.com")).willReturn(Optional.empty());
        service.requestReset("ghost@example.com");

        alice.setStatus(UserStatus.LOCKED);
        given(userRepository.findByEmail("alice@example.com")).willReturn(Optional.of(alice));
        service.requestReset("alice@example.com");

        verify(tokenRepository, never()).save(any());
        verify(notifier, never()).sendResetLink(any(), anyString());
    }

    @Test
    void resetPasswordUpdatesHashInvalidatesTokensAndLogsOutEverywhere() {
        given(tokenRepository.findByTokenHash(anyString())).willReturn(Optional.of(token(NOW.plusSeconds(60), null)));
        given(passwordEncoder.encode("newsecret123")).willReturn("hashed");

        service.resetPassword("raw", "newsecret123");

        assertThat(alice.getPasswordHash()).isEqualTo("hashed");
        verify(tokenRepository).invalidateAllForUser(42L, NOW);
        verify(refreshTokenRepository).revokeAllForUser(42L, NOW);
    }

    @Test
    void resetPasswordRejectsExpiredOrUsedToken() {
        given(tokenRepository.findByTokenHash(anyString())).willReturn(Optional.of(token(NOW.minusSeconds(1), null)));
        assertThatThrownBy(() -> service.resetPassword("raw", "newsecret123"))
                .isInstanceOf(BusinessException.class);

        given(tokenRepository.findByTokenHash(anyString())).willReturn(Optional.of(token(NOW.plusSeconds(60), NOW)));
        assertThatThrownBy(() -> service.resetPassword("raw", "newsecret123"))
                .isInstanceOf(BusinessException.class);

        verify(passwordEncoder, never()).encode(anyString());
        verify(refreshTokenRepository, never()).revokeAllForUser(any(), any());
    }

    private PasswordResetToken token(Instant expiresAt, Instant usedAt) {
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(alice);
        token.setExpiresAt(expiresAt);
        token.setUsedAt(usedAt);
        return token;
    }
}
