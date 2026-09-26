package com.charlie.quizlet.admin;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.charlie.quizlet.common.error.BusinessException;
import com.charlie.quizlet.common.error.ErrorCode;
import com.charlie.quizlet.user.User;
import com.charlie.quizlet.user.UserRepository;
import com.charlie.quizlet.user.UserResponse;
import com.charlie.quizlet.user.UserStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;

    /** Tài khoản chờ duyệt, cũ nhất trước. */
    @Transactional(readOnly = true)
    public List<UserResponse> listPending() {
        return userRepository.findByStatusOrderByCreatedAtAsc(UserStatus.PENDING).stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional
    public UserResponse approve(Long userId) {
        User user = findPending(userId);
        user.setStatus(UserStatus.ACTIVE);
        return UserResponse.from(user);
    }

    /**
     * Xoá hẳn tài khoản bị từ chối: tài khoản này chưa từng đăng nhập nên không có dữ liệu gì, và người dùng
     * dùng lại được email đó để đăng ký (vd. đăng ký lại làm Student).
     */
    @Transactional
    public void reject(Long userId) {
        userRepository.delete(findPending(userId));
    }

    private User findPending(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_NOT_FOUND));
        if (user.getStatus() != UserStatus.PENDING) {
            throw new BusinessException(ErrorCode.ADMIN_USER_NOT_PENDING);
        }
        return user;
    }
}
