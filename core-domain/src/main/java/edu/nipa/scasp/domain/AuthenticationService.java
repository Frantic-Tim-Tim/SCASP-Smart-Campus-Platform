package edu.nipa.scasp.domain;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class AuthenticationService {
    private final Map<String, Account> accounts = new HashMap<>();

    public AuthenticationService() {
    }

    public synchronized void register(User user, char[] password) {
        Objects.requireNonNull(user, "user");
        PasswordHasher.validate(password);
        if (accounts.containsKey(user.userId())) {
            throw new IllegalArgumentException("Account already exists: " + user.userId());
        }
        byte[] salt = PasswordHasher.newSalt();
        accounts.put(user.userId(), new Account(user, salt, PasswordHasher.hash(password, salt)));
    }

    public synchronized User authenticate(String userId, char[] password) {
        User.requireText(userId, "userId");
        Objects.requireNonNull(password, "password");
        Account account = accounts.get(userId);
        if (account == null || !PasswordHasher.matches(password, account.salt(),
                account.passwordHash()) || account.user().status() != UserStatus.ACTIVE) {
            throw new SecurityException("Invalid credentials");
        }
        return account.user();
    }

    private record Account(User user, byte[] salt, byte[] passwordHash) {
        private Account {
            salt = Arrays.copyOf(salt, salt.length);
            passwordHash = Arrays.copyOf(passwordHash, passwordHash.length);
        }

        @Override
        public byte[] salt() {
            return Arrays.copyOf(salt, salt.length);
        }

        @Override
        public byte[] passwordHash() {
            return Arrays.copyOf(passwordHash, passwordHash.length);
        }
    }
}
