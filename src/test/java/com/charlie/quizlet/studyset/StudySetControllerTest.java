package com.charlie.quizlet.studyset;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.charlie.quizlet.auth.JwtService;
import com.charlie.quizlet.common.GlobalExceptionHandler;
import com.charlie.quizlet.common.dto.PageResponse;
import com.charlie.quizlet.common.error.BusinessException;
import com.charlie.quizlet.common.error.ErrorCatalog;
import com.charlie.quizlet.common.error.ErrorCode;
import com.charlie.quizlet.common.error.ErrorCodeRepository;
import com.charlie.quizlet.config.AppConfig;
import com.charlie.quizlet.config.JwtConfig;
import com.charlie.quizlet.config.SecurityConfig;
import com.charlie.quizlet.studyset.dto.CardResponse;
import com.charlie.quizlet.studyset.dto.StudySetRequest;
import com.charlie.quizlet.studyset.dto.StudySetResponse;
import com.charlie.quizlet.studyset.dto.StudySetSummaryResponse;
import com.charlie.quizlet.user.Role;
import com.charlie.quizlet.user.User;

@WebMvcTest(StudySetController.class)
@Import({ AppConfig.class, SecurityConfig.class, JwtConfig.class, JwtService.class, GlobalExceptionHandler.class,
        ErrorCatalog.class })
class StudySetControllerTest {

    private static final String VALID_BODY = """
            {"title":"Animals","visibility":"PRIVATE",
             "cards":[{"term":"cat","definition":"con mèo"},{"term":"dog","definition":"con chó"}]}
            """;

    private static final StudySetResponse ANIMALS = new StudySetResponse(10L, "Animals", null,
            StudySetVisibility.PRIVATE, new StudySetResponse.Owner(1L, "Alice"),
            List.of(new CardResponse(101L, "cat", "con mèo"), new CardResponse(102L, "dog", "con chó")),
            Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private StudySetService studySetService;

    @MockitoBean
    private ErrorCodeRepository errorCodeRepository;

    @Test
    void requiresLogin() throws Exception {
        mockMvc.perform(get("/api/study-sets/10"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("COMMON_UNAUTHORIZED"));
        verifyNoInteractions(studySetService);
    }

    @Test
    void createReturnsCreatedSet() throws Exception {
        given(studySetService.create(eq(1L), any(StudySetRequest.class))).willReturn(ANIMALS);

        mockMvc.perform(post("/api/study-sets").header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.cards[1].term").value("dog"))
                .andExpect(jsonPath("$.owner.fullName").value("Alice"));
    }

    @Test
    void createNeedsAtLeastTwoValidCards() throws Exception {
        mockMvc.perform(post("/api/study-sets").header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"title":"","visibility":"PRIVATE","cards":[{"term":"","definition":"x"}]}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.cards").exists())
                .andExpect(jsonPath("$.errors['cards[0].term']").exists());
        verifyNoInteractions(studySetService);
    }

    @Test
    void notFoundHasErrorCode() throws Exception {
        given(studySetService.get(1L, 99L)).willThrow(new BusinessException(ErrorCode.STUDY_SET_NOT_FOUND));

        mockMvc.perform(get("/api/study-sets/99").header("Authorization", bearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("STUDY_SET_NOT_FOUND"));
    }

    @Test
    void listMineUsesDefaultsAndIsNotMistakenForAnId() throws Exception {
        given(studySetService.listMine(1L, "", StudySetSort.RECENT, 0, 12)).willReturn(new PageResponse<>(
                List.of(new StudySetSummaryResponse(10L, "Animals", null, StudySetVisibility.PRIVATE, 2,
                        Instant.parse("2026-01-01T00:00:00Z"))), 0, 12, 1, 1));

        mockMvc.perform(get("/api/study-sets/mine").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].cardCount").value(2))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listMineRejectsUnknownSort() throws Exception {
        mockMvc.perform(get("/api/study-sets/mine?sort=password").header("Authorization", bearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("COMMON_BAD_REQUEST"));
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/study-sets/10").header("Authorization", bearer()))
                .andExpect(status().isNoContent());
        verify(studySetService).delete(1L, 10L);
    }

    private String bearer() {
        User user = new User();
        user.setId(1L);
        user.setEmail("alice@example.com");
        user.setRole(Role.STUDENT);
        return "Bearer " + jwtService.issueAccessToken(user);
    }
}
