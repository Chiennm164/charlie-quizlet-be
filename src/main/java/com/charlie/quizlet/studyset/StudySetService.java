package com.charlie.quizlet.studyset;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.charlie.quizlet.common.dto.PageResponse;
import com.charlie.quizlet.common.error.BusinessException;
import com.charlie.quizlet.common.error.ErrorCode;
import com.charlie.quizlet.studyset.dto.CardRequest;
import com.charlie.quizlet.studyset.dto.StudySetRequest;
import com.charlie.quizlet.studyset.dto.StudySetResponse;
import com.charlie.quizlet.studyset.dto.StudySetSummaryResponse;
import com.charlie.quizlet.user.User;
import com.charlie.quizlet.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StudySetService {

    static final int MAX_PAGE_SIZE = 50;

    private final StudySetRepository studySetRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Transactional
    public StudySetResponse create(Long userId, StudySetRequest request) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_USER_NOT_FOUND));

        StudySet set = new StudySet();
        set.setOwner(owner);
        applyRequest(set, request);
        return StudySetResponse.from(studySetRepository.saveAndFlush(set));
    }

    /**
     * Học phần của user, tìm theo tiêu đề (không phân biệt hoa thường). Số trang / cỡ trang ngoài khoảng hợp lệ
     * được kéo về giới hạn thay vì báo lỗi.
     */
    @Transactional(readOnly = true)
    public PageResponse<StudySetSummaryResponse> listMine(Long userId, String query, StudySetSort sort, int page,
            int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE), sort.sort());
        return PageResponse.from(studySetRepository.findSummaries(userId, containsPattern(query), pageable));
    }

    @Transactional(readOnly = true)
    public StudySetResponse get(Long userId, Long id) {
        return StudySetResponse.from(findViewable(userId, id));
    }

    @Transactional
    public StudySetResponse update(Long userId, Long id, StudySetRequest request) {
        StudySet set = findOwned(userId, id);
        applyRequest(set, request);
        // Flush để thẻ mới có id trước khi trả về.
        studySetRepository.flush();
        return StudySetResponse.from(set);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        studySetRepository.delete(findOwned(userId, id));
    }

    /** Riêng tư mà không phải chủ: báo không tồn tại, không để lộ là có học phần này. */
    private StudySet findViewable(Long userId, Long id) {
        return studySetRepository.findById(id)
                .filter(set -> set.getVisibility() == StudySetVisibility.PUBLIC || isOwner(set, userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDY_SET_NOT_FOUND));
    }

    /** Học phần công khai của người khác: xem được nhưng không sửa / xoá được. */
    private StudySet findOwned(Long userId, Long id) {
        StudySet set = findViewable(userId, id);
        if (!isOwner(set, userId)) {
            throw new BusinessException(ErrorCode.COMMON_FORBIDDEN);
        }
        return set;
    }

    private static boolean isOwner(StudySet set, Long userId) {
        return set.getOwner().getId().equals(userId);
    }

    private void applyRequest(StudySet set, StudySetRequest request) {
        ensureUniqueTerms(request.cards());
        set.setTitle(request.title().trim());
        set.setDescription(request.description() == null || request.description().isBlank()
                ? null : request.description().trim());
        set.setVisibility(request.visibility());
        set.setUpdatedAt(clock.instant());
        replaceCards(set, request.cards());
    }

    /**
     * Thẻ có id được giữ nguyên (chỉ cập nhật nội dung / vị trí) để không mất dữ liệu gắn với thẻ, vd. tiến độ ôn
     * tập sau này. Thẻ cũ không còn trong danh sách bị xoá (orphanRemoval).
     */
    private void replaceCards(StudySet set, List<CardRequest> requests) {
        Map<Long, Card> existing = set.getCards().stream()
                .collect(Collectors.toMap(Card::getId, Function.identity()));

        List<Card> ordered = new ArrayList<>(requests.size());
        for (int i = 0; i < requests.size(); i++) {
            CardRequest request = requests.get(i);
            Card card;
            if (request.id() == null) {
                card = new Card();
                card.setStudySet(set);
            } else {
                // remove: cùng 1 id gửi 2 lần thì lần sau không còn trong map -> lỗi.
                card = existing.remove(request.id());
                if (card == null) {
                    throw new BusinessException(ErrorCode.STUDY_SET_CARD_NOT_FOUND);
                }
            }
            card.setPosition(i);
            card.setTerm(request.term().trim());
            card.setDefinition(request.definition().trim());
            ordered.add(card);
        }

        set.getCards().clear();
        set.getCards().addAll(ordered);
    }

    /** Pattern LIKE "chứa chuỗi": escape %, _ và dấu \ để người dùng gõ các ký tự này được tìm đúng nghĩa đen. */
    static String containsPattern(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return "%" + q.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }

    /** Trùng sau khi bỏ khoảng trắng đầu / cuối, không phân biệt hoa thường. */
    private static void ensureUniqueTerms(List<CardRequest> cards) {
        Set<String> seen = new HashSet<>();
        for (CardRequest card : cards) {
            if (!seen.add(card.term().trim().toLowerCase(Locale.ROOT))) {
                throw new BusinessException(ErrorCode.STUDY_SET_DUPLICATE_TERM);
            }
        }
    }
}
