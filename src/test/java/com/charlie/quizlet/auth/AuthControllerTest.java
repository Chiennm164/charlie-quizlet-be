package com.charlie.quizlet.auth;

import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import com.charlie.quizlet.auth.dto.AuthResponse;
import com.charlie.quizlet.auth.dto.LoginRequest;
import com.charlie.quizlet.auth.dto.RegisterRequest;
import com.charlie.quizlet.common.GlobalExceptionHandler;
import com.charlie.quizlet.config.JwtConfig;
import com.charlie.quizlet.config.SecurityConfig;
import com.charlie.quizlet.user.Role;
import com.charlie.quizlet.user.User;
import com.charlie.quizlet.user.UserResponse;
import com.charlie.quizlet.user.UserStatus;

@WebMvcTest(AuthController.class)
@Import({ SecurityConfig.class, JwtConfig.class, JwtService.class, GlobalExceptionHandler.class })
class AuthControllerTest {

    private static final UserResponse ALICE = new UserResponse(42L, "alice@example.com", "Alice", Role.STUDENT,
            UserStatus.ACTIVE, Instant.parse("2026-01-01T00:00:00Z"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AuthService authService;

    @Test
    void registerIsPublicAndReturnsCreated() throws Exception {
        given(authService.register(any(RegisterRequest.class)))
                .willReturn(new AuthResponse("token", "Bearer", 3600, ALICE));

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
    void registerRejectsInvalidBody() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"not-an-email","password":"short","fullName":""}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.fullName").exists());
    }

    @Test
    void loginWithBadCredentialsReturnsUnauthorized() throws Exception {
        given(authService.login(any(LoginRequest.class)))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"alice@example.com","password":"wrong-password"}
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
    }

    @Test
    void meWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
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
