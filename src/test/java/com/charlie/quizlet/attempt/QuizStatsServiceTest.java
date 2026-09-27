package com.charlie.quizlet.attempt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.charlie.quizlet.config.AppProperties;
import com.charlie.quizlet.attempt.dto.QuizStatsResponse;
import com.charlie.quizlet.common.error.BusinessException;
import com.charlie.quizlet.quiz.Question;
import com.charlie.quizlet.quiz.QuestionOption;
import com.charlie.quizlet.quiz.Quiz;
import com.charlie.quizlet.quiz.QuizRepository;
import com.charlie.quizlet.quiz.QuizService;
import com.charlie.quizlet.topic.TopicService;
import com.charlie.quizlet.user.UserRepository;

class QuizStatsServiceTest {

    private final QuizRepository quizRepository = mock(QuizRepository.class);
    private final AttemptStatsRepository statsRepository = mock(AttemptStatsRepository.class);
    private final QuizStatsService service = new QuizStatsService(new QuizService(quizRepository,
            mock(UserRepository.class), mock(TopicService.class), Clock.systemUTC(), new AppProperties(null, null, null, null, null, new AppProperties.Attempt(30))), statsRepository);

    @Test
    void combinesCountsPerQuestionAndOptionInQuizOrder() {
        quiz();
        given(statsRepository.summary(10L)).willReturn(List.<Object[]>of(new Object[] { 4L, 3L, 0.625 }));
        given(statsRepository.countByMode(10L)).willReturn(List.<Object[]>of(new Object[] { AttemptMode.EXAM, 3L },
                new Object[] { AttemptMode.PRACTICE, 1L }));
        given(statsRepository.countByQuestion(10L)).willReturn(List.<Object[]>of(new Object[] { 100L, 4L, 3L, 1L }));
        given(statsRepository.countByOption(10L)).willReturn(List.<Object[]>of(new Object[] { 102L, 3L }));

        QuizStatsResponse stats = service.get(10L);

        assertThat(stats.attemptCount()).isEqualTo(4);
        assertThat(stats.takerCount()).isEqualTo(3);
        assertThat(stats.examCount()).isEqualTo(3);
        assertThat(stats.practiceCount()).isEqualTo(1);
        assertThat(stats.averagePercent()).isEqualTo(62.5);
        assertThat(stats.questions()).extracting(QuizStatsResponse.QuestionStats::questionId)
                .containsExactly(100L, 200L);
        QuizStatsResponse.QuestionStats first = stats.questions().getFirst();
        assertThat(first.answeredCount()).isEqualTo(4);
        assertThat(first.correctCount()).isEqualTo(3);
        assertThat(first.skippedCount()).isEqualTo(1);
        assertThat(first.options()).extracting(QuizStatsResponse.OptionStats::pickCount).containsExactly(0L, 3L);
        // Câu chưa ai làm: toàn số 0.
        assertThat(stats.questions().get(1).answeredCount()).isZero();
    }

    @Test
    void noAttemptsYetHasNoAverage() {
        quiz();
        given(statsRepository.summary(10L)).willReturn(List.<Object[]>of(new Object[] { 0L, 0L, null }));

        assertThat(service.get(10L).averagePercent()).isNull();
    }

    @Test
    void unknownQuizIsRejected() {
        given(quizRepository.findById(99L)).willReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(99L)).isInstanceOf(BusinessException.class);
    }

    private void quiz() {
        Quiz quiz = new Quiz();
        quiz.setId(10L);
        quiz.setTitle("Math");
        for (long id : List.of(100L, 200L)) {
            Question question = new Question();
            question.setId(id);
            question.setContent("Câu " + id);
            for (long optionId : List.of(id + 1, id + 2)) {
                QuestionOption option = new QuestionOption();
                option.setId(optionId);
                option.setContent("Đáp án " + optionId);
                option.setCorrect(optionId == id + 2);
                question.getOptions().add(option);
            }
            quiz.getQuestions().add(question);
        }
        given(quizRepository.findById(10L)).willReturn(Optional.of(quiz));
    }
}
