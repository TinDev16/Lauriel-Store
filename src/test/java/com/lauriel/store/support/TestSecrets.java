package com.lauriel.store.support;

import java.security.SecureRandom;
import java.util.Base64;

public final class TestSecrets {

    private TestSecrets() {}

    public static String randomBase64() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
