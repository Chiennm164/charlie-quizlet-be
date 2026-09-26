package com.charlie.quizlet.topic;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.charlie.quizlet.common.error.BusinessException;
import com.charlie.quizlet.common.error.ErrorCode;
import com.charlie.quizlet.quiz.QuizRepository;
import com.charlie.quizlet.topic.dto.TopicRequest;
import com.charlie.quizlet.topic.dto.TopicResponse;

import lombok.RequiredArgsConstructor;

/** Chủ đề: ai đăng nhập cũng xem được; chỉ ADMIN tạo / đổi tên / xoá (API dưới /api/admin, SecurityConfig). */
@Service
@RequiredArgsConstructor
public class TopicService {

    private final TopicRepository topicRepository;
    private final QuizRepository quizRepository;

    /** Theo tên A → Z. */
    @Transactional(readOnly = true)
    public List<TopicResponse> list() {
        return topicRepository.findAllWithQuizCount();
    }

    @Transactional
    public TopicResponse create(TopicRequest request) {
        String name = request.name().trim();
        if (topicRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessException(ErrorCode.TOPIC_NAME_TAKEN);
        }
        Topic topic = new Topic();
        topic.setName(name);
        topicRepository.saveAndFlush(topic);
        return new TopicResponse(topic.getId(), topic.getName(), 0);
    }

    @Transactional
    public TopicResponse rename(Long id, TopicRequest request) {
        Topic topic = find(id);
        String name = request.name().trim();
        if (topicRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new BusinessException(ErrorCode.TOPIC_NAME_TAKEN);
        }
        topic.setName(name);
        return new TopicResponse(topic.getId(), topic.getName(), quizRepository.countByTopicId(id));
    }

    /** Chỉ xoá được chủ đề trống — không tự xoá / chuyển bộ đề của người khác theo. */
    @Transactional
    public void delete(Long id) {
        Topic topic = find(id);
        if (quizRepository.countByTopicId(id) > 0) {
            throw new BusinessException(ErrorCode.TOPIC_IN_USE);
        }
        topicRepository.delete(topic);
    }

    public Topic find(Long id) {
        return topicRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.TOPIC_NOT_FOUND));
    }
}
