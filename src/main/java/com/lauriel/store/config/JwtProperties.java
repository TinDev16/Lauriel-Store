package com.lauriel.store.config;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.ConfigurationProperties;

// No generated toString: this configuration contains signing key material.
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    private String secret;
    private Duration expiration = Duration.ofHours(1);
    private SecretKey secretKey;

    @PostConstruct
    public void validate() {
        byte[] key;
        try {
            key = Base64.getDecoder().decode(secret == null ? "" : secret);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("JWT_SECRET phải là Base64 hợp lệ chứa ít nhất 32 byte ngẫu nhiên");
        }
        if (key.length < 32) {
            throw new IllegalStateException("JWT_SECRET phải là Base64 hợp lệ chứa ít nhất 32 byte ngẫu nhiên");
        }
        if (expiration == null || expiration.isNegative() || expiration.isZero()
                || expiration.getSeconds() < 1) {
            throw new IllegalStateException("JWT_EXPIRATION phải là duration dương, ít nhất 1 giây (ví dụ PT1H)");
        }
        secretKey = new SecretKeySpec(key, "HmacSHA256");
    }

    public SecretKey secretKey() {
        return secretKey;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public Duration getExpiration() {
        return expiration;
    }

    public void setExpiration(Duration expiration) {
        this.expiration = expiration;
    }
}
