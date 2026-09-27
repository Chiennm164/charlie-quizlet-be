package com.charlie.quizlet.attempt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.charlie.quizlet.config.AppProperties;
import com.charlie.quizlet.attempt.dto.AnswerRequest;
import com.charlie.quizlet.attempt.dto.AttemptResponse;
import com.charlie.quizlet.attempt.dto.StartAttemptRequest;
import com.charlie.quizlet.auth.CurrentUser;
import com.charlie.quizlet.common.error.BusinessException;
import com.charlie.quizlet.common.error.ErrorCode;
import com.charlie.quizlet.quiz.Question;
import com.charlie.quizlet.quiz.QuestionOption;
import com.charlie.quizlet.quiz.Quiz;
import com.charlie.quizlet.quiz.QuizRepository;
import com.charlie.quizlet.quiz.QuizService;
import com.charlie.quizlet.quiz.QuizStatus;
import com.charlie.quizlet.topic.Topic;
import com.charlie.quizlet.topic.TopicService;
import com.charlie.quizlet.user.Role;
import com.charlie.quizlet.user.UserRepository;

class AttemptServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final CurrentUser STUDENT = new CurrentUser(3L, Role.STUDENT);
    private static final CurrentUser OTHER = new CurrentUser(4L, Role.STUDENT);

    private final QuizAttemptRepository attemptRepository = mock(QuizAttemptRepository.class);
    private final QuizRepository quizRepository = mock(QuizRepository.class);
    private final MutableClock clock = new MutableClock(NOW);
    private final AttemptService service = new AttemptService(attemptRepository,
            new QuizService(quizRepository, mock(UserRepository.class), mock(TopicService.class), clock,
                    new AppProperties(null, null, null, null, null, new AppProperties.Attempt(30))),
            clock);

    @BeforeEach
    void setUp() {
        given(attemptRepository.saveAndFlush(any())).willAnswer(call -> {
            QuizAttempt attempt = call.getArgument(0);
            attempt.setId(50L);
            given(attemptRepository.findById(50L)).willReturn(Optional.of(attempt));
            return attempt;
        });
    }

    @Test
    void startHidesCorrectAnswersAndSetsDeadlineOnlyForTimedExam() {
        quiz(QuizStatus.PUBLISHED, 15);

        AttemptResponse exam = service.start(STUDENT, 10L, new StartAttemptRequest(AttemptMode.EXAM, null));
        assertThat(exam.deadline()).isEqualTo(NOW.plusSeconds(15 * 60));
        assertThat(exam.questions()).hasSize(3)
                .allSatisfy(q -> {
                    assertThat(q.correctOptionId()).isNull();
                    assertThat(q.explanation()).isNull();
                });
        assertThat(exam.questions()).extracting(AttemptResponse.QuestionView::questionId)
                .containsExactlyInAnyOrder(100L, 200L, 300L);

        AttemptResponse practice = service.start(STUDENT, 10L, new StartAttemptRequest(AttemptMode.PRACTICE, null));
        assertThat(practice.deadline()).isNull();
        assertThat(practice.questions()).extracting(AttemptResponse.QuestionView::questionId)
                .containsExactly(100L, 200L, 300L);
    }

    @Test
    void examDrawsTheConfiguredNumberOfRandomQuestionsWhilePracticeUsesTheWholeBank() {
        quiz(QuizStatus.PUBLISHED, null).setExamQuestionCount(2);

        AttemptResponse exam = service.start(STUDENT, 10L, new StartAttemptRequest(AttemptMode.EXAM, null));
        assertThat(exam.questionCount()).isEqualTo(2);
        assertThat(exam.questions()).extracting(AttemptResponse.QuestionView::questionId)
                .doesNotHaveDuplicates()
                .isSubsetOf(100L, 200L, 300L);

        AttemptResponse practice = service.start(STUDENT, 10L, new StartAttemptRequest(AttemptMode.PRACTICE, null));
        assertThat(practice.questionCount()).isEqualTo(3);
    }

    @Test
    void quizWithoutItsOwnCountUsesTheSystemDefault() {
        AttemptService withDefaultTwo = new AttemptService(attemptRepository,
                new QuizService(quizRepository, mock(UserRepository.class), mock(TopicService.class), clock,
                        new AppProperties(null, null, null, null, null, new AppProperties.Attempt(2))),
                clock);
        quiz(QuizStatus.PUBLISHED, null);

        AttemptResponse exam = withDefaultTwo.start(STUDENT, 10L, new StartAttemptRequest(AttemptMode.EXAM, null));
        assertThat(exam.questionCount()).isEqualTo(2);
    }

    @Test
    void draftQuizCannotBeStartedByStudentsAndStartingAgainDiscardsUnfinished() {
        quiz(QuizStatus.DRAFT, null);
        assertErrorCode(() -> service.start(STUDENT, 10L, new StartAttemptRequest(AttemptMode.EXAM, null)),
                ErrorCode.QUIZ_NOT_FOUND);

        quiz(QuizStatus.PUBLISHED, null);
        QuizAttempt unfinished = new QuizAttempt();
        given(attemptRepository.findByUserIdAndQuizIdAndStatus(3L, 10L, AttemptStatus.IN_PROGRESS))
                .willReturn(Optional.of(unfinished));
        service.start(STUDENT, 10L, new StartAttemptRequest(AttemptMode.EXAM, null));
        verify(attemptRepository).delete(unfinished);
    }

    @Test
    void examIsGradedOnlyOnSubmit() {
        quiz(QuizStatus.PUBLISHED, null);
        service.start(STUDENT, 10L, new StartAttemptRequest(AttemptMode.EXAM, null));

        AttemptResponse.QuestionView saved = service.answer(STUDENT, 50L, 100L, new AnswerRequest(102L, true));
        assertThat(saved.selectedOptionId()).isEqualTo(102L);
        assertThat(saved.flagged()).isTrue();
        assertThat(saved.correctOptionId()).isNull();
        // Thi thử được đổi / bỏ chọn trước khi nộp.
        service.answer(STUDENT, 50L, 200L, new AnswerRequest(201L, false));
        service.answer(STUDENT, 50L, 200L, new AnswerRequest(202L, false));

        AttemptResponse result = service.submit(STUDENT, 50L);
        assertThat(result.status()).isEqualTo(AttemptStatus.SUBMITTED);
        assertThat(result.correctCount()).isEqualTo(2);
        assertThat(result.submittedAt()).isEqualTo(NOW);
        assertThat(result.questions()).allSatisfy(q -> assertThat(q.correctOptionId()).isNotNull());
        assertThat(result.questions()).filteredOn(q -> q.questionId() == 300L).singleElement()
                .satisfies(q -> assertThat(q.correct()).isFalse());

        assertThat(service.submit(STUDENT, 50L).correctCount()).isEqualTo(2);
        assertErrorCode(() -> service.answer(STUDENT, 50L, 100L, new AnswerRequest(101L, false)),
                ErrorCode.ATTEMPT_ALREADY_SUBMITTED);
    }

    @Test
    void practiceRevealsAnswerAtOnceAndLocksIt() {
        quiz(QuizStatus.PUBLISHED, 15);
        service.start(STUDENT, 10L, new StartAttemptRequest(AttemptMode.PRACTICE, null));

        AttemptResponse.QuestionView wrong = service.answer(STUDENT, 50L, 100L, new AnswerRequest(101L, false));
        assertThat(wrong.correct()).isFalse();
        assertThat(wrong.correctOptionId()).isEqualTo(102L);
        assertThat(wrong.explanation()).isEqualTo("Giải thích 100");

        // Gửi lại đúng đáp án cũ: không lỗi; đổi đáp án: bị khoá.
        service.answer(STUDENT, 50L, 100L, new AnswerRequest(101L, false));
        assertErrorCode(() -> service.answer(STUDENT, 50L, 100L, new AnswerRequest(102L, false)),
                ErrorCode.ATTEMPT_ANSWER_LOCKED);
        assertErrorCode(() -> service.answer(STUDENT, 50L, 200L, new AnswerRequest(null, false)),
                ErrorCode.ATTEMPT_INVALID_ANSWER);
    }

    @Test
    void answersOutsideTheAttemptAreRejectedAndOthersCannotSeeIt() {
        quiz(QuizStatus.PUBLISHED, null);
        service.start(STUDENT, 10L, new StartAttemptRequest(AttemptMode.EXAM, null));

        assertErrorCode(() -> service.answer(STUDENT, 50L, 999L, new AnswerRequest(101L, false)),
                ErrorCode.ATTEMPT_INVALID_ANSWER);
        assertErrorCode(() -> service.answer(STUDENT, 50L, 100L, new AnswerRequest(201L, false)),
                ErrorCode.ATTEMPT_INVALID_ANSWER);
        assertErrorCode(() -> service.get(OTHER, 50L), ErrorCode.ATTEMPT_NOT_FOUND);
    }

    @Test
    void overdueExamIsSubmittedAutomaticallyAfterGracePeriod() {
        quiz(QuizStatus.PUBLISHED, 1);
        service.start(STUDENT, 10L, new StartAttemptRequest(AttemptMode.EXAM, null));
        service.answer(STUDENT, 50L, 100L, new AnswerRequest(102L, false));

        // Trong thời gian ân hạn vẫn lưu được.
        clock.set(NOW.plusSeconds(60).plus(AttemptService.DEADLINE_GRACE));
        service.answer(STUDENT, 50L, 200L, new AnswerRequest(202L, false));

        clock.set(NOW.plusSeconds(60).plus(AttemptService.DEADLINE_GRACE).plusSeconds(1));
        assertErrorCode(() -> service.answer(STUDENT, 50L, 300L, new AnswerRequest(302L, false)),
                ErrorCode.ATTEMPT_TIME_UP);
        AttemptResponse result = service.get(STUDENT, 50L);
        assertThat(result.status()).isEqualTo(AttemptStatus.SUBMITTED);
        assertThat(result.submittedAt()).isEqualTo(NOW.plusSeconds(60));
        assertThat(result.correctCount()).isEqualTo(2);
    }

    @Test
    void startingAgainGradesAnOverdueUnfinishedExamInsteadOfDiscardingIt() {
        quiz(QuizStatus.PUBLISHED, 1);
        service.start(STUDENT, 10L, new StartAttemptRequest(AttemptMode.EXAM, null));
        service.answer(STUDENT, 50L, 100L, new AnswerRequest(102L, false));
        QuizAttempt overdue = attemptRepository.findById(50L).orElseThrow();
        given(attemptRepository.findByUserIdAndQuizIdAndStatus(3L, 10L, AttemptStatus.IN_PROGRESS))
                .willReturn(Optional.of(overdue));

        clock.set(NOW.plusSeconds(60).plus(AttemptService.DEADLINE_GRACE).plusSeconds(1));
        willAnswer(call -> call.getArgument(0)).given(attemptRepository).saveAndFlush(any());
        service.start(STUDENT, 10L, new StartAttemptRequest(AttemptMode.EXAM, null));

        assertThat(overdue.getStatus()).isEqualTo(AttemptStatus.SUBMITTED);
        assertThat(overdue.getCorrectCount()).isEqualTo(1);
        verify(attemptRepository, never()).delete(overdue);
    }

    @Test
    void retryAsksOnlyWrongAndSkippedQuestions() {
        quiz(QuizStatus.PUBLISHED, null);
        service.start(STUDENT, 10L, new StartAttemptRequest(AttemptMode.PRACTICE, null));
        service.answer(STUDENT, 50L, 100L, new AnswerRequest(102L, false));
        service.answer(STUDENT, 50L, 200L, new AnswerRequest(201L, false));
        QuizAttempt first = attemptRepository.findById(50L).orElseThrow();
        assertErrorCode(() -> service.start(STUDENT, 10L, new StartAttemptRequest(AttemptMode.EXAM, 50L)),
                ErrorCode.ATTEMPT_NOT_FOUND);
        service.submit(STUDENT, 50L);

        // Lượt làm lại giữ id khác, không đè lượt 50 trong mock.
        willAnswer(call -> call.getArgument(0)).given(attemptRepository).saveAndFlush(any());
        AttemptResponse retry = service.start(STUDENT, 10L, new StartAttemptRequest(AttemptMode.PRACTICE, 50L));
        assertThat(retry.questions()).extracting(AttemptResponse.QuestionView::questionId)
                .containsExactly(200L, 300L);

        first.getAnswers().forEach(a -> a.setCorrect(true));
        assertErrorCode(() -> service.start(STUDENT, 10L, new StartAttemptRequest(AttemptMode.PRACTICE, 50L)),
                ErrorCode.ATTEMPT_NOTHING_TO_RETRY);
    }

    /** Bộ đề id 10, 3 câu (id 100, 200, 300), mỗi câu 2 đáp án: id+1 sai, id+2 đúng. */
    private Quiz quiz(QuizStatus status, Integer timeLimitMinutes) {
        Topic topic = new Topic();
        topic.setId(5L);
        topic.setName("Toán");
        Quiz quiz = new Quiz();
        quiz.setId(10L);
        quiz.setTopic(topic);
        quiz.setTitle("Math");
        quiz.setStatus(status);
        quiz.setTimeLimitMinutes(timeLimitMinutes);
        for (long id : List.of(100L, 200L, 300L)) {
            Question question = new Question();
            question.setId(id);
            question.setQuiz(quiz);
            question.setContent("Câu " + id);
            question.setExplanation("Giải thích " + id);
            question.getOptions().add(option(question, id + 1, false));
            question.getOptions().add(option(question, id + 2, true));
            quiz.getQuestions().add(question);
        }
        given(quizRepository.findById(10L)).willReturn(Optional.of(quiz));
        return quiz;
    }

    private static QuestionOption option(Question question, Long id, boolean correct) {
        QuestionOption option = new QuestionOption();
        option.setId(id);
        option.setQuestion(question);
        option.setContent("Đáp án " + id);
        option.setCorrect(correct);
        return option;
    }

    private static void assertErrorCode(Runnable call, ErrorCode expected) {
        assertThatThrownBy(call::run)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }

    /** Đồng hồ chỉnh được giờ để thử hết giờ làm bài. */
    private static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void set(Instant now) {
            this.now = now;
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
