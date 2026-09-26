package com.charlie.quizlet.studyset.dto;

import com.charlie.quizlet.studyset.Card;

public record CardResponse(Long id, String term, String definition) {

    public static CardResponse from(Card card) {
        return new CardResponse(card.getId(), card.getTerm(), card.getDefinition());
    }
}
