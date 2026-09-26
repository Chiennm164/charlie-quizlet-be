package com.charlie.quizlet.topic;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.charlie.quizlet.common.ApiPaths;
import com.charlie.quizlet.topic.dto.TopicRequest;
import com.charlie.quizlet.topic.dto.TopicResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "Topics", description = "Quiz topics (flat list). Anyone logged in can read; only ADMIN can change.")
public class TopicController {

    private final TopicService topicService;

    @GetMapping(ApiPaths.TOPICS)
    @Operation(summary = "List all topics, A → Z, with their quiz count")
    @ApiResponse(responseCode = "200", description = "Topics")
    public List<TopicResponse> list() {
        return topicService.list();
    }

    @PostMapping(ApiPaths.ADMIN_TOPICS)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a topic (ADMIN)")
    @ApiResponse(responseCode = "201", description = "Created")
    @ApiResponse(responseCode = "403", description = "Not an admin", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Name already exists (case-insensitive)", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public TopicResponse create(@Valid @RequestBody TopicRequest request) {
        return topicService.create(request);
    }

    @PutMapping(ApiPaths.ADMIN_TOPIC)
    @Operation(summary = "Rename a topic (ADMIN)")
    @ApiResponse(responseCode = "200", description = "Renamed")
    @ApiResponse(responseCode = "403", description = "Not an admin", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Topic not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Name already exists (case-insensitive)", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public TopicResponse rename(@PathVariable Long id, @Valid @RequestBody TopicRequest request) {
        return topicService.rename(id, request);
    }

    @DeleteMapping(ApiPaths.ADMIN_TOPIC)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an empty topic (ADMIN)")
    @ApiResponse(responseCode = "204", description = "Deleted")
    @ApiResponse(responseCode = "403", description = "Not an admin", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Topic not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "The topic still has quizzes", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public void delete(@PathVariable Long id) {
        topicService.delete(id);
    }
}
