package com.charlie.quizlet.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Token ngẫu nhiên gửi cho client (refresh token, link đặt lại mật khẩu). DB chỉ lưu hash SHA-256 của token,
 * nên lộ DB cũng không dùng được token; tra cứu bằng cách hash token client gửi lên rồi so với cột {@code token_hash}.
 */
public final class OpaqueTokens {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private OpaqueTokens() {
    }

    /** Token mới: 32 byte ngẫu nhiên dạng base64url (43 ký tự, đặt được vào URL). */
    public static String generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 dạng hex (64 ký tự) — giá trị lưu ở cột {@code token_hash}. */
    public static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
