package com.charlie.quizlet.auth;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.charlie.quizlet.auth.dto.AuthResponse;
import com.charlie.quizlet.auth.dto.LoginRequest;
import com.charlie.quizlet.auth.dto.RegisterRequest;
import com.charlie.quizlet.user.Role;
import com.charlie.quizlet.user.User;
import com.charlie.quizlet.user.UserRepository;
import com.charlie.quizlet.user.UserResponse;
import com.charlie.quizlet.user.UserStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        // Self-registration is always STUDENT; TEACHER/ADMIN are granted by an admin.
        user.setRole(Role.STUDENT);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.saveAndFlush(user);

        return toAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.email()))
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        ensureActive(user);
        return toAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User no longer exists"));
        ensureActive(user);
        return UserResponse.from(user);
    }

    private void ensureActive(User user) {
        switch (user.getStatus()) {
            case ACTIVE -> { }
            case LOCKED -> throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is locked");
            case PENDING -> throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is pending approval");
        }
    }

    private AuthResponse toAuthResponse(User user) {
        return new AuthResponse(jwtService.issueAccessToken(user), "Bearer", jwtService.expiresInSeconds(),
                UserResponse.from(user));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
