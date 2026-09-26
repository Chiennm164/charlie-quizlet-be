package com.charlie.quizlet.quiz;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.charlie.quizlet.auth.CurrentUser;
import com.charlie.quizlet.common.ApiPaths;
import com.charlie.quizlet.common.dto.PageResponse;
import com.charlie.quizlet.quiz.dto.QuizRequest;
import com.charlie.quizlet.quiz.dto.QuizResponse;
import com.charlie.quizlet.quiz.dto.QuizSummaryResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "Quizzes", description = "Multiple-choice quizzes authored by teachers / admins")
public class QuizController {

    private final QuizService quizService;

    @GetMapping(ApiPaths.QUIZZES)
    @Operation(summary = "List published quizzes", description = "Search by title (case-insensitive), sort and paginate.")
    @ApiResponse(responseCode = "200", description = "One page of quizzes")
    @ApiResponse(responseCode = "401", description = "Missing, invalid or expired token", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public PageResponse<QuizSummaryResponse> listPublished(
            @Parameter(description = "Text contained in the title") @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "RECENT") QuizSort sort,
            @Parameter(description = "Page number, from 0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size, 1-50") @RequestParam(defaultValue = "12") int size) {
        return quizService.listPublished(q, sort, page, size);
    }

    @GetMapping(ApiPaths.QUIZZES_MINE)
    @Operation(summary = "List quizzes authored by the current user, drafts included (TEACHER / ADMIN)")
    @ApiResponse(responseCode = "200", description = "One page of quizzes")
    @ApiResponse(responseCode = "403", description = "Not a teacher or admin", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public PageResponse<QuizSummaryResponse> listMine(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Text contained in the title") @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "RECENT") QuizSort sort,
            @Parameter(description = "Page number, from 0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size, 1-50") @RequestParam(defaultValue = "12") int size) {
        return quizService.listMine(CurrentUser.from(jwt), q, sort, page, size);
    }

    @PostMapping(ApiPaths.QUIZZES)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a quiz (TEACHER / ADMIN)")
    @ApiResponse(responseCode = "201", description = "Created")
    @ApiResponse(responseCode = "400", description = "Validation failed, not exactly one correct answer, duplicated answers, or publishing an empty quiz", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "Not a teacher or admin", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public QuizResponse create(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody QuizRequest request) {
        return quizService.create(CurrentUser.from(jwt), request);
    }

    @GetMapping(ApiPaths.QUIZ)
    @Operation(summary = "Get a quiz",
            description = "Questions with correct answers are returned only when canEdit is true (author / admin).")
    @ApiResponse(responseCode = "200", description = "Quiz")
    @ApiResponse(responseCode = "404", description = "Not found, or a draft the current user cannot edit", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public QuizResponse get(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return quizService.get(CurrentUser.from(jwt), id);
    }

    @PutMapping(ApiPaths.QUIZ)
    @Operation(summary = "Replace a quiz and its questions (author / admin)",
            description = "Send every question in the new order. Questions and answers with an id are kept and updated, "
                    + "those without an id are added, and existing ones missing from the request are deleted.")
    @ApiResponse(responseCode = "200", description = "Updated")
    @ApiResponse(responseCode = "400", description = "Validation failed, invalid answers, or an id not in this quiz", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "Not a teacher or admin, or someone else's quiz", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Not found, or a draft the current user cannot edit", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public QuizResponse update(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @Valid @RequestBody QuizRequest request) {
        return quizService.update(CurrentUser.from(jwt), id, request);
    }

    @DeleteMapping(ApiPaths.QUIZ)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a quiz (author / admin)")
    @ApiResponse(responseCode = "204", description = "Deleted")
    @ApiResponse(responseCode = "403", description = "Not a teacher or admin, or someone else's quiz", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Not found, or a draft the current user cannot edit", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public void delete(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        quizService.delete(CurrentUser.from(jwt), id);
    }
}
