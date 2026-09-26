package com.charlie.quizlet.common;

import java.util.Locale;

/** Pattern cho câu query {@code lower(x) like :pattern escape '\'}. */
public final class SearchPatterns {

    /**
     * "Chứa chuỗi", không phân biệt hoa thường; rỗng / null = khớp tất cả. Escape %, _ và dấu \ để người dùng gõ
     * các ký tự này được tìm đúng nghĩa đen.
     */
    public static String contains(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return "%" + q.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }

    private SearchPatterns() {
    }
}
