package kz.chitas.chess.utils;

import java.security.SecureRandom;

public class RandomStringUtil {
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_+=<>?";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private RandomStringUtil() {
    }

    public static String generate(int length) {
        if (length < 1) {
            throw new IllegalArgumentException("Must be above 0");
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARACTERS.charAt(SECURE_RANDOM.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }
}
