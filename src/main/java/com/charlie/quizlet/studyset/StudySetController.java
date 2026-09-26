package com.charlie.quizlet.studyset;

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

import com.charlie.quizlet.common.ApiPaths;
import com.charlie.quizlet.common.dto.PageResponse;
import com.charlie.quizlet.studyset.dto.StudySetRequest;
import com.charlie.quizlet.studyset.dto.StudySetResponse;
import com.charlie.quizlet.studyset.dto.StudySetSummaryResponse;

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
@Tag(name = "Study sets", description = "Study sets (a list of term/definition cards)")
public class StudySetController {

    private final StudySetService studySetService;

    @PostMapping(ApiPaths.STUDY_SETS)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a study set owned by the current user")
    @ApiResponse(responseCode = "201", description = "Created")
    @ApiResponse(responseCode = "400", description = "Validation failed (at least 2 cards) or duplicated terms", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "Missing, invalid or expired token", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public StudySetResponse create(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody StudySetRequest request) {
        return studySetService.create(userId(jwt), request);
    }

    @GetMapping(ApiPaths.STUDY_SETS_MINE)
    @Operation(summary = "List study sets of the current user",
            description = "Search by title (case-insensitive), sort and paginate. Cards are not included, only their count.")
    @ApiResponse(responseCode = "200", description = "One page of study sets")
    @ApiResponse(responseCode = "400", description = "Unknown sort value", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "Missing, invalid or expired token", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public PageResponse<StudySetSummaryResponse> listMine(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Text contained in the title") @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "RECENT") StudySetSort sort,
            @Parameter(description = "Page number, from 0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size, 1-50") @RequestParam(defaultValue = "12") int size) {
        return studySetService.listMine(userId(jwt), q, sort, page, size);
    }

    @GetMapping(ApiPaths.STUDY_SET)
    @Operation(summary = "Get a study set with its cards", description = "Private study sets are visible to their owner only.")
    @ApiResponse(responseCode = "200", description = "Study set")
    @ApiResponse(responseCode = "401", description = "Missing, invalid or expired token", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Not found, or private and not owned by the current user", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public StudySetResponse get(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return studySetService.get(userId(jwt), id);
    }

    @PutMapping(ApiPaths.STUDY_SET)
    @Operation(summary = "Replace a study set and its cards",
            description = "Send every card in the new order. Cards with an id are kept and updated, cards without an id "
                    + "are added, and existing cards missing from the list are deleted.")
    @ApiResponse(responseCode = "200", description = "Updated")
    @ApiResponse(responseCode = "400", description = "Validation failed, duplicated terms, or a card id not in this set", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "Public study set owned by someone else", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Not found, or private and not owned by the current user", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public StudySetResponse update(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @Valid @RequestBody StudySetRequest request) {
        return studySetService.update(userId(jwt), id, request);
    }

    @DeleteMapping(ApiPaths.STUDY_SET)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a study set and its cards")
    @ApiResponse(responseCode = "204", description = "Deleted")
    @ApiResponse(responseCode = "403", description = "Public study set owned by someone else", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Not found, or private and not owned by the current user", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public void delete(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        studySetService.delete(userId(jwt), id);
    }

    private static Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
