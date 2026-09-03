package app.dam.room;

import java.security.SecureRandom;
import java.util.random.RandomGenerator;

/**
 * 코드는 사람이 불러 주기도 한다. O 와 0, I 와 1 은 듣고 옮겨 적을 때 섞여서 뺐다.
 * 프론트의 입력 필드도 같은 문자집합을 쓴다.
 */
public final class InviteCodes {

    static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    static final int LENGTH = 6;

    private static final RandomGenerator RANDOM = new SecureRandom();

    private InviteCodes() {
    }

    public static String next() {
        return next(RANDOM);
    }

    static String next(RandomGenerator random) {
        StringBuilder code = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }

    /** 사용자가 소문자로 적거나 앞뒤 공백을 붙여 붙여넣는 일이 잦다. */
    public static String normalize(String raw) {
        return raw == null ? "" : raw.strip().toUpperCase();
    }

    public static boolean looksValid(String code) {
        if (code == null || code.length() != LENGTH) {
            return false;
        }
        return code.chars().allMatch(c -> ALPHABET.indexOf(c) >= 0);
    }
}
