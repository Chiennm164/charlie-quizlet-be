package com.charlie.quizlet.quiz;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.charlie.quizlet.auth.CurrentUser;
import com.charlie.quizlet.auth.JwtService;
import com.charlie.quizlet.common.GlobalExceptionHandler;
import com.charlie.quizlet.common.dto.PageResponse;
import com.charlie.quizlet.common.error.ErrorCatalog;
import com.charlie.quizlet.common.error.ErrorCodeRepository;
import com.charlie.quizlet.config.AppConfig;
import com.charlie.quizlet.config.JwtConfig;
import com.charlie.quizlet.config.SecurityConfig;
import com.charlie.quizlet.quiz.dto.QuizRequest;
import com.charlie.quizlet.quiz.dto.QuizResponse;
import com.charlie.quizlet.user.Role;
import com.charlie.quizlet.user.User;

@WebMvcTest(QuizController.class)
@Import({ AppConfig.class, SecurityConfig.class, JwtConfig.class, JwtService.class, GlobalExceptionHandler.class,
        ErrorCatalog.class })
class QuizControllerTest {

    private static final String BODY = """
            {"title":"Math","timeLimitMinutes":15,"status":"PUBLISHED","questions":[
              {"content":"2 + 2 = ?","options":[{"content":"3","correct":false},{"content":"4","correct":true}]}]}
            """;

    private static final QuizResponse MATH = new QuizResponse(10L, "Math", null, 15, QuizStatus.PUBLISHED,
            new QuizResponse.Owner(1L, "Teacher"), 1, List.of(), true, Instant.EPOCH, Instant.EPOCH, Instant.EPOCH);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private QuizService quizService;

    @MockitoBean
    private ErrorCodeRepository errorCodeRepository;

    @Test
    void studentCannotCreateUpdateDeleteOrListOwnQuizzes() throws Exception {
        String student = bearer(Role.STUDENT);
        mockMvc.perform(post("/api/quizzes").header("Authorization", student)
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("COMMON_FORBIDDEN"));
        mockMvc.perform(put("/api/quizzes/10").header("Authorization", student)
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/quizzes/10").header("Authorization", student))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/quizzes/mine").header("Authorization", student))
                .andExpect(status().isForbidden());
        verifyNoInteractions(quizService);
    }

    @Test
    void teacherCreatesQuiz() throws Exception {
        given(quizService.create(eq(new CurrentUser(1L, Role.TEACHER)), any(QuizRequest.class))).willReturn(MATH);

        mockMvc.perform(post("/api/quizzes").header("Authorization", bearer(Role.TEACHER))
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.canEdit").value(true));
    }

    @Test
    void studentCanListAndViewPublishedQuizzes() throws Exception {
        given(quizService.listPublished("", QuizSort.RECENT, 0, 12))
                .willReturn(new PageResponse<>(List.of(), 0, 12, 0, 0));
        given(quizService.get(new CurrentUser(1L, Role.STUDENT), 10L)).willReturn(MATH);

        mockMvc.perform(get("/api/quizzes").header("Authorization", bearer(Role.STUDENT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/api/quizzes/10").header("Authorization", bearer(Role.STUDENT)))
                .andExpect(status().isOk());
    }

    @Test
    void validatesOptionsCountAndTimeLimit() throws Exception {
        mockMvc.perform(post("/api/quizzes").header("Authorization", bearer(Role.TEACHER))
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"title":"Math","timeLimitMinutes":0,"status":"DRAFT","questions":[
                          {"content":"Q","options":[{"content":"only","correct":true}]}]}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.timeLimitMinutes").exists())
                .andExpect(jsonPath("$.errors['questions[0].options']").exists());
        verifyNoInteractions(quizService);
    }

    private String bearer(Role role) {
        User user = new User();
        user.setId(1L);
        user.setEmail("someone@example.com");
        user.setRole(role);
        return "Bearer " + jwtService.issueAccessToken(user);
    }
}
