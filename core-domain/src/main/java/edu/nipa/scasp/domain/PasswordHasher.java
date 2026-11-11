package edu.nipa.scasp.domain;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordHasher {
    private static final int ITERATIONS = 120_000;
    private static final int KEY_BITS = 256;
    private static final int SALT_BYTES = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    public static byte[] newSalt() {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        return salt;
    }

    public static byte[] hash(char[] password, byte[] salt) {
        validate(password);
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Password hashing is unavailable", exception);
        } finally {
            spec.clearPassword();
        }
    }

    public static boolean matches(char[] password, byte[] salt, byte[] expectedHash) {
        if (password == null || password.length < 12 || salt == null || expectedHash == null) {
            return false;
        }
        return MessageDigest.isEqual(expectedHash, hash(password, salt));
    }

    public static void validate(char[] password) {
        if (password == null || password.length < 12) {
            throw new IllegalArgumentException("Password must contain at least 12 characters");
        }
    }
}
