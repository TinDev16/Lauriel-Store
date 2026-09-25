package com.lauriel.store.dto.response;

import com.lauriel.store.entity.enums.ActiveStatus;
import java.time.LocalDateTime;

public record CategoryResponse(Long categoryId, String categoryName, ActiveStatus status,
                               LocalDateTime createdAt, long productCount) {
}
