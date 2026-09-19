package com.lauriel.store.dto.response;

public record RegisterResponse(Long userId, String username, String email, String role, String message) {
}
