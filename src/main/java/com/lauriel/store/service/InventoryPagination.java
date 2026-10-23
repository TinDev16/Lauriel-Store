package com.lauriel.store.service;

import com.lauriel.store.dto.response.PageResponse;
import com.lauriel.store.exception.BadRequestException;
import java.util.function.Function;
import org.springframework.data.domain.*;

final class InventoryPagination {

    private InventoryPagination() {}

    static Pageable page(int page, int size, Sort sort) {
        if (
            page < 0 ||
            size < 1 ||
            size > 100 ||
            (long) page * size > Integer.MAX_VALUE
        ) {
            throw new BadRequestException(
                "Phân trang không hợp lệ; size phải từ 1 đến 100"
            );
        }
        return PageRequest.of(page, size, sort);
    }

    static <T, R> PageResponse<R> response(
        Page<T> result,
        Function<T, R> mapper
    ) {
        return new PageResponse<>(
            result.getContent().stream().map(mapper).toList(),
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages()
        );
    }
}
