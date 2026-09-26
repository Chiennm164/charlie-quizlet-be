-- Gỡ tính năng học phần / thẻ ghi nhớ (V7): app chỉ tập trung vào bộ đề trắc nghiệm.
DROP TABLE cards;
DROP TABLE study_sets;

DELETE FROM error_codes WHERE code IN ('STUDY_SET_NOT_FOUND', 'STUDY_SET_DUPLICATE_TERM', 'STUDY_SET_CARD_NOT_FOUND');
