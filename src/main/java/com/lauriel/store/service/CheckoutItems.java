package com.lauriel.store.service;

import com.lauriel.store.dto.request.CheckoutItemRequest;
import com.lauriel.store.exception.BadRequestException;
import java.util.*;

final class CheckoutItems {
    private CheckoutItems() {}

    static SortedMap<Long, Integer> normalize(List<CheckoutItemRequest> items) {
        if (items == null || items.isEmpty() || items.size() > 100) {
            throw new BadRequestException("Đơn hàng phải có từ 1 đến 100 items");
        }
        var merged = new TreeMap<Long, Integer>();
        for (var item : items) {
            if (item == null || item.variantId() == null || item.variantId() <= 0
                    || item.quantity() == null || item.quantity() <= 0) {
                throw new BadRequestException("Biến thể hoặc số lượng không hợp lệ");
            }
            try {
                merged.merge(item.variantId(), item.quantity(), Math::addExact);
            } catch (ArithmeticException overflow) {
                throw new BadRequestException("Tổng số lượng vượt giới hạn Integer");
            }
        }
        return merged;
    }
}
