package com.lauriel.store.dto.response;

public record LoginResponse(String token, String tokenType, long expiresIn,
                            Long userId, String username, String role) {
    @Override
    public String toString() {
        return "LoginResponse[token redacted]";
    }
}
