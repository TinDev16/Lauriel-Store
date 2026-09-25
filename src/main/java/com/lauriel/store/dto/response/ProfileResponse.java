package com.lauriel.store.dto.response;

public record ProfileResponse(Long customerId, Long userId, String username, String fullName,
                              String email, String phone, String address) {}
