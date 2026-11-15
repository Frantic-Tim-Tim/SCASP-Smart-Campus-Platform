package edu.nipa.scasp.enterprise.security;

import java.security.GeneralSecurityException;
import java.security.Provider;
import java.security.SecureRandom;

import org.wildfly.security.WildFlyElytronProvider;
import org.wildfly.security.password.PasswordFactory;
import org.wildfly.security.password.interfaces.BCryptPassword;
import org.wildfly.security.password.spec.EncryptablePasswordSpec;
import org.wildfly.security.password.spec.IteratedSaltedPasswordAlgorithmSpec;
import org.wildfly.security.password.util.ModularCrypt;

public final class PasswordHashService {

    private static final int BCRYPT_COST = 12;

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final Provider PROVIDER =
            new WildFlyElytronProvider();

    private PasswordHashService() {
    }

    public static String hash(char[] password) {
        if (password == null || password.length < 12) {
            throw new IllegalArgumentException(
                    "Password must contain at least 12 characters"
            );
        }

        try {
            byte[] salt = new byte[BCryptPassword.BCRYPT_SALT_SIZE];
            RANDOM.nextBytes(salt);

            IteratedSaltedPasswordAlgorithmSpec algorithmSpec =
                    new IteratedSaltedPasswordAlgorithmSpec(
                            BCRYPT_COST,
                            salt
                    );

            EncryptablePasswordSpec passwordSpec =
                    new EncryptablePasswordSpec(
                            password,
                            algorithmSpec
                    );

            PasswordFactory passwordFactory =
                    PasswordFactory.getInstance(
                            BCryptPassword.ALGORITHM_BCRYPT,
                            PROVIDER
                    );

            BCryptPassword bcrypt =
                    (BCryptPassword) passwordFactory.generatePassword(
                            passwordSpec
                    );

            return ModularCrypt.encodeAsString(bcrypt);

        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "Unable to generate password hash",
                    exception
            );
        }
    }
}