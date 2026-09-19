package com.lauriel.store.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lauriel.store.support.TestSecrets;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class JwtPropertiesTest {
    @Test
    void missingOrWeakOrMalformedSecretIsRejectedWithoutEchoingIt() {
        for (String value : new String[] {"", "short", "not-valid-@@", "c2hvcnQ="}) {
            var properties = new JwtProperties();
            properties.setSecret(value);
            assertThatThrownBy(properties::validate).isInstanceOf(IllegalStateException.class)
                    .hasMessage("JWT_SECRET phải là Base64 hợp lệ chứa ít nhất 32 byte ngẫu nhiên");
        }
    }

    @Test
    void configuredRandomSecretAndExpirationAreAccepted() {
        var properties = new JwtProperties();
        properties.setSecret(TestSecrets.randomBase64());
        properties.setExpiration(Duration.ofMinutes(30));
        properties.validate();
        assertThat(properties.secretKey().getEncoded()).hasSize(32);
        assertThat(properties.getExpiration()).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    void nonpositiveOrSubsecondExpirationIsRejected() {
        for (Duration value : new Duration[] {Duration.ZERO, Duration.ofSeconds(-1), Duration.ofMillis(500)}) {
            var properties = new JwtProperties();
            properties.setSecret(TestSecrets.randomBase64());
            properties.setExpiration(value);
            assertThatThrownBy(properties::validate).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("JWT_EXPIRATION");
        }
    }
}
