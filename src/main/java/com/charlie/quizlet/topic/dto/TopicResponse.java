package com.charlie.quizlet.topic.dto;

/** @param quizCount số bộ đề thuộc chủ đề (cả nháp) — Admin xem trước khi xoá */
public record TopicResponse(Long id, String name, long quizCount) {
}
