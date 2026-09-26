package com.charlie.quizlet.topic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.charlie.quizlet.common.error.BusinessException;
import com.charlie.quizlet.common.error.ErrorCode;
import com.charlie.quizlet.quiz.QuizRepository;
import com.charlie.quizlet.topic.dto.TopicRequest;
import com.charlie.quizlet.topic.dto.TopicResponse;

class TopicServiceTest {

    private final TopicRepository topicRepository = mock(TopicRepository.class);
    private final QuizRepository quizRepository = mock(QuizRepository.class);
    private final TopicService service = new TopicService(topicRepository, quizRepository);

    @Test
    void createTrimsName() {
        given(topicRepository.saveAndFlush(any())).willAnswer(inv -> inv.getArgument(0));

        TopicResponse res = service.create(new TopicRequest("  Toán  "));

        ArgumentCaptor<Topic> saved = ArgumentCaptor.forClass(Topic.class);
        verify(topicRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getName()).isEqualTo("Toán");
        assertThat(res.quizCount()).isZero();
    }

    @Test
    void duplicateNameIsRejectedIgnoringCase() {
        given(topicRepository.existsByNameIgnoreCase("toán")).willReturn(true);
        given(topicRepository.findById(1L)).willReturn(Optional.of(topic(1L, "Văn")));
        given(topicRepository.existsByNameIgnoreCaseAndIdNot("Toán", 1L)).willReturn(true);

        assertErrorCode(() -> service.create(new TopicRequest("toán")), ErrorCode.TOPIC_NAME_TAKEN);
        assertErrorCode(() -> service.rename(1L, new TopicRequest("Toán")), ErrorCode.TOPIC_NAME_TAKEN);
        verify(topicRepository, never()).saveAndFlush(any());
    }

    @Test
    void renameKeepsIdAndReturnsQuizCount() {
        Topic topic = topic(1L, "Toan");
        given(topicRepository.findById(1L)).willReturn(Optional.of(topic));
        given(quizRepository.countByTopicId(1L)).willReturn(3L);

        TopicResponse res = service.rename(1L, new TopicRequest("Toán"));

        assertThat(topic.getName()).isEqualTo("Toán");
        assertThat(res.quizCount()).isEqualTo(3);
    }

    @Test
    void onlyEmptyTopicCanBeDeleted() {
        Topic topic = topic(1L, "Toán");
        given(topicRepository.findById(1L)).willReturn(Optional.of(topic));
        given(quizRepository.countByTopicId(1L)).willReturn(2L);

        assertErrorCode(() -> service.delete(1L), ErrorCode.TOPIC_IN_USE);
        verify(topicRepository, never()).delete(any());

        given(quizRepository.countByTopicId(1L)).willReturn(0L);
        service.delete(1L);
        verify(topicRepository).delete(topic);
    }

    @Test
    void unknownTopicIsNotFound() {
        given(topicRepository.findById(9L)).willReturn(Optional.empty());

        assertErrorCode(() -> service.find(9L), ErrorCode.TOPIC_NOT_FOUND);
    }

    private static Topic topic(Long id, String name) {
        Topic topic = new Topic();
        topic.setId(id);
        topic.setName(name);
        return topic;
    }

    private static void assertErrorCode(Runnable call, ErrorCode expected) {
        assertThatThrownBy(call::run)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }
}
