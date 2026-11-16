package edu.nipa.scasp.rmi.server;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RmiSessionManager {

    private final SecureRandom random = new SecureRandom();
    private final Map<String, String> sessions = new ConcurrentHashMap<>();

    public String createSession(String administratorId) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);

        String token = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);

        sessions.put(token, administratorId);
        return token;
    }

    public String requireAdministrator(String token) {
        if (token == null || token.isBlank()) {
            throw new SecurityException("Authentication required.");
        }

        String administratorId = sessions.get(token);

        if (administratorId == null) {
            throw new SecurityException("Invalid or expired session.");
        }

        return administratorId;
    }

    public void invalidateSession(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }
}
