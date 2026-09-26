package com.charlie.quizlet.admin;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.charlie.quizlet.common.ApiPaths;
import com.charlie.quizlet.user.UserResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "Admin - Users", description = "Approve or reject accounts waiting for approval (ADMIN only)")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping(ApiPaths.ADMIN_USERS_PENDING)
    @Operation(summary = "List accounts waiting for approval, oldest first")
    @ApiResponse(responseCode = "200", description = "Pending accounts")
    @ApiResponse(responseCode = "401", description = "Missing, invalid or expired token", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "Not an admin", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public List<UserResponse> listPending() {
        return adminUserService.listPending();
    }

    @PostMapping(ApiPaths.ADMIN_USER_APPROVE)
    @Operation(summary = "Approve a pending account so it can log in")
    @ApiResponse(responseCode = "200", description = "Approved")
    @ApiResponse(responseCode = "403", description = "Not an admin", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "User not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Account is not pending", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public UserResponse approve(@PathVariable Long id) {
        return adminUserService.approve(id);
    }

    @PostMapping(ApiPaths.ADMIN_USER_REJECT)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Reject a pending account", description = "The account is deleted, so the e-mail can be registered again.")
    @ApiResponse(responseCode = "204", description = "Rejected and deleted")
    @ApiResponse(responseCode = "403", description = "Not an admin", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "User not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Account is not pending", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public void reject(@PathVariable Long id) {
        adminUserService.reject(id);
    }
}
