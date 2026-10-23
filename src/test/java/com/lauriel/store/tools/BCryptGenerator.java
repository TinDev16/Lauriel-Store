package com.lauriel.store.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Local-only helper for provisioning a password hash; run this main method from IntelliJ. */
public final class BCryptGenerator {

    private BCryptGenerator() {}

    public static void main(String[] args) throws IOException {
        System.out.print("Password to hash: ");
        String password = new BufferedReader(
            new InputStreamReader(System.in, StandardCharsets.UTF_8)
        ).readLine();
        if (
            password == null ||
            password.length() < 8 ||
            password.getBytes(StandardCharsets.UTF_8).length > 72
        ) {
            throw new IllegalArgumentException(
                "Password must be at least 8 characters and at most 72 UTF-8 bytes"
            );
        }
        System.out.println(new BCryptPasswordEncoder().encode(password));
    }
}
