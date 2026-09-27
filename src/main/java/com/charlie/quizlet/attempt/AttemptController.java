package com.charlie.quizlet.attempt;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.charlie.quizlet.attempt.dto.AnswerRequest;
import com.charlie.quizlet.attempt.dto.AttemptResponse;
import com.charlie.quizlet.attempt.dto.AttemptSummaryResponse;
import com.charlie.quizlet.attempt.dto.QuizStatsResponse;
import com.charlie.quizlet.attempt.dto.StartAttemptRequest;
import com.charlie.quizlet.auth.CurrentUser;
import com.charlie.quizlet.common.ApiPaths;

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
@Tag(name = "Attempts", description = "Taking a quiz: start, save answers, submit, see results. Only the taker sees an attempt.")
public class AttemptController {

    private final AttemptService attemptService;
    private final QuizStatsService quizStatsService;

    @PostMapping(ApiPaths.QUIZ_ATTEMPTS)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Start an attempt",
            description = "PRACTICE shows the answer after each question; EXAM shuffles questions and answers and "
                    + "has a deadline when the quiz has a time limit. Replaces the caller's unfinished attempt of this quiz. "
                    + "With retryWrongOf, only the wrong or skipped questions of that submitted attempt are asked.")
    @ApiResponse(responseCode = "201", description = "Started")
    @ApiResponse(responseCode = "400", description = "The quiz has no questions, or nothing to retry", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Quiz or retried attempt not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public AttemptResponse start(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @Valid @RequestBody StartAttemptRequest request) {
        return attemptService.start(CurrentUser.from(jwt), id, request);
    }

    @GetMapping(ApiPaths.QUIZ_ATTEMPTS)
    @Operation(summary = "My attempts of a quiz", description = "Latest 20, newest first, unfinished one included.")
    @ApiResponse(responseCode = "200", description = "Attempts")
    public List<AttemptSummaryResponse> history(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {
        return attemptService.history(CurrentUser.from(jwt), id);
    }

    @GetMapping(ApiPaths.ATTEMPT)
    @Operation(summary = "Get an attempt",
            description = "Correct answers and explanations are included once submitted, or per answered question in PRACTICE. "
                    + "An attempt past its deadline is submitted automatically.")
    @ApiResponse(responseCode = "200", description = "Attempt")
    @ApiResponse(responseCode = "404", description = "Not found or not the caller's", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public AttemptResponse get(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return attemptService.get(CurrentUser.from(jwt), id);
    }

    @PutMapping(ApiPaths.ATTEMPT_ANSWER)
    @Operation(summary = "Save the answer of one question",
            description = "Autosaved on every change. PRACTICE: graded at once and cannot be changed.")
    @ApiResponse(responseCode = "200", description = "Saved question")
    @ApiResponse(responseCode = "400", description = "Question or answer not in this attempt", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Already submitted, time is up, or already answered in PRACTICE", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public AttemptResponse.QuestionView answer(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id, @PathVariable Long questionId, @RequestBody AnswerRequest request) {
        return attemptService.answer(CurrentUser.from(jwt), id, questionId, request);
    }

    @GetMapping(ApiPaths.ADMIN_QUIZ_STATS)
    @Operation(summary = "Quiz statistics (ADMIN)",
            description = "Submitted attempts only: attempt / taker counts, average score, and per question "
                    + "correct / skipped counts with how often each answer was picked.")
    @ApiResponse(responseCode = "200", description = "Statistics")
    @ApiResponse(responseCode = "403", description = "Not an admin", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Quiz not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public QuizStatsResponse stats(@PathVariable Long id) {
        return quizStatsService.get(id);
    }

    @PostMapping(ApiPaths.ATTEMPT_SUBMIT)
    @Operation(summary = "Submit and grade", description = "Idempotent: submitting again returns the result.")
    @ApiResponse(responseCode = "200", description = "Graded attempt")
    @ApiResponse(responseCode = "404", description = "Not found or not the caller's", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public AttemptResponse submit(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return attemptService.submit(CurrentUser.from(jwt), id);
    }
}
