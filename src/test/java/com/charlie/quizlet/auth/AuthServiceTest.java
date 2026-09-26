package com.charlie.quizlet.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;

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

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final AuthService service = new AuthService(userRepository, passwordEncoder, jwtService);

    @BeforeEach
    void setUp() {
        given(jwtService.issueAccessToken(any())).willReturn("token");
        given(jwtService.expiresInSeconds()).willReturn(3600L);
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
    }

    @Test
    void loginSucceedsForActiveAccount() {
        given(userRepository.findByEmail("alice@example.com")).willReturn(Optional.of(user(UserStatus.ACTIVE)));

        assertThat(service.login(new LoginRequest(" ALICE@example.com ", "secret123")).accessToken())
                .isEqualTo("token");
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
