package com.charlie.quizlet.auth.reset;

import org.springframework.stereotype.Component;

import com.charlie.quizlet.user.User;

import lombok.extern.slf4j.Slf4j;

/**
 * Dev-only notifier: writes the reset link to the log instead of e-mailing it.
 * Replace with an SMTP-backed implementation once mail is configured.
 */
@Slf4j
@Component
public class LoggingPasswordResetNotifier implements PasswordResetNotifier {

    @Override
    public void sendResetLink(User user, String resetLink) {
        log.info("Password reset link for {}: {}", user.getEmail(), resetLink);
    }
}
