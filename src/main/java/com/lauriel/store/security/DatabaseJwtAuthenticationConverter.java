package com.lauriel.store.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Component;

@Component
public class DatabaseJwtAuthenticationConverter
    implements Converter<Jwt, AbstractAuthenticationToken>
{

    private final StoreUserDetailsService userDetailsService;

    public DatabaseJwtAuthenticationConverter(
        StoreUserDetailsService userDetailsService
    ) {
        this.userDetailsService = userDetailsService;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        StoreUserDetails user;
        try {
            user = userDetailsService.loadByUserId(
                Long.valueOf(jwt.getSubject())
            );
        } catch (
            IllegalArgumentException
            | UsernameNotFoundException exception
        ) {
            throw new InvalidBearerTokenException("Token không hợp lệ");
        }
        if (
            !user.isEnabled() ||
            !user.isAccountNonLocked() ||
            !user.getRole().name().equals(jwt.getClaimAsString("role"))
        ) {
            throw new InvalidBearerTokenException("Token không hợp lệ");
        }
        // The API principal needs no password after the database check.
        user.eraseCredentials();
        return UsernamePasswordAuthenticationToken.authenticated(
            user,
            null,
            user.getAuthorities()
        );
    }
}
