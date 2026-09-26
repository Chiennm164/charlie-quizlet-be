package com.charlie.quizlet.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.charlie.quizlet.common.error.BusinessException;
import com.charlie.quizlet.common.error.ErrorCode;
import com.charlie.quizlet.user.Role;
import com.charlie.quizlet.user.User;
import com.charlie.quizlet.user.UserRepository;
import com.charlie.quizlet.user.UserResponse;
import com.charlie.quizlet.user.UserStatus;

class AdminUserServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final AdminUserService service = new AdminUserService(userRepository);

    @Test
    void listPendingReturnsPendingUsers() {
        given(userRepository.findByStatusOrderByCreatedAtAsc(UserStatus.PENDING))
                .willReturn(List.of(teacher(1L, UserStatus.PENDING)));

        List<UserResponse> res = service.listPending();

        assertThat(res).extracting(UserResponse::id).containsExactly(1L);
    }

    @Test
    void approveActivatesPendingUser() {
        User bob = teacher(1L, UserStatus.PENDING);
        given(userRepository.findById(1L)).willReturn(Optional.of(bob));

        UserResponse res = service.approve(1L);

        assertThat(bob.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(res.status()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void rejectDeletesPendingUser() {
        User bob = teacher(1L, UserStatus.PENDING);
        given(userRepository.findById(1L)).willReturn(Optional.of(bob));

        service.reject(1L);

        verify(userRepository).delete(bob);
    }

    @Test
    void approveOrRejectNonPendingUserFails() {
        given(userRepository.findById(1L)).willReturn(Optional.of(teacher(1L, UserStatus.ACTIVE)));

        assertErrorCode(() -> service.approve(1L), ErrorCode.ADMIN_USER_NOT_PENDING);
        assertErrorCode(() -> service.reject(1L), ErrorCode.ADMIN_USER_NOT_PENDING);
        verify(userRepository, never()).delete(any());
    }

    @Test
    void unknownUserIsNotFound() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertErrorCode(() -> service.approve(99L), ErrorCode.COMMON_NOT_FOUND);
    }

    private static void assertErrorCode(Runnable call, ErrorCode expected) {
        assertThatThrownBy(call::run)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }

    private static User teacher(Long id, UserStatus status) {
        User user = new User();
        user.setId(id);
        user.setEmail("bob@example.com");
        user.setFullName("Bob");
        user.setRole(Role.TEACHER);
        user.setStatus(status);
        return user;
    }
}
