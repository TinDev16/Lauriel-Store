package com.lauriel.store.security;

import com.lauriel.store.config.JwtProperties;
import com.lauriel.store.dto.response.LoginResponse;
import java.time.Clock;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    public static final String ISSUER = "lauriel-store";
    private final JwtEncoder encoder;
    private final JwtProperties properties;
    private final Clock clock;

    public JwtService(
        JwtEncoder encoder,
        JwtProperties properties,
        Clock clock
    ) {
        this.encoder = encoder;
        this.properties = properties;
        this.clock = clock;
    }

    public LoginResponse issue(StoreUserDetails user) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(ISSUER)
            .subject(user.getUserId().toString())
            .issuedAt(now)
            .expiresAt(now.plus(properties.getExpiration()))
            .claim("role", user.getRole().name())
            .build();
        String token = encoder
            .encode(
                JwtEncoderParameters.from(
                    JwsHeader.with(MacAlgorithm.HS256).build(),
                    claims
                )
            )
            .getTokenValue();
        return new LoginResponse(
            token,
            "Bearer",
            properties.getExpiration().getSeconds(),
            user.getUserId(),
            user.getUsername(),
            user.getRole().name()
        );
    }
}
