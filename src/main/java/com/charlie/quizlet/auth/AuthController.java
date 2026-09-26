package com.charlie.quizlet.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.charlie.quizlet.auth.dto.AuthResponse;
import com.charlie.quizlet.auth.dto.ChangePasswordRequest;
import com.charlie.quizlet.auth.dto.ForgotPasswordRequest;
import com.charlie.quizlet.auth.dto.LoginRequest;
import com.charlie.quizlet.auth.dto.RefreshTokenRequest;
import com.charlie.quizlet.auth.dto.RegisterRequest;
import com.charlie.quizlet.auth.dto.RegisterResponse;
import com.charlie.quizlet.auth.dto.ResetPasswordRequest;
import com.charlie.quizlet.auth.dto.UpdateProfileRequest;
import com.charlie.quizlet.auth.reset.PasswordResetService;
import com.charlie.quizlet.common.ApiPaths;
import com.charlie.quizlet.user.UserResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Registration, login, password reset, current user and profile")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping(ApiPaths.AUTH_REGISTER)
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirements
    @Operation(summary = "Register a STUDENT or TEACHER account",
            description = "STUDENT accounts are active and logged in at once. TEACHER accounts wait for admin approval: "
                    + "status is PENDING and session is null.")
    @ApiResponse(responseCode = "201", description = "Registered")
    @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Email is already registered", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping(ApiPaths.AUTH_LOGIN)
    @SecurityRequirements
    @Operation(summary = "Log in with email and password and return an access token")
    @ApiResponse(responseCode = "200", description = "Logged in")
    @ApiResponse(responseCode = "401", description = "Invalid email or password", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "Account is locked or pending", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping(ApiPaths.AUTH_REFRESH)
    @SecurityRequirements
    @Operation(summary = "Exchange a refresh token for a new access token and refresh token",
            description = "The refresh token is single-use: the old one is revoked and a new one is returned.")
    @ApiResponse(responseCode = "200", description = "New tokens")
    @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "Refresh token is invalid, expired or revoked", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "Account is locked or pending", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping(ApiPaths.AUTH_LOGOUT)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirements
    @Operation(summary = "Log out by revoking the refresh token of this session",
            description = "Always returns 204, even if the token is unknown or already revoked.")
    @ApiResponse(responseCode = "204", description = "Logged out")
    @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public void logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
    }

    @PostMapping(ApiPaths.AUTH_FORGOT_PASSWORD)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirements
    @Operation(summary = "Send a password reset link if the e-mail belongs to an active account",
            description = "Always returns 204, whether or not the e-mail is registered.")
    @ApiResponse(responseCode = "204", description = "Request accepted")
    @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.email());
    }

    @PostMapping(ApiPaths.AUTH_RESET_PASSWORD)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirements
    @Operation(summary = "Set a new password using the token from the reset link")
    @ApiResponse(responseCode = "204", description = "Password changed")
    @ApiResponse(responseCode = "400", description = "Validation failed, or the token is invalid, used or expired", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.token(), request.newPassword());
    }

    @GetMapping(ApiPaths.AUTH_ME)
    @Operation(summary = "Get the currently authenticated user")
    @ApiResponse(responseCode = "200", description = "Current user")
    @ApiResponse(responseCode = "401", description = "Missing, invalid or expired token", content = @Content)
    public UserResponse me(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        return authService.currentUser(Long.valueOf(jwt.getSubject()));
    }

    @PatchMapping(ApiPaths.AUTH_ME)
    @Operation(summary = "Update the profile (full name) of the current user")
    @ApiResponse(responseCode = "200", description = "Updated user")
    @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "Missing, invalid or expired token", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public UserResponse updateProfile(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateProfileRequest request) {
        return authService.updateProfile(Long.valueOf(jwt.getSubject()), request);
    }

    @PostMapping(ApiPaths.AUTH_CHANGE_PASSWORD)
    @Operation(summary = "Change the password of the current user",
            description = "Logs out every other device and returns a new session for this one.")
    @ApiResponse(responseCode = "200", description = "Password changed, new tokens")
    @ApiResponse(responseCode = "400", description = "Validation failed, or the current password is incorrect", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "Missing, invalid or expired token", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public AuthResponse changePassword(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ChangePasswordRequest request) {
        return authService.changePassword(Long.valueOf(jwt.getSubject()), request);
    }
}
