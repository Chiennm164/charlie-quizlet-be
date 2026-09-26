package com.charlie.quizlet.admin;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.charlie.quizlet.auth.JwtService;
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

@WebMvcTest(AdminUserController.class)
@Import({ AppConfig.class, SecurityConfig.class, JwtConfig.class, JwtService.class, GlobalExceptionHandler.class,
        ErrorCatalog.class })
class AdminUserControllerTest {

    private static final UserResponse BOB = new UserResponse(7L, "bob@example.com", "Bob", Role.TEACHER,
            UserStatus.PENDING, Instant.parse("2026-01-01T00:00:00Z"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AdminUserService adminUserService;

    @MockitoBean
    private ErrorCodeRepository errorCodeRepository;

    @Test
    void withoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/users/pending"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("COMMON_UNAUTHORIZED"));
    }

    @Test
    void nonAdminIsForbidden() throws Exception {
        for (Role role : List.of(Role.STUDENT, Role.TEACHER)) {
            mockMvc.perform(get("/api/admin/users/pending").header("Authorization", bearer(role)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.errorCode").value("COMMON_FORBIDDEN"));
            mockMvc.perform(post("/api/admin/users/7/approve").header("Authorization", bearer(role)))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(adminUserService);
    }

    @Test
    void adminListsPendingUsers() throws Exception {
        given(adminUserService.listPending()).willReturn(List.of(BOB));

        mockMvc.perform(get("/api/admin/users/pending").header("Authorization", bearer(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("bob@example.com"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void adminApprovesUser() throws Exception {
        given(adminUserService.approve(7L)).willReturn(new UserResponse(7L, "bob@example.com", "Bob", Role.TEACHER,
                UserStatus.ACTIVE, BOB.createdAt()));

        mockMvc.perform(post("/api/admin/users/7/approve").header("Authorization", bearer(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void adminRejectsUser() throws Exception {
        mockMvc.perform(post("/api/admin/users/7/reject").header("Authorization", bearer(Role.ADMIN)))
                .andExpect(status().isNoContent());
        verify(adminUserService).reject(7L);
    }

    @Test
    void approvingNonPendingUserIsConflict() throws Exception {
        willThrow(new BusinessException(ErrorCode.ADMIN_USER_NOT_PENDING)).given(adminUserService).approve(7L);

        mockMvc.perform(post("/api/admin/users/7/approve").header("Authorization", bearer(Role.ADMIN)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ADMIN_USER_NOT_PENDING"));
    }

    private String bearer(Role role) {
        User user = new User();
        user.setId(1L);
        user.setEmail("someone@example.com");
        user.setRole(role);
        return "Bearer " + jwtService.issueAccessToken(user);
    }
}
