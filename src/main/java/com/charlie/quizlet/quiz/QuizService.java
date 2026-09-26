package com.charlie.quizlet.quiz;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.charlie.quizlet.auth.CurrentUser;
import com.charlie.quizlet.common.SearchPatterns;
import com.charlie.quizlet.common.dto.PageRequests;
import com.charlie.quizlet.common.dto.PageResponse;
import com.charlie.quizlet.common.error.BusinessException;
import com.charlie.quizlet.common.error.ErrorCode;
import com.charlie.quizlet.quiz.dto.OptionRequest;
import com.charlie.quizlet.quiz.dto.QuestionRequest;
import com.charlie.quizlet.quiz.dto.QuizRequest;
import com.charlie.quizlet.quiz.dto.QuizResponse;
import com.charlie.quizlet.quiz.dto.QuizSummaryResponse;
import com.charlie.quizlet.user.User;
import com.charlie.quizlet.user.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Bộ đề trắc nghiệm. Chỉ TEACHER / ADMIN tạo được (chặn ở SecurityConfig); người soạn sửa / xoá đề của mình,
 * ADMIN sửa / xoá mọi đề. Nháp chỉ người sửa được mới thấy; người khác nhận QUIZ_NOT_FOUND (không lộ là có tồn tại).
 */
@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Transactional
    public QuizResponse create(CurrentUser user, QuizRequest request) {
        User owner = userRepository.findById(user.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND));

        Quiz quiz = new Quiz();
        quiz.setOwner(owner);
        applyRequest(quiz, request);
        return QuizResponse.from(quizRepository.saveAndFlush(quiz), true);
    }

    @Transactional(readOnly = true)
    public QuizResponse get(CurrentUser user, Long id) {
        Quiz quiz = findViewable(user, id);
        return QuizResponse.from(quiz, canEdit(quiz, user));
    }

    @Transactional
    public QuizResponse update(CurrentUser user, Long id, QuizRequest request) {
        Quiz quiz = findEditable(user, id);
        applyRequest(quiz, request);
        // Flush để câu hỏi / đáp án mới có id trước khi trả về.
        quizRepository.flush();
        return QuizResponse.from(quiz, true);
    }

    @Transactional
    public void delete(CurrentUser user, Long id) {
        quizRepository.delete(findEditable(user, id));
    }

    /** Bộ đề do người gọi soạn, cả nháp. */
    @Transactional(readOnly = true)
    public PageResponse<QuizSummaryResponse> listMine(CurrentUser user, String query, QuizSort sort, int page,
            int size) {
        return PageResponse.from(quizRepository.findOwnedSummaries(user.id(), SearchPatterns.contains(query),
                PageRequests.of(page, size, sort.sort())));
    }

    /** Bộ đề đã xuất bản của mọi người soạn — ai đăng nhập cũng xem được. */
    @Transactional(readOnly = true)
    public PageResponse<QuizSummaryResponse> listPublished(String query, QuizSort sort, int page, int size) {
        return PageResponse.from(quizRepository.findSummariesByStatus(QuizStatus.PUBLISHED,
                SearchPatterns.contains(query), PageRequests.of(page, size, sort.sort())));
    }

    private Quiz findViewable(CurrentUser user, Long id) {
        return quizRepository.findById(id)
                .filter(quiz -> quiz.getStatus() == QuizStatus.PUBLISHED || canEdit(quiz, user))
                .orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_NOT_FOUND));
    }

    /** Đề đã xuất bản của người khác: xem được nhưng không sửa / xoá được. */
    private Quiz findEditable(CurrentUser user, Long id) {
        Quiz quiz = findViewable(user, id);
        if (!canEdit(quiz, user)) {
            throw new BusinessException(ErrorCode.COMMON_FORBIDDEN);
        }
        return quiz;
    }

    private static boolean canEdit(Quiz quiz, CurrentUser user) {
        return user.isAdmin() || quiz.getOwner().getId().equals(user.id());
    }

    private void applyRequest(Quiz quiz, QuizRequest request) {
        request.questions().forEach(QuizService::validateQuestion);
        if (request.status() == QuizStatus.PUBLISHED && request.questions().isEmpty()) {
            throw new BusinessException(ErrorCode.QUIZ_EMPTY);
        }

        quiz.setTitle(request.title().trim());
        quiz.setDescription(blankToNull(request.description()));
        quiz.setTimeLimitMinutes(request.timeLimitMinutes());
        // Giữ thời điểm xuất bản lần đầu khi sửa đề đang xuất bản; về nháp thì xoá.
        if (request.status() == QuizStatus.PUBLISHED && quiz.getStatus() != QuizStatus.PUBLISHED) {
            quiz.setPublishedAt(clock.instant());
        } else if (request.status() == QuizStatus.DRAFT) {
            quiz.setPublishedAt(null);
        }
        quiz.setStatus(request.status());
        quiz.setUpdatedAt(clock.instant());
        replaceQuestions(quiz, request.questions());
    }

    /**
     * Câu hỏi / đáp án có id được giữ nguyên (chỉ cập nhật nội dung, vị trí) để bài làm sau này vẫn trỏ đúng câu
     * đã trả lời. Câu / đáp án cũ không còn trong danh sách bị xoá (orphanRemoval).
     */
    private static void replaceQuestions(Quiz quiz, List<QuestionRequest> requests) {
        Map<Long, Question> existing = byId(quiz.getQuestions(), Question::getId);
        List<Question> ordered = new ArrayList<>(requests.size());
        for (int i = 0; i < requests.size(); i++) {
            QuestionRequest request = requests.get(i);
            Question question = request.id() == null ? new Question() : takeExisting(existing, request.id());
            question.setQuiz(quiz);
            question.setPosition(i);
            question.setContent(request.content().trim());
            question.setExplanation(blankToNull(request.explanation()));
            replaceOptions(question, request.options());
            ordered.add(question);
        }
        quiz.getQuestions().clear();
        quiz.getQuestions().addAll(ordered);
    }

    private static void replaceOptions(Question question, List<OptionRequest> requests) {
        Map<Long, QuestionOption> existing = byId(question.getOptions(), QuestionOption::getId);
        List<QuestionOption> ordered = new ArrayList<>(requests.size());
        for (int i = 0; i < requests.size(); i++) {
            OptionRequest request = requests.get(i);
            QuestionOption option = request.id() == null ? new QuestionOption() : takeExisting(existing, request.id());
            option.setQuestion(question);
            option.setPosition(i);
            option.setContent(request.content().trim());
            option.setCorrect(request.correct());
            ordered.add(option);
        }
        question.getOptions().clear();
        question.getOptions().addAll(ordered);
    }

    /** Lấy ra (remove) để cùng 1 id gửi 2 lần thì lần sau báo lỗi; id của câu / đáp án khác cũng báo lỗi. */
    private static <T> T takeExisting(Map<Long, T> existing, Long id) {
        T item = existing.remove(id);
        if (item == null) {
            throw new BusinessException(ErrorCode.QUIZ_ITEM_NOT_FOUND);
        }
        return item;
    }

    private static <T> Map<Long, T> byId(List<T> items, Function<T, Long> id) {
        return items.stream().collect(Collectors.toMap(id, Function.identity()));
    }

    private static void validateQuestion(QuestionRequest question) {
        long correct = question.options().stream().filter(OptionRequest::correct).count();
        if (correct != 1) {
            throw new BusinessException(ErrorCode.QUIZ_CORRECT_OPTION_REQUIRED);
        }
        Set<String> seen = new HashSet<>();
        for (OptionRequest option : question.options()) {
            if (!seen.add(option.content().trim().toLowerCase(Locale.ROOT))) {
                throw new BusinessException(ErrorCode.QUIZ_DUPLICATE_OPTION);
            }
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
