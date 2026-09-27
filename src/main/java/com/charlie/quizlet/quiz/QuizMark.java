package com.charlie.quizlet.quiz;

import com.charlie.quizlet.auth.CurrentUser;

/** Lọc bộ đề theo quan hệ với người xem. */
public enum QuizMark {
    ALL,
    /** Chưa nộp lần nào. */
    NOT_TAKEN,
    /** Đã đánh dấu yêu thích. */
    FAVORITE;

    Long notTakenBy(CurrentUser user) {
        return this == NOT_TAKEN ? user.id() : null;
    }

    Long favoriteOf(CurrentUser user) {
        return this == FAVORITE ? user.id() : null;
    }
}
