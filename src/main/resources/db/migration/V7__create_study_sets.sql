-- Học phần (bộ thẻ) và thẻ. Thứ tự thẻ theo cột position (0, 1, 2...).
CREATE TABLE study_sets (
    id          BIGSERIAL PRIMARY KEY,
    owner_id    BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title       VARCHAR(255)  NOT NULL,
    description VARCHAR(2000),
    visibility  VARCHAR(20)   NOT NULL CHECK (visibility IN ('PUBLIC', 'PRIVATE')),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_study_sets_owner_id ON study_sets (owner_id);

-- Không đặt UNIQUE (study_set_id, position): khi sắp xếp lại, các thẻ đổi position cho nhau trong cùng 1 lần lưu.
CREATE TABLE cards (
    id           BIGSERIAL PRIMARY KEY,
    study_set_id BIGINT        NOT NULL REFERENCES study_sets (id) ON DELETE CASCADE,
    position     INT           NOT NULL,
    term         VARCHAR(500)  NOT NULL,
    definition   VARCHAR(2000) NOT NULL,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_cards_study_set_id ON cards (study_set_id);

INSERT INTO error_codes (code, http_status, message_vi, message_en, description_vi, description_en, note) VALUES
('STUDY_SET_NOT_FOUND', 404, 'Không tìm thấy học phần', 'Study set not found',
 'Học phần có thể đã bị xoá hoặc đang ở chế độ riêng tư.', 'The study set may have been deleted or is private.',
 'Không tồn tại, hoặc riêng tư và không phải của người xem (không để lộ là có tồn tại)'),
('STUDY_SET_DUPLICATE_TERM', 400, 'Có thuật ngữ bị trùng', 'Some terms are duplicated',
 'Mỗi thuật ngữ trong học phần chỉ được xuất hiện một lần.', 'Each term can appear only once in a study set.',
 'So sánh sau khi bỏ khoảng trắng đầu/cuối, không phân biệt hoa thường'),
('STUDY_SET_CARD_NOT_FOUND', 400, 'Thẻ không thuộc học phần này', 'A card does not belong to this study set',
 'Dữ liệu có thể vừa được thay đổi ở nơi khác. Vui lòng tải lại trang.', 'The data may have just been changed elsewhere. Please reload the page.',
 'PUT gửi card id không có trong học phần (đã bị xoá / của học phần khác)');
