package com.charlie.quizlet.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.charlie.quizlet.auth.dto.AuthResponse;
import com.charlie.quizlet.auth.dto.LoginRequest;
import com.charlie.quizlet.auth.dto.RegisterRequest;
import com.charlie.quizlet.auth.reset.PasswordResetService;
import com.charlie.quizlet.common.GlobalExceptionHandler;
import com.charlie.quizlet.common.error.BusinessException;
import com.charlie.quizlet.common.error.ErrorCatalog;
import com.charlie.quizlet.common.error.ErrorCode;
import com.charlie.quizlet.common.error.ErrorCodeRepository;
import com.charlie.quizlet.config.AppConfig;
import com.charlie.quizlet.config.JwtConfig;
import com.charlie.quizlet.config.SecurityConfig;
import com.charlie.quizlet.user.Role;
import com.charlie.quizlet.user.User;
import com.charlie.quizlet.user.UserResponse;
import com.charlie.quizlet.user.UserStatus;

@WebMvcTest(AuthController.class)
@Import({ AppConfig.class, SecurityConfig.class, JwtConfig.class, JwtService.class, GlobalExceptionHandler.class,
        ErrorCatalog.class })
class AuthControllerTest {

    private static final UserResponse ALICE = new UserResponse(42L, "alice@example.com", "Alice", Role.STUDENT,
            UserStatus.ACTIVE, Instant.parse("2026-01-01T00:00:00Z"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private PasswordResetService passwordResetService;

    /** Bảng error_codes rỗng -> dùng message mặc định trong enum ErrorCode. */
    @MockitoBean
    private ErrorCodeRepository errorCodeRepository;

    @Test
    void registerIsPublicAndReturnsCreated() throws Exception {
        given(authService.register(any(RegisterRequest.class)))
                .willReturn(new AuthResponse("token", "Bearer", 900, "refresh", ALICE));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"alice@example.com","password":"secret123","fullName":"Alice"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("token"))
                .andExpect(jsonPath("$.user.email").value("alice@example.com"));
    }

    @Test
    void validationMessagesFollowAcceptLanguage() throws Exception {
        String body = """
                {"email":"alice@example.com","password":"secret123","fullName":""}
                """;

        mockMvc.perform(post("/api/auth/register").header("Accept-Language", "vi")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(jsonPath("$.errors.fullName").value("không được để trống"))
                .andExpect(jsonPath("$.errorCode").value("COMMON_VALIDATION_FAILED"));

        mockMvc.perform(post("/api/auth/register").header("Accept-Language", "en")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(jsonPath("$.errors.fullName").value("must not be blank"));
    }

    @Test
    void registerRejectsInvalidBody() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"not-an-email","password":"short","fullName":""}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.fullName").exists());
    }

    @Test
    void loginWithBadCredentialsReturnsUnauthorized() throws Exception {
        given(authService.login(any(LoginRequest.class)))
                .willThrow(new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"alice@example.com","password":"wrong-password"}
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH_INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.errorMessage").value("Incorrect email or password"));
    }

    @Test
    void refreshIsPublicAndReturnsNewTokens() throws Exception {
        given(authService.refresh("old-refresh"))
                .willReturn(new AuthResponse("new-access", "Bearer", 900, "new-refresh", ALICE));

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"refreshToken":"old-refresh"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh"));
    }

    @Test
    void refreshWithInvalidTokenReturnsUnauthorized() throws Exception {
        given(authService.refresh("bad"))
                .willThrow(new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID));

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"refreshToken":"bad"}
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("AUTH_REFRESH_TOKEN_INVALID"));
    }

    @Test
    void refreshRejectsBlankToken() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"refreshToken":""}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.refreshToken").exists());
    }

    @Test
    void logoutIsPublicAndReturnsNoContent() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"refreshToken":"old-refresh"}
                        """))
                .andExpect(status().isNoContent());
        verify(authService).logout("old-refresh");
    }

    @Test
    void forgotPasswordIsPublicAndReturnsNoContent() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"alice@example.com"}
                        """))
                .andExpect(status().isNoContent());
        verify(passwordResetService).requestReset("alice@example.com");
    }

    @Test
    void forgotPasswordRejectsInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"not-an-email"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    void resetPasswordIsPublicAndReturnsNoContent() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"token":"abc","newPassword":"newsecret123"}
                        """))
                .andExpect(status().isNoContent());
        verify(passwordResetService).resetPassword("abc", "newsecret123");
    }

    @Test
    void resetPasswordWithBadTokenReturnsBadRequest() throws Exception {
        willThrow(new BusinessException(ErrorCode.AUTH_RESET_TOKEN_INVALID))
                .given(passwordResetService).resetPassword("bad", "newsecret123");

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"token":"bad","newPassword":"newsecret123"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("AUTH_RESET_TOKEN_INVALID"));
    }

    @Test
    void meWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("COMMON_UNAUTHORIZED"));
    }

    @Test
    void meWithValidTokenReturnsCurrentUser() throws Exception {
        given(authService.currentUser(42L)).willReturn(ALICE);

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + tokenFor(42L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.email").value("alice@example.com"));
        verify(authService).currentUser(42L);
    }

    @Test
    void meWithTamperedTokenIsUnauthorized() throws Exception {
        String token = tokenFor(42L);
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("AA") ? "BB" : "AA");

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }

    private String tokenFor(long userId) {
        User user = new User();
        user.setId(userId);
        user.setEmail("alice@example.com");
        user.setRole(Role.STUDENT);
        return jwtService.issueAccessToken(user);
    }
}
