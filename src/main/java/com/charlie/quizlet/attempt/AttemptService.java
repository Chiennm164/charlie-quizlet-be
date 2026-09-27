package com.charlie.quizlet.attempt;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.charlie.quizlet.attempt.dto.AnswerRequest;
import com.charlie.quizlet.attempt.dto.AttemptResponse;
import com.charlie.quizlet.attempt.dto.AttemptSummaryResponse;
import com.charlie.quizlet.attempt.dto.StartAttemptRequest;
import com.charlie.quizlet.auth.CurrentUser;
import com.charlie.quizlet.common.error.BusinessException;
import com.charlie.quizlet.common.error.ErrorCode;
import com.charlie.quizlet.quiz.Question;
import com.charlie.quizlet.quiz.QuestionOption;
import com.charlie.quizlet.quiz.Quiz;
import com.charlie.quizlet.quiz.QuizService;

import lombok.RequiredArgsConstructor;

/**
 * Làm bài. Server giữ đáp án đúng và giờ làm bài: FE chỉ nhận đáp án đúng khi đã được xem (xem AttemptResponse),
 * đếm ngược ở FE chỉ để hiển thị. Quá {@code deadline} (+ {@link #DEADLINE_GRACE} cho độ trễ mạng) thì không
 * lưu được câu trả lời nữa và lượt làm tự nộp ở lần gọi kế tiếp. Mỗi lượt chỉ người làm xem được — người khác
 * nhận ATTEMPT_NOT_FOUND (không lộ là có).
 */
@Service
@RequiredArgsConstructor
public class AttemptService {

    /** Chấp nhận câu trả lời / nộp bài gửi trễ tối đa chừng này sau deadline (mạng chậm, lệch đồng hồ). */
    static final Duration DEADLINE_GRACE = Duration.ofSeconds(30);

    private final QuizAttemptRepository attemptRepository;
    private final QuizService quizService;
    private final Clock clock;
    private final Random random = new Random();

    /**
     * Bắt đầu lượt mới. Lượt đang làm dở của bộ đề này (nếu có) bị huỷ — FE hỏi lại người dùng trước khi gọi; lượt dở
     * đã quá giờ thì được chấm như tự nộp.
     */
    @Transactional
    public AttemptResponse start(CurrentUser user, Long quizId, StartAttemptRequest request) {
        Quiz quiz = quizService.findViewable(user, quizId);
        List<Question> questions = request.retryWrongOf() == null
                ? new ArrayList<>(quiz.getQuestions())
                : wrongQuestionsOf(user, quizId, request.retryWrongOf());
        if (questions.isEmpty()) {
            throw new BusinessException(request.retryWrongOf() == null ? ErrorCode.QUIZ_EMPTY
                    : ErrorCode.ATTEMPT_NOTHING_TO_RETRY);
        }

        attemptRepository.findByUserIdAndQuizIdAndStatus(user.id(), quizId, AttemptStatus.IN_PROGRESS)
                .ifPresent(unfinished -> {
                    // Lượt đã quá giờ thì chấm (giữ kết quả); còn giờ thì huỷ.
                    if (!submitIfOverdue(unfinished)) {
                        attemptRepository.delete(unfinished);
                    }
                    // Ghi xuống DB trước khi thêm lượt mới: DB chỉ cho 1 lượt dở / người / bộ đề.
                    attemptRepository.flush();
                });

        Instant now = clock.instant();
        QuizAttempt attempt = new QuizAttempt();
        attempt.setQuiz(quiz);
        attempt.setUserId(user.id());
        attempt.setMode(request.mode());
        attempt.setPreview(user.isAdmin());
        attempt.setStatus(AttemptStatus.IN_PROGRESS);
        attempt.setStartedAt(now);
        if (request.mode() == AttemptMode.EXAM) {
            long seed = random.nextLong();
            attempt.setShuffleSeed(seed);
            Collections.shuffle(questions, new Random(seed));
            // Ngân hàng lớn: mỗi lượt thi thử chỉ lấy N câu đầu sau khi trộn (làm lại câu sai thì giữ đủ các câu sai).
            if (request.retryWrongOf() == null) {
                questions = new ArrayList<>(questions.subList(0, quizService.examDrawCount(quiz)));
            }
            if (quiz.getTimeLimitMinutes() != null) {
                attempt.setDeadline(now.plus(Duration.ofMinutes(quiz.getTimeLimitMinutes())));
            }
        }
        for (int i = 0; i < questions.size(); i++) {
            AttemptAnswer answer = new AttemptAnswer();
            answer.setAttempt(attempt);
            answer.setQuestion(questions.get(i));
            answer.setPosition(i);
            attempt.getAnswers().add(answer);
        }
        attempt.setQuestionCount(questions.size());
        return AttemptResponse.from(attemptRepository.saveAndFlush(attempt), now);
    }

    @Transactional
    public AttemptResponse get(CurrentUser user, Long id) {
        QuizAttempt attempt = findOwned(user, id);
        submitIfOverdue(attempt);
        return AttemptResponse.from(attempt, clock.instant());
    }

    /** Lịch sử làm 1 bộ đề (20 lượt gần nhất), gồm cả lượt đang làm dở. */
    @Transactional
    public List<AttemptSummaryResponse> history(CurrentUser user, Long quizId) {
        List<QuizAttempt> attempts = attemptRepository.findTop20ByUserIdAndQuizIdOrderByStartedAtDesc(user.id(),
                quizId);
        attempts.forEach(this::submitIfOverdue);
        return attempts.stream().map(AttemptSummaryResponse::from).toList();
    }

    /**
     * Lưu câu trả lời 1 câu. Luyện tập: chấm ngay, mỗi câu chọn 1 lần. Không rollback khi báo ATTEMPT_TIME_UP để
     * lượt vừa tự nộp vẫn được lưu.
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public AttemptResponse.QuestionView answer(CurrentUser user, Long id, Long questionId, AnswerRequest request) {
        QuizAttempt attempt = findOwned(user, id);
        if (submitIfOverdue(attempt)) {
            throw new BusinessException(ErrorCode.ATTEMPT_TIME_UP);
        }
        if (attempt.getStatus() == AttemptStatus.SUBMITTED) {
            throw new BusinessException(ErrorCode.ATTEMPT_ALREADY_SUBMITTED);
        }
        AttemptAnswer answer = attempt.getAnswers().stream()
                .filter(a -> a.getQuestion().getId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.ATTEMPT_INVALID_ANSWER));
        QuestionOption option = request.optionId() == null ? null
                : answer.getQuestion().getOptions().stream()
                        .filter(o -> o.getId().equals(request.optionId()))
                        .findFirst()
                        .orElseThrow(() -> new BusinessException(ErrorCode.ATTEMPT_INVALID_ANSWER));

        if (attempt.getMode() == AttemptMode.PRACTICE) {
            if (option == null) {
                throw new BusinessException(ErrorCode.ATTEMPT_INVALID_ANSWER);
            }
            // Gửi lại đúng đáp án đã chọn (bấm 2 lần, gửi lại khi mạng chập chờn) thì coi như thành công.
            if (answer.getOption() != null && !answer.getOption().getId().equals(option.getId())) {
                throw new BusinessException(ErrorCode.ATTEMPT_ANSWER_LOCKED);
            }
            answer.setCorrect(option.isCorrect());
        }
        if (answer.getOption() != option) {
            answer.setAnsweredAt(clock.instant());
        }
        answer.setOption(option);
        answer.setFlagged(request.flagged());
        return AttemptResponse.QuestionView.from(answer);
    }

    /** Nộp bài và chấm điểm. Gọi lại khi đã nộp (bấm 2 lần, vừa tự nộp vì hết giờ) trả về kết quả luôn. */
    @Transactional
    public AttemptResponse submit(CurrentUser user, Long id) {
        QuizAttempt attempt = findOwned(user, id);
        if (!submitIfOverdue(attempt) && attempt.getStatus() == AttemptStatus.IN_PROGRESS) {
            grade(attempt, clock.instant());
        }
        return AttemptResponse.from(attempt, clock.instant());
    }

    /** Chấm mọi lượt thi quá giờ của người dùng (trước khi đọc lịch sử / tiến độ để điểm luôn đúng). */
    @Transactional
    public void submitOverdue(Long userId) {
        attemptRepository.findByUserIdAndStatusAndDeadlineBefore(userId, AttemptStatus.IN_PROGRESS,
                clock.instant().minus(DEADLINE_GRACE)).forEach(this::submitIfOverdue);
    }

    private List<Question> wrongQuestionsOf(CurrentUser user, Long quizId, Long attemptId) {
        QuizAttempt previous = findOwned(user, attemptId);
        if (!previous.getQuiz().getId().equals(quizId) || previous.getStatus() != AttemptStatus.SUBMITTED) {
            throw new BusinessException(ErrorCode.ATTEMPT_NOT_FOUND);
        }
        return new ArrayList<>(previous.getAnswers().stream()
                .filter(a -> !Boolean.TRUE.equals(a.getCorrect()))
                .map(AttemptAnswer::getQuestion)
                .toList());
    }

    private QuizAttempt findOwned(CurrentUser user, Long id) {
        return attemptRepository.findById(id)
                .filter(attempt -> attempt.getUserId().equals(user.id()))
                .orElseThrow(() -> new BusinessException(ErrorCode.ATTEMPT_NOT_FOUND));
    }

    /** Quá giờ (kể cả thời gian ân hạn) mà chưa nộp -> nộp với các câu đã lưu, tính thời điểm nộp = deadline. */
    private boolean submitIfOverdue(QuizAttempt attempt) {
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS || attempt.getDeadline() == null
                || !clock.instant().isAfter(attempt.getDeadline().plus(DEADLINE_GRACE))) {
            return false;
        }
        grade(attempt, attempt.getDeadline());
        return true;
    }

    private static void grade(QuizAttempt attempt, Instant submittedAt) {
        int correct = 0;
        for (AttemptAnswer answer : attempt.getAnswers()) {
            boolean isCorrect = answer.getOption() != null && answer.getOption().isCorrect();
            answer.setCorrect(isCorrect);
            if (isCorrect) {
                correct++;
            }
        }
        attempt.setCorrectCount(correct);
        // Câu bị Admin xoá khỏi đề trong lúc làm cũng bị xoá khỏi lượt làm (khoá ngoại cascade) -> đếm lại.
        attempt.setQuestionCount(attempt.getAnswers().size());
        attempt.setSubmittedAt(submittedAt);
        attempt.setStatus(AttemptStatus.SUBMITTED);
    }
}
