package com.charlie.quizlet.topic;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.charlie.quizlet.common.error.ErrorCatalog;
import com.charlie.quizlet.common.error.ErrorCodeRepository;
import com.charlie.quizlet.config.AppConfig;
import com.charlie.quizlet.config.JwtConfig;
import com.charlie.quizlet.config.SecurityConfig;
import com.charlie.quizlet.topic.dto.TopicRequest;
import com.charlie.quizlet.topic.dto.TopicResponse;
import com.charlie.quizlet.user.Role;
import com.charlie.quizlet.user.User;

@WebMvcTest(TopicController.class)
@Import({ AppConfig.class, SecurityConfig.class, JwtConfig.class, JwtService.class, GlobalExceptionHandler.class,
        ErrorCatalog.class })
class TopicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private TopicService topicService;

    @MockitoBean
    private ErrorCodeRepository errorCodeRepository;

    @Test
    void studentReadsTopicsButCannotChangeThem() throws Exception {
        given(topicService.list()).willReturn(List.of(new TopicResponse(1L, "Toán", 2)));

        mockMvc.perform(get("/api/topics").header("Authorization", bearer(Role.STUDENT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Toán"));
        mockMvc.perform(post("/api/admin/topics").header("Authorization", bearer(Role.STUDENT))
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Văn\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/admin/topics/1").header("Authorization", bearer(Role.STUDENT)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCreatesTopicAndBlankNameIsInvalid() throws Exception {
        given(topicService.create(any(TopicRequest.class))).willReturn(new TopicResponse(2L, "Văn", 0));

        mockMvc.perform(post("/api/admin/topics").header("Authorization", bearer(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Văn\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2));
        mockMvc.perform(post("/api/admin/topics").header("Authorization", bearer(Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void requiresLogin() throws Exception {
        mockMvc.perform(get("/api/topics")).andExpect(status().isUnauthorized());
        verifyNoInteractions(topicService);
    }

    private String bearer(Role role) {
        User user = new User();
        user.setId(1L);
        user.setEmail("someone@example.com");
        user.setRole(role);
        return "Bearer " + jwtService.issueAccessToken(user);
    }
}
