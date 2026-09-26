package com.charlie.quizlet.quiz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.charlie.quizlet.auth.CurrentUser;
import com.charlie.quizlet.common.error.BusinessException;
import com.charlie.quizlet.common.error.ErrorCode;
import com.charlie.quizlet.quiz.dto.OptionRequest;
import com.charlie.quizlet.quiz.dto.QuestionRequest;
import com.charlie.quizlet.quiz.dto.QuizRequest;
import com.charlie.quizlet.quiz.dto.QuizResponse;
import com.charlie.quizlet.quiz.dto.QuizSummaryResponse;
import com.charlie.quizlet.quiz.dto.TopicQuizzesResponse;
import com.charlie.quizlet.topic.Topic;
import com.charlie.quizlet.topic.TopicService;
import com.charlie.quizlet.user.Role;
import com.charlie.quizlet.user.User;
import com.charlie.quizlet.user.UserRepository;

class QuizServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final CurrentUser AUTHOR = new CurrentUser(1L, Role.ADMIN);
    private static final CurrentUser OTHER_ADMIN = new CurrentUser(9L, Role.ADMIN);
    private static final CurrentUser STUDENT = new CurrentUser(3L, Role.STUDENT);

    private final QuizRepository quizRepository = mock(QuizRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final TopicService topicService = mock(TopicService.class);
    private final QuizService service = new QuizService(quizRepository, userRepository, topicService,
            Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void setUp() {
        given(topicService.find(5L)).willReturn(topic(5L, "Toán"));
        given(topicService.find(99L)).willThrow(new BusinessException(ErrorCode.TOPIC_NOT_FOUND));
    }

    @Test
    void createPublishedQuizKeepsOrderAndTrims() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L)));
        given(quizRepository.saveAndFlush(any())).willAnswer(inv -> inv.getArgument(0));

        QuizResponse res = service.create(AUTHOR, request(QuizStatus.PUBLISHED,
                question(null, " 2 + 2 = ? ", option(null, "3", false), option(null, " 4 ", true))));

        ArgumentCaptor<Quiz> saved = ArgumentCaptor.forClass(Quiz.class);
        verify(quizRepository).saveAndFlush(saved.capture());
        Quiz quiz = saved.getValue();
        assertThat(quiz.getPublishedAt()).isEqualTo(NOW);
        Question question = quiz.getQuestions().get(0);
        assertThat(question.getContent()).isEqualTo("2 + 2 = ?");
        assertThat(question.getExplanation()).isNull();
        assertThat(question.getOptions()).extracting(QuestionOption::getContent).containsExactly("3", "4");
        assertThat(question.getOptions()).extracting(QuestionOption::getPosition).containsExactly(0, 1);
        assertThat(question.getOptions().get(1).getQuestion()).isSameAs(question);
        assertThat(res.canEdit()).isTrue();
        assertThat(res.questions()).hasSize(1);
    }

    @Test
    void eachQuestionNeedsExactlyOneCorrectAnswer() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L)));

        assertErrorCode(() -> service.create(AUTHOR, request(QuizStatus.DRAFT,
                question(null, "Q", option(null, "a", false), option(null, "b", false)))),
                ErrorCode.QUIZ_CORRECT_OPTION_REQUIRED);
        assertErrorCode(() -> service.create(AUTHOR, request(QuizStatus.DRAFT,
                question(null, "Q", option(null, "a", true), option(null, "b", true)))),
                ErrorCode.QUIZ_CORRECT_OPTION_REQUIRED);
        verify(quizRepository, never()).saveAndFlush(any());
    }

    @Test
    void duplicateAnswersAreRejected() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L)));

        assertErrorCode(() -> service.create(AUTHOR, request(QuizStatus.DRAFT,
                question(null, "Q", option(null, "Paris", true), option(null, " paris ", false)))),
                ErrorCode.QUIZ_DUPLICATE_OPTION);
    }

    @Test
    void draftMayBeEmptyButPublishedMayNot() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L)));
        given(quizRepository.saveAndFlush(any())).willAnswer(inv -> inv.getArgument(0));

        assertThat(service.create(AUTHOR, request(QuizStatus.DRAFT)).questionCount()).isZero();
        assertErrorCode(() -> service.create(AUTHOR, request(QuizStatus.PUBLISHED)), ErrorCode.QUIZ_EMPTY);
    }

    @Test
    void draftIsHiddenFromStudentsButVisibleToEveryAdmin() {
        stored(QuizStatus.DRAFT);

        assertErrorCode(() -> service.get(STUDENT, 10L), ErrorCode.QUIZ_NOT_FOUND);
        assertThat(service.get(OTHER_ADMIN, 10L).canEdit()).isTrue();
    }

    @Test
    void publishedQuizHidesAnswersFromStudents() {
        stored(QuizStatus.PUBLISHED);

        QuizResponse res = service.get(STUDENT, 10L);
        assertThat(res.canEdit()).isFalse();
        assertThat(res.questions()).isNull();
        assertThat(res.questionCount()).isEqualTo(1);
        assertThat(res.topic().name()).isEqualTo("Toán");

        assertThat(service.get(AUTHOR, 10L).questions().get(0).options()).extracting(o -> o.correct())
                .containsExactly(false, true);
    }

    @Test
    void anyAdminEditsAndDeletesAnyQuiz() {
        Quiz quiz = stored(QuizStatus.PUBLISHED);

        service.update(OTHER_ADMIN, 10L, request(QuizStatus.PUBLISHED,
                question(null, "Q", option(null, "a", true), option(null, "b", false))));
        service.delete(OTHER_ADMIN, 10L);

        verify(quizRepository).delete(quiz);
    }

    @Test
    void unknownTopicIsRejected() {
        given(userRepository.findById(1L)).willReturn(Optional.of(user(1L)));

        assertErrorCode(() -> service.create(AUTHOR, new QuizRequest(99L, "Math", null, null, QuizStatus.DRAFT,
                List.of())), ErrorCode.TOPIC_NOT_FOUND);
        verify(quizRepository, never()).saveAndFlush(any());
    }

    @Test
    void byTopicListsLatestPublishedQuizzesPerTopic() {
        given(quizRepository.findTopicsHavingQuizzes(QuizStatus.PUBLISHED))
                .willReturn(List.of(topic(5L, "Toán"), topic(6L, "Văn")));
        given(quizRepository.findSummaries(eq(QuizStatus.PUBLISHED), eq(5L), eq("%%"), any()))
                .willReturn(new PageImpl<>(List.of(summary(1L)), PageRequest.of(0, 1), 9));
        given(quizRepository.findSummaries(eq(QuizStatus.PUBLISHED), eq(6L), eq("%%"), any()))
                .willReturn(new PageImpl<>(List.of(summary(2L)), PageRequest.of(0, 1), 1));

        List<TopicQuizzesResponse> res = service.listPublishedByTopic(999);

        assertThat(res).extracting(t -> t.topic().name()).containsExactly("Toán", "Văn");
        assertThat(res.get(0).totalQuizzes()).isEqualTo(9);
        assertThat(res.get(0).quizzes()).extracting(QuizSummaryResponse::id).containsExactly(1L);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(quizRepository).findSummaries(eq(QuizStatus.PUBLISHED), eq(5L), eq("%%"), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(QuizService.MAX_PER_TOPIC);
    }

    @Test
    void updateKeepsIdsSwitchesCorrectAnswerAndDropsMissing() {
        Quiz quiz = stored(QuizStatus.PUBLISHED);
        Question question = quiz.getQuestions().get(0);
        QuestionOption three = question.getOptions().get(0);

        service.update(AUTHOR, 10L, request(QuizStatus.PUBLISHED,
                question(null, "New question", option(null, "x", true), option(null, "y", false)),
                question(100L, "2 + 2 = ?", option(null, "5", false), option(101L, "3", true))));

        assertThat(quiz.getQuestions()).hasSize(2);
        assertThat(quiz.getQuestions().get(1)).isSameAs(question);
        assertThat(question.getPosition()).isEqualTo(1);
        assertThat(question.getOptions()).hasSize(2);
        assertThat(question.getOptions().get(1)).isSameAs(three);
        assertThat(three.isCorrect()).isTrue();
        assertThat(question.getOptions()).extracting(QuestionOption::getContent).containsExactly("5", "3");
        // Đang xuất bản -> giữ thời điểm xuất bản cũ.
        assertThat(quiz.getPublishedAt()).isEqualTo(Instant.parse("2025-12-01T00:00:00Z"));
    }

    @Test
    void unknownOrForeignIdsAreRejected() {
        stored(QuizStatus.DRAFT);

        assertErrorCode(() -> service.update(AUTHOR, 10L, request(QuizStatus.DRAFT,
                question(999L, "Q", option(null, "a", true), option(null, "b", false)))),
                ErrorCode.QUIZ_ITEM_NOT_FOUND);
        assertErrorCode(() -> service.update(AUTHOR, 10L, request(QuizStatus.DRAFT,
                question(100L, "Q", option(101L, "a", true), option(101L, "b", false)))),
                ErrorCode.QUIZ_ITEM_NOT_FOUND);
    }

    @Test
    void backToDraftClearsPublishedAt() {
        Quiz quiz = stored(QuizStatus.PUBLISHED);

        service.update(AUTHOR, 10L, request(QuizStatus.DRAFT));

        assertThat(quiz.getPublishedAt()).isNull();
        assertThat(quiz.getQuestions()).isEmpty();
    }

    /** Bộ đề id 10 của AUTHOR, 1 câu (id 100) với 2 đáp án: 101 "3" (sai), 102 "4" (đúng). */
    private Quiz stored(QuizStatus status) {
        Quiz quiz = new Quiz();
        quiz.setId(10L);
        quiz.setOwner(user(1L));
        quiz.setTopic(topic(5L, "Toán"));
        quiz.setTitle("Math");
        quiz.setStatus(status);
        quiz.setPublishedAt(status == QuizStatus.PUBLISHED ? Instant.parse("2025-12-01T00:00:00Z") : null);
        Question question = new Question();
        question.setId(100L);
        question.setQuiz(quiz);
        question.setContent("2 + 2 = ?");
        question.getOptions().add(storedOption(question, 101L, 0, "3", false));
        question.getOptions().add(storedOption(question, 102L, 1, "4", true));
        quiz.getQuestions().add(question);
        given(quizRepository.findById(10L)).willReturn(Optional.of(quiz));
        return quiz;
    }

    private static QuestionOption storedOption(Question question, Long id, int position, String content,
            boolean correct) {
        QuestionOption option = new QuestionOption();
        option.setId(id);
        option.setQuestion(question);
        option.setPosition(position);
        option.setContent(content);
        option.setCorrect(correct);
        return option;
    }

    private static QuizRequest request(QuizStatus status, QuestionRequest... questions) {
        return new QuizRequest(5L, "Math", null, 15, status, List.of(questions));
    }

    private static QuestionRequest question(Long id, String content, OptionRequest... options) {
        return new QuestionRequest(id, content, "  ", List.of(options));
    }

    private static OptionRequest option(Long id, String content, boolean correct) {
        return new OptionRequest(id, content, correct);
    }

    private static Topic topic(Long id, String name) {
        Topic topic = new Topic();
        topic.setId(id);
        topic.setName(name);
        return topic;
    }

    private static QuizSummaryResponse summary(Long id) {
        return new QuizSummaryResponse(id, "Quiz " + id, null, QuizStatus.PUBLISHED, null, 1, 5L, "Toán", "Admin",
                NOW, NOW);
    }

    private static User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setFullName("User " + id);
        return user;
    }

    private static void assertErrorCode(Runnable call, ErrorCode expected) {
        assertThatThrownBy(call::run)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }
}
