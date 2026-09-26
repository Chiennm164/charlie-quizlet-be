package com.charlie.quizlet.studyset.dto;

import java.time.Instant;
import java.util.List;

import com.charlie.quizlet.studyset.StudySet;
import com.charlie.quizlet.studyset.StudySetVisibility;

/** @param cards theo đúng thứ tự trong học phần */
public record StudySetResponse(Long id, String title, String description, StudySetVisibility visibility,
        Owner owner, List<CardResponse> cards, Instant createdAt, Instant updatedAt) {

    public record Owner(Long id, String fullName) {
    }

    public static StudySetResponse from(StudySet set) {
        return new StudySetResponse(set.getId(), set.getTitle(), set.getDescription(), set.getVisibility(),
                new Owner(set.getOwner().getId(), set.getOwner().getFullName()),
                set.getCards().stream().map(CardResponse::from).toList(),
                set.getCreatedAt(), set.getUpdatedAt());
    }
}
