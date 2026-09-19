package com.lauriel.store.config;

import com.lauriel.store.security.JwtService;
import java.time.Clock;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {
    @Bean
    public JwtEncoder jwtEncoder(JwtProperties properties) {
        return NimbusJwtEncoder.withSecretKey(properties.secretKey())
                .algorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    public JwtDecoder jwtDecoder(JwtProperties properties, Clock clock) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(properties.secretKey())
                .macAlgorithm(MacAlgorithm.HS256).build();
        JwtTimestampValidator timestamps = new JwtTimestampValidator(Duration.ZERO);
        timestamps.setClock(clock);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                timestamps, new JwtIssuerValidator(JwtService.ISSUER),
                jwt -> {
                    if (jwt.getIssuedAt() == null || jwt.getExpiresAt() == null
                            || !jwt.getExpiresAt().isAfter(jwt.getIssuedAt())
                            || jwt.getIssuedAt().isAfter(clock.instant())) {
                        return OAuth2TokenValidatorResult.failure(
                                new OAuth2Error("invalid_token", "Token không hợp lệ", null));
                    }
                    return OAuth2TokenValidatorResult.success();
                }));
        return decoder;
    }
}
