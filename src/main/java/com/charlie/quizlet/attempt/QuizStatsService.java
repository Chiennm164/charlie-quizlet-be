package com.charlie.quizlet.attempt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.charlie.quizlet.attempt.dto.QuizStatsResponse;
import com.charlie.quizlet.attempt.dto.QuizStatsResponse.OptionStats;
import com.charlie.quizlet.attempt.dto.QuizStatsResponse.QuestionStats;
import com.charlie.quizlet.quiz.Question;
import com.charlie.quizlet.quiz.Quiz;
import com.charlie.quizlet.quiz.QuizService;

import lombok.RequiredArgsConstructor;

/** Thống kê bộ đề cho Admin (chặn ở SecurityConfig qua /api/admin/**): lượt làm, điểm trung bình, từng câu / đáp án. */
@Service
@RequiredArgsConstructor
public class QuizStatsService {

    /** Câu chưa có lượt nào làm: [id, số lượt, đúng, bỏ trống] đều 0. */
    private static final Object[] NO_ANSWERS = { null, 0L, 0L, 0L };

    private final QuizService quizService;
    private final AttemptStatsRepository statsRepository;

    @Transactional(readOnly = true)
    public QuizStatsResponse get(Long quizId) {
        Quiz quiz = quizService.find(quizId);

        Object[] summary = statsRepository.summary(quizId).getFirst();
        Map<AttemptMode, Long> byMode = new HashMap<>();
        statsRepository.countByMode(quizId).forEach(row -> byMode.put((AttemptMode) row[0], (Long) row[1]));
        Map<Long, Object[]> byQuestion = new HashMap<>();
        statsRepository.countByQuestion(quizId).forEach(row -> byQuestion.put((Long) row[0], row));
        Map<Long, Long> byOption = new HashMap<>();
        statsRepository.countByOption(quizId).forEach(row -> byOption.put((Long) row[0], (Long) row[1]));

        List<QuestionStats> questions = quiz.getQuestions().stream()
                .map(question -> questionStats(question, byQuestion.getOrDefault(question.getId(), NO_ANSWERS),
                        byOption))
                .toList();
        Double average = summary[2] == null ? null : ((Number) summary[2]).doubleValue() * 100;
        return new QuizStatsResponse(quiz.getId(), quiz.getTitle(), (Long) summary[0], (Long) summary[1],
                byMode.getOrDefault(AttemptMode.PRACTICE, 0L), byMode.getOrDefault(AttemptMode.EXAM, 0L), average,
                questions);
    }

    private static QuestionStats questionStats(Question question, Object[] counts, Map<Long, Long> byOption) {
        List<OptionStats> options = question.getOptions().stream()
                .map(o -> new OptionStats(o.getId(), o.getContent(), o.isCorrect(), byOption.getOrDefault(o.getId(), 0L)))
                .toList();
        return new QuestionStats(question.getId(), question.getPosition(), question.getContent(),
                count(counts[1]), count(counts[2]), count(counts[3]), options);
    }

    /** count(...) / sum(...) trả về Long hoặc Integer tuỳ biểu thức. */
    private static long count(Object value) {
        return ((Number) value).longValue();
    }
}
