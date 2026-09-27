package com.charlie.quizlet.me;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.charlie.quizlet.attempt.AttemptMode;
import com.charlie.quizlet.attempt.AttemptStatus;
import com.charlie.quizlet.auth.CurrentUser;
import com.charlie.quizlet.common.ApiPaths;
import com.charlie.quizlet.common.dto.PageResponse;
import com.charlie.quizlet.me.dto.MyAttemptResponse;
import com.charlie.quizlet.me.dto.QuizMarksResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "Me", description = "The caller's own data: attempt history, progress per quiz, favorite quizzes.")
public class MeController {

    private final MeService meService;

    @GetMapping(ApiPaths.ME_ATTEMPTS)
    @Operation(summary = "My attempts across all quizzes",
            description = "SUBMITTED: newest submission first; IN_PROGRESS: newest start first. Overdue exams are graded first.")
    @ApiResponse(responseCode = "200", description = "One page of attempts")
    public PageResponse<MyAttemptResponse> attempts(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "SUBMITTED") AttemptStatus status,
            @Parameter(description = "Only this mode") @RequestParam(required = false) AttemptMode mode,
            @Parameter(description = "Only this topic") @RequestParam(required = false) Long topicId,
            @Parameter(description = "Page number, from 0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size, 1-50") @RequestParam(defaultValue = "12") int size) {
        return meService.attempts(CurrentUser.from(jwt), status, mode, topicId, page, size);
    }

    @GetMapping(ApiPaths.ME_QUIZ_MARKS)
    @Operation(summary = "My marks on quizzes",
            description = "Best score / unfinished attempt per quiz I have taken, and my favorite quiz ids.")
    @ApiResponse(responseCode = "200", description = "Marks")
    public QuizMarksResponse marks(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return meService.marks(CurrentUser.from(jwt));
    }

    @PutMapping(ApiPaths.ME_FAVORITE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Add a quiz to my favorites (idempotent)")
    @ApiResponse(responseCode = "204", description = "Added")
    @ApiResponse(responseCode = "404", description = "Quiz not found or a draft", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public void addFavorite(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @PathVariable Long quizId) {
        meService.addFavorite(CurrentUser.from(jwt), quizId);
    }

    @DeleteMapping(ApiPaths.ME_FAVORITE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a quiz from my favorites (idempotent)")
    @ApiResponse(responseCode = "204", description = "Removed")
    public void removeFavorite(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @PathVariable Long quizId) {
        meService.removeFavorite(CurrentUser.from(jwt), quizId);
    }
}
