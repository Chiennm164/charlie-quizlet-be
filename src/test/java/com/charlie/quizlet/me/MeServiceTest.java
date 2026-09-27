package com.charlie.quizlet.me;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.charlie.quizlet.attempt.AttemptService;
import com.charlie.quizlet.auth.CurrentUser;
import com.charlie.quizlet.me.dto.QuizMarksResponse;
import com.charlie.quizlet.quiz.Quiz;
import com.charlie.quizlet.quiz.QuizService;
import com.charlie.quizlet.user.Role;

class MeServiceTest {

    private static final CurrentUser STUDENT = new CurrentUser(3L, Role.STUDENT);

    private final MeRepository meRepository = mock(MeRepository.class);
    private final QuizFavoriteRepository favoriteRepository = mock(QuizFavoriteRepository.class);
    private final AttemptService attemptService = mock(AttemptService.class);
    private final QuizService quizService = mock(QuizService.class);
    private final MeService service = new MeService(meRepository, favoriteRepository, attemptService, quizService);

    @Test
    void marksGradeOverdueFirstAndConvertBestScoreToPercent() {
        given(meRepository.progressByQuiz(3L)).willReturn(List.<Object[]>of(
                new Object[] { 10L, 2L, 0.666, null },
                new Object[] { 11L, 0L, null, 55L }));
        given(favoriteRepository.findQuizIdsByUserId(3L)).willReturn(List.of(11L));

        QuizMarksResponse marks = service.marks(STUDENT);

        verify(attemptService).submitOverdue(3L);
        assertThat(marks.progress()).containsExactly(
                new QuizMarksResponse.QuizProgress(10L, 2, 67, null),
                new QuizMarksResponse.QuizProgress(11L, 0, null, 55L));
        assertThat(marks.favoriteQuizIds()).containsExactly(11L);
    }

    @Test
    void addingAFavoriteTwiceKeepsOneRow() {
        given(quizService.findViewable(STUDENT, 10L)).willReturn(new Quiz());
        given(favoriteRepository.existsByUserIdAndQuizId(3L, 10L)).willReturn(false, true);

        service.addFavorite(STUDENT, 10L);
        service.addFavorite(STUDENT, 10L);

        verify(favoriteRepository).save(any());
        verify(favoriteRepository, never()).deleteByUserIdAndQuizId(any(), any());
    }
}
