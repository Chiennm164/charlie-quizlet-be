package com.charlie.quizlet.studyset;

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

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.charlie.quizlet.common.dto.PageResponse;
import com.charlie.quizlet.common.error.BusinessException;
import com.charlie.quizlet.common.error.ErrorCode;
import com.charlie.quizlet.studyset.dto.CardRequest;
import com.charlie.quizlet.studyset.dto.CardResponse;
import com.charlie.quizlet.studyset.dto.StudySetRequest;
import com.charlie.quizlet.studyset.dto.StudySetResponse;
import com.charlie.quizlet.studyset.dto.StudySetSummaryResponse;
import com.charlie.quizlet.user.User;
import com.charlie.quizlet.user.UserRepository;

class StudySetServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final long OWNER_ID = 1L;
    private static final long OTHER_ID = 2L;

    private final StudySetRepository studySetRepository = mock(StudySetRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final StudySetService service = new StudySetService(studySetRepository, userRepository,
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void createTrimsFieldsAndKeepsCardOrder() {
        given(userRepository.findById(OWNER_ID)).willReturn(Optional.of(user(OWNER_ID)));
        given(studySetRepository.saveAndFlush(any())).willAnswer(inv -> inv.getArgument(0));

        service.create(OWNER_ID, request("  Tiếng Anh  ", "   ",
                new CardRequest(null, " apple ", " quả táo "), new CardRequest(null, "cat", "con mèo")));

        ArgumentCaptor<StudySet> saved = ArgumentCaptor.forClass(StudySet.class);
        verify(studySetRepository).saveAndFlush(saved.capture());
        StudySet set = saved.getValue();
        assertThat(set.getTitle()).isEqualTo("Tiếng Anh");
        assertThat(set.getDescription()).isNull();
        assertThat(set.getUpdatedAt()).isEqualTo(NOW);
        assertThat(set.getCards()).extracting(Card::getTerm).containsExactly("apple", "cat");
        assertThat(set.getCards()).extracting(Card::getDefinition).containsExactly("quả táo", "con mèo");
        assertThat(set.getCards()).extracting(Card::getPosition).containsExactly(0, 1);
        assertThat(set.getCards()).allMatch(card -> card.getStudySet() == set);
    }

    @Test
    void duplicateTermsIgnoringCaseAndSpacesAreRejected() {
        given(userRepository.findById(OWNER_ID)).willReturn(Optional.of(user(OWNER_ID)));

        assertErrorCode(() -> service.create(OWNER_ID, request("Set", null,
                new CardRequest(null, "Apple", "a"), new CardRequest(null, " apple ", "b"))),
                ErrorCode.STUDY_SET_DUPLICATE_TERM);
        verify(studySetRepository, never()).saveAndFlush(any());
    }

    @Test
    void privateSetOfSomeoneElseLooksNotFound() {
        stored(StudySetVisibility.PRIVATE);

        assertErrorCode(() -> service.get(OTHER_ID, 10L), ErrorCode.STUDY_SET_NOT_FOUND);
        assertErrorCode(() -> service.delete(OTHER_ID, 10L), ErrorCode.STUDY_SET_NOT_FOUND);
        assertThat(service.get(OWNER_ID, 10L).id()).isEqualTo(10L);
    }

    @Test
    void publicSetOfSomeoneElseIsReadOnly() {
        stored(StudySetVisibility.PUBLIC);

        assertThat(service.get(OTHER_ID, 10L).cards()).hasSize(2);
        assertErrorCode(() -> service.update(OTHER_ID, 10L, request("x", null,
                new CardRequest(null, "a", "b"), new CardRequest(null, "c", "d"))), ErrorCode.COMMON_FORBIDDEN);
        assertErrorCode(() -> service.delete(OTHER_ID, 10L), ErrorCode.COMMON_FORBIDDEN);
        verify(studySetRepository, never()).delete(any());
    }

    @Test
    void updateKeepsCardsWithIdAddsNewOnesAndDropsMissing() {
        StudySet set = stored(StudySetVisibility.PRIVATE);
        Card apple = set.getCards().get(0);

        StudySetResponse res = service.update(OWNER_ID, 10L, request("Renamed", "desc",
                new CardRequest(null, "dog", "con chó"), new CardRequest(101L, "apple", "trái táo")));

        assertThat(set.getCards()).hasSize(2);
        assertThat(set.getCards().get(1)).isSameAs(apple);
        assertThat(apple.getDefinition()).isEqualTo("trái táo");
        assertThat(apple.getPosition()).isEqualTo(1);
        assertThat(set.getCards().get(0).getId()).isNull();
        assertThat(res.cards()).extracting(CardResponse::term).containsExactly("dog", "apple");
        assertThat(res.title()).isEqualTo("Renamed");
    }

    @Test
    void updateRejectsUnknownOrRepeatedCardId() {
        stored(StudySetVisibility.PRIVATE);

        assertErrorCode(() -> service.update(OWNER_ID, 10L, request("x", null,
                new CardRequest(999L, "a", "b"), new CardRequest(null, "c", "d"))),
                ErrorCode.STUDY_SET_CARD_NOT_FOUND);
        assertErrorCode(() -> service.update(OWNER_ID, 10L, request("x", null,
                new CardRequest(101L, "a", "b"), new CardRequest(101L, "c", "d"))),
                ErrorCode.STUDY_SET_CARD_NOT_FOUND);
    }

    @Test
    void listMineClampsPagingAndBuildsEscapedPattern() {
        given(studySetRepository.findSummaries(eq(OWNER_ID), any(), any()))
                .willAnswer(inv -> new PageImpl<>(List.of(), inv.getArgument(2, Pageable.class), 0));

        PageResponse<StudySetSummaryResponse> res = service.listMine(OWNER_ID, "  50%_Off ", StudySetSort.TITLE, -3,
                999);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(studySetRepository).findSummaries(eq(OWNER_ID), eq("%50\\%\\_off%"), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(StudySetService.MAX_PAGE_SIZE);
        assertThat(pageable.getValue().getSort()).isEqualTo(StudySetSort.TITLE.sort());
        assertThat(res.totalElements()).isZero();
    }

    @Test
    void containsPatternEscapesBackslash() {
        assertThat(StudySetService.containsPattern(null)).isEqualTo("%%");
        assertThat(StudySetService.containsPattern("a\\b")).isEqualTo("%a\\\\b%");
    }

    @Test
    void ownerDeletesSet() {
        StudySet set = stored(StudySetVisibility.PRIVATE);

        service.delete(OWNER_ID, 10L);

        verify(studySetRepository).delete(set);
    }

    /** Học phần id 10 của OWNER_ID, có 2 thẻ id 101 (apple) và 102 (cat). */
    private StudySet stored(StudySetVisibility visibility) {
        StudySet set = new StudySet();
        set.setId(10L);
        set.setOwner(user(OWNER_ID));
        set.setTitle("Animals");
        set.setVisibility(visibility);
        set.getCards().add(card(set, 101L, 0, "apple", "quả táo"));
        set.getCards().add(card(set, 102L, 1, "cat", "con mèo"));
        given(studySetRepository.findById(10L)).willReturn(Optional.of(set));
        return set;
    }

    private static Card card(StudySet set, Long id, int position, String term, String definition) {
        Card card = new Card();
        card.setId(id);
        card.setStudySet(set);
        card.setPosition(position);
        card.setTerm(term);
        card.setDefinition(definition);
        return card;
    }

    private static StudySetRequest request(String title, String description, CardRequest... cards) {
        return new StudySetRequest(title, description, StudySetVisibility.PRIVATE, List.of(cards));
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
