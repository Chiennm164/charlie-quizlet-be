package com.charlie.quizlet.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.charlie.quizlet.auth.dto.AuthResponse;
import com.charlie.quizlet.auth.dto.ChangePasswordRequest;
import com.charlie.quizlet.auth.dto.LoginRequest;
import com.charlie.quizlet.auth.dto.RegisterRequest;
import com.charlie.quizlet.auth.dto.RegisterResponse;
import com.charlie.quizlet.auth.dto.UpdateProfileRequest;
import com.charlie.quizlet.common.error.BusinessException;
import com.charlie.quizlet.common.error.ErrorCode;
import com.charlie.quizlet.user.Role;
import com.charlie.quizlet.user.User;
import com.charlie.quizlet.user.UserRepository;
import com.charlie.quizlet.user.UserResponse;
import com.charlie.quizlet.user.UserStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    /**
     * Refresh token đã bị thay (xoay vòng) mà được gửi lại trong khoảng này thì coi là 2 request làm mới gần như
     * cùng lúc (vd. 2 tab dùng chung phiên): chỉ từ chối. Gửi lại sau khoảng này: nghi token bị lộ, thu hồi cả phiên.
     */
    private static final Duration REFRESH_REUSE_GRACE = Duration.ofSeconds(10);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.AUTH_EMAIL_ALREADY_REGISTERED);
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setRole(request.role().toRole());
        // Teacher tạo được câu hỏi / đề thi nên phải chờ Admin duyệt mới đăng nhập được.
        user.setStatus(user.getRole() == Role.TEACHER ? UserStatus.PENDING : UserStatus.ACTIVE);
        userRepository.saveAndFlush(user);

        AuthResponse session = user.getStatus() == UserStatus.ACTIVE ? issueTokens(user, UUID.randomUUID()) : null;
        return new RegisterResponse(UserResponse.from(user), session);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.email()))
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS));
        ensureActive(user);
        return issueTokens(user, UUID.randomUUID());
    }

    /**
     * Đổi refresh token lấy cặp token mới. Token cũ bị thu hồi ngay (xoay vòng), nên refresh token bị lộ chỉ dùng
     * được tới lần làm mới kế tiếp của chủ tài khoản; sau đó ai dùng lại token cũ cũng làm cả phiên bị thu hồi.
     * <p>
     * {@code noRollbackFor}: việc thu hồi cả phiên phải được lưu dù ngay sau đó ném lỗi.
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public AuthResponse refresh(String rawRefreshToken) {
        Instant now = clock.instant();
        RefreshToken token = refreshTokenRepository.findByTokenHash(OpaqueTokens.hash(rawRefreshToken))
                .orElseThrow(AuthService::invalidRefreshToken);
        if (token.getRevokedAt() != null) {
            if (now.isAfter(token.getRevokedAt().plus(REFRESH_REUSE_GRACE))) {
                log.warn("Revoked refresh token reused for user {}, revoking the whole session", token.getUser().getId());
                refreshTokenRepository.revokeFamily(token.getFamilyId(), now);
            }
            throw invalidRefreshToken();
        }
        if (!now.isBefore(token.getExpiresAt())) {
            throw invalidRefreshToken();
        }

        User user = token.getUser();
        ensureActive(user);
        token.setRevokedAt(now);
        return issueTokens(user, token.getFamilyId());
    }

    /** Thu hồi refresh token của phiên hiện tại. Token không tồn tại / đã thu hồi cũng không báo lỗi: kết quả như nhau. */
    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(OpaqueTokens.hash(rawRefreshToken))
                .filter(token -> token.getRevokedAt() == null)
                .ifPresent(token -> token.setRevokedAt(clock.instant()));
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(Long userId) {
        return UserResponse.from(findActiveUser(userId));
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findActiveUser(userId);
        user.setFullName(request.fullName().trim());
        return UserResponse.from(user);
    }

    /**
     * Đổi mật khẩu rồi đăng xuất mọi thiết bị khác (ai đang giữ phiên có thể là người đã biết mật khẩu cũ).
     * Thiết bị đang đổi nhận phiên mới nên không phải đăng nhập lại.
     */
    @Transactional
    public AuthResponse changePassword(Long userId, ChangePasswordRequest request) {
        User user = findActiveUser(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.AUTH_CURRENT_PASSWORD_INCORRECT);
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        refreshTokenRepository.revokeAllForUser(user.getId(), clock.instant());
        return issueTokens(user, UUID.randomUUID());
    }

    private User findActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND));
        ensureActive(user);
        return user;
    }

    private void ensureActive(User user) {
        switch (user.getStatus()) {
            case ACTIVE -> { }
            case LOCKED -> throw new BusinessException(ErrorCode.AUTH_ACCOUNT_LOCKED);
            case PENDING -> throw new BusinessException(ErrorCode.AUTH_ACCOUNT_PENDING);
        }
    }

    /** Cấp access token + refresh token mới; {@code familyId} mới cho lần đăng nhập, giữ nguyên khi làm mới. */
    private AuthResponse issueTokens(User user, UUID familyId) {
        String refreshToken = OpaqueTokens.generate();
        RefreshToken entity = new RefreshToken();
        entity.setUser(user);
        entity.setFamilyId(familyId);
        entity.setTokenHash(OpaqueTokens.hash(refreshToken));
        entity.setExpiresAt(clock.instant().plus(jwtProperties.refreshExpiration()));
        refreshTokenRepository.save(entity);

        return new AuthResponse(jwtService.issueAccessToken(user), JwtService.TOKEN_TYPE, jwtService.expiresInSeconds(),
                refreshToken, UserResponse.from(user));
    }

    private static BusinessException invalidRefreshToken() {
        return new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
