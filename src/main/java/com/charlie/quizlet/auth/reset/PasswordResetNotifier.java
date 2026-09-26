package com.charlie.quizlet.auth.reset;

import com.charlie.quizlet.user.User;

/** Delivers a password reset link to the user (e-mail in production). */
public interface PasswordResetNotifier {

    void sendResetLink(User user, String resetLink);
}
