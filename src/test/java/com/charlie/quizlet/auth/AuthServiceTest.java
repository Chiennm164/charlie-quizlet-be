package com.charlie.quizlet.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.charlie.quizlet.auth.dto.AuthResponse;
import com.charlie.quizlet.auth.dto.LoginRequest;
import com.charlie.quizlet.auth.dto.RegisterRequest;
import com.charlie.quizlet.common.error.BusinessException;
import com.charlie.quizlet.common.error.ErrorCode;
import com.charlie.quizlet.user.Role;
import com.charlie.quizlet.user.User;
import com.charlie.quizlet.user.UserRepository;
import com.charlie.quizlet.user.UserStatus;

class AuthServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final Duration REFRESH_TTL = Duration.ofDays(30);
    private static final UUID FAMILY = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final UserRepository userRepository = mock(UserRepository.class);
    private final RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final AuthService service = new AuthService(userRepository, refreshTokenRepository, passwordEncoder,
            jwtService,
            new JwtProperties("test-secret-0123456789abcdef-0123456789", "test", Duration.ofMinutes(15), REFRESH_TTL),
            Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void setUp() {
        given(jwtService.issueAccessToken(any())).willReturn("token");
        given(jwtService.expiresInSeconds()).willReturn(900L);
        given(passwordEncoder.encode("secret123")).willReturn("hashed");
        given(passwordEncoder.matches("secret123", "hashed")).willReturn(true);
    }

    @Test
    void registerCreatesActiveStudentWithNormalizedEmail() {
        AuthResponse res = service.register(new RegisterRequest("  Alice@Example.COM ", "secret123", "  Alice  "));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("alice@example.com");
        assertThat(saved.getValue().getFullName()).isEqualTo("Alice");
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("hashed");
        assertThat(saved.getValue().getRole()).isEqualTo(Role.STUDENT);
        assertThat(saved.getValue().getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(res.accessToken()).isEqualTo("token");
        assertThat(res.tokenType()).isEqualTo(JwtService.TOKEN_TYPE);
        assertThat(savedRefreshToken().getTokenHash()).isEqualTo(OpaqueTokens.hash(res.refreshToken()));
    }

    @Test
    void registerRejectsDuplicateEmail() {
        given(userRepository.existsByEmail("alice@example.com")).willReturn(true);

        assertThatThrownBy(() -> service.register(new RegisterRequest("alice@example.com", "secret123", "Alice")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_EMAIL_ALREADY_REGISTERED);
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void loginWithWrongPasswordOrUnknownEmailFailsWithSameCode() {
        given(userRepository.findByEmail("alice@example.com")).willReturn(Optional.of(user(UserStatus.ACTIVE)));

        assertErrorCode(() -> service.login(new LoginRequest("alice@example.com", "wrong")),
                ErrorCode.AUTH_INVALID_CREDENTIALS);
        assertErrorCode(() -> service.login(new LoginRequest("ghost@example.com", "secret123")),
                ErrorCode.AUTH_INVALID_CREDENTIALS);
    }

    @Test
    void loginRejectsLockedOrPendingAccount() {
        given(userRepository.findByEmail("locked@example.com")).willReturn(Optional.of(user(UserStatus.LOCKED)));
        given(userRepository.findByEmail("pending@example.com")).willReturn(Optional.of(user(UserStatus.PENDING)));

        assertErrorCode(() -> service.login(new LoginRequest("locked@example.com", "secret123")),
                ErrorCode.AUTH_ACCOUNT_LOCKED);
        assertErrorCode(() -> service.login(new LoginRequest("pending@example.com", "secret123")),
                ErrorCode.AUTH_ACCOUNT_PENDING);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void loginStartsSessionWithHashedRefreshToken() {
        given(userRepository.findByEmail("alice@example.com")).willReturn(Optional.of(user(UserStatus.ACTIVE)));

        AuthResponse res = service.login(new LoginRequest(" ALICE@example.com ", "secret123"));

        assertThat(res.accessToken()).isEqualTo("token");
        RefreshToken saved = savedRefreshToken();
        assertThat(saved.getTokenHash()).hasSize(64).isEqualTo(OpaqueTokens.hash(res.refreshToken()));
        assertThat(saved.getExpiresAt()).isEqualTo(NOW.plus(REFRESH_TTL));
        assertThat(saved.getFamilyId()).isNotNull();
    }

    @Test
    void refreshRotatesTokenWithinSameSession() {
        RefreshToken old = storedToken(user(UserStatus.ACTIVE), NOW.plusSeconds(60), null);

        AuthResponse res = service.refresh("old-token");

        assertThat(old.getRevokedAt()).isEqualTo(NOW);
        assertThat(res.accessToken()).isEqualTo("token");
        assertThat(res.refreshToken()).isNotEqualTo("old-token");
        RefreshToken issued = savedRefreshToken();
        assertThat(issued.getFamilyId()).isEqualTo(FAMILY);
        assertThat(issued.getTokenHash()).isEqualTo(OpaqueTokens.hash(res.refreshToken()));
        assertThat(issued.getExpiresAt()).isEqualTo(NOW.plus(REFRESH_TTL));
    }

    @Test
    void refreshRejectsUnknownOrExpiredToken() {
        assertErrorCode(() -> service.refresh("unknown"), ErrorCode.AUTH_REFRESH_TOKEN_INVALID);

        storedToken(user(UserStatus.ACTIVE), NOW, null);
        assertErrorCode(() -> service.refresh("old-token"), ErrorCode.AUTH_REFRESH_TOKEN_INVALID);

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void tokenReusedRightAfterRotationIsRejectedButSessionKept() {
        // 2 tab làm mới gần như cùng lúc bằng cùng 1 token: tab chậm hơn bị từ chối, phiên vẫn giữ.
        storedToken(user(UserStatus.ACTIVE), NOW.plusSeconds(60), NOW.minusSeconds(5));

        assertErrorCode(() -> service.refresh("old-token"), ErrorCode.AUTH_REFRESH_TOKEN_INVALID);
        verify(refreshTokenRepository, never()).revokeFamily(any(), any());
    }

    @Test
    void tokenReusedLaterRevokesWholeSession() {
        storedToken(user(UserStatus.ACTIVE), NOW.plusSeconds(60), NOW.minusSeconds(60));

        assertErrorCode(() -> service.refresh("old-token"), ErrorCode.AUTH_REFRESH_TOKEN_INVALID);
        verify(refreshTokenRepository).revokeFamily(FAMILY, NOW);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void refreshRejectsLockedAccountWithoutRotating() {
        RefreshToken old = storedToken(user(UserStatus.LOCKED), NOW.plusSeconds(60), null);

        assertErrorCode(() -> service.refresh("old-token"), ErrorCode.AUTH_ACCOUNT_LOCKED);
        assertThat(old.getRevokedAt()).isNull();
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void logoutRevokesTokenAndIgnoresUnknownOne() {
        RefreshToken token = storedToken(user(UserStatus.ACTIVE), NOW.plusSeconds(60), null);

        service.logout("old-token");
        service.logout("unknown");

        assertThat(token.getRevokedAt()).isEqualTo(NOW);
    }

    private RefreshToken savedRefreshToken() {
        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(saved.capture());
        return saved.getValue();
    }

    /** Refresh token "old-token" đang có trong DB. */
    private RefreshToken storedToken(User owner, Instant expiresAt, Instant revokedAt) {
        RefreshToken token = new RefreshToken();
        token.setUser(owner);
        token.setFamilyId(FAMILY);
        token.setTokenHash(OpaqueTokens.hash("old-token"));
        token.setExpiresAt(expiresAt);
        token.setRevokedAt(revokedAt);
        given(refreshTokenRepository.findByTokenHash(OpaqueTokens.hash("old-token"))).willReturn(Optional.of(token));
        return token;
    }

    private static void assertErrorCode(Runnable call, ErrorCode expected) {
        assertThatThrownBy(call::run)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }

    private static User user(UserStatus status) {
        User user = new User();
        user.setId(1L);
        user.setEmail("alice@example.com");
        user.setFullName("Alice");
        user.setPasswordHash("hashed");
        user.setRole(Role.STUDENT);
        user.setStatus(status);
        return user;
    }
}
