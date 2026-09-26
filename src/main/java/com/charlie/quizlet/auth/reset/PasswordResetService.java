package com.charlie.quizlet.auth.reset;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.charlie.quizlet.auth.OpaqueTokens;
import com.charlie.quizlet.auth.RefreshTokenRepository;
import com.charlie.quizlet.common.error.BusinessException;
import com.charlie.quizlet.common.error.ErrorCode;
import com.charlie.quizlet.config.AppProperties;
import com.charlie.quizlet.user.User;
import com.charlie.quizlet.user.UserRepository;
import com.charlie.quizlet.user.UserStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetNotifier notifier;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final AppProperties appProperties;

    /**
     * Issues a reset link if the e-mail belongs to an active account. Silently does nothing
     * otherwise, so the endpoint cannot be used to discover which e-mails are registered.
     */
    @Transactional
    public void requestReset(String email) {
        userRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .ifPresent(this::issueToken);
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        Instant now = clock.instant();
        PasswordResetToken token = tokenRepository.findByTokenHash(OpaqueTokens.hash(rawToken))
                .filter(t -> t.isUsable(now))
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_RESET_TOKEN_INVALID));

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        tokenRepository.invalidateAllForUser(user.getId(), now);
        // Đăng xuất mọi thiết bị: ai đang giữ phiên cũ (có thể là người đã biết mật khẩu cũ) phải đăng nhập lại.
        refreshTokenRepository.revokeAllForUser(user.getId(), now);
    }

    private void issueToken(User user) {
        Instant now = clock.instant();
        tokenRepository.invalidateAllForUser(user.getId(), now);

        String rawToken = OpaqueTokens.generate();
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(OpaqueTokens.hash(rawToken));
        token.setExpiresAt(now.plus(appProperties.passwordReset().tokenTtl()));
        tokenRepository.save(token);

        notifier.sendResetLink(user, appProperties.frontend().resetPasswordLink(rawToken));
    }
}
