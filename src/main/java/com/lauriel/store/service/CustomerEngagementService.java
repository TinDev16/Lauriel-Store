package com.lauriel.store.service;

import com.lauriel.store.dto.request.CartSnapshotRequest;
import com.lauriel.store.exception.BadRequestException;
import com.lauriel.store.repository.ProductVariantRepository;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerEngagementService {
    private final JdbcTemplate jdbc;
    private final ProductVariantRepository variants;
    private final Clock clock;

    public CustomerEngagementService(JdbcTemplate jdbc, ProductVariantRepository variants,
            @Qualifier("pricingClock") Clock clock) {
        this.jdbc = jdbc;
        this.variants = variants;
        this.clock = clock;
    }

    @Transactional
    public void heartbeat(long userId) {
        LocalDateTime now = LocalDateTime.now(clock);
        int updated = jdbc.update("UPDATE customer_activity SET last_seen_at=? WHERE user_id=?",
                Timestamp.valueOf(now), userId);
        if (updated == 0) {
            jdbc.update("INSERT INTO customer_activity(user_id,last_seen_at) VALUES(?,?)",
                    userId, Timestamp.valueOf(now));
        }
    }

    @Transactional
    public void syncCart(long userId, CartSnapshotRequest request) {
        Map<Long, Integer> normalized = new LinkedHashMap<>();
        request.items().forEach(item -> normalized.merge(item.variantId(), item.quantity(), Math::addExact));
        if (normalized.values().stream().anyMatch(quantity -> quantity <= 0 || quantity > 999)) {
            throw new BadRequestException("Số lượng sản phẩm trong giỏ không hợp lệ.");
        }
        if (!normalized.isEmpty() && variants.findByVariantIdIn(normalized.keySet()).size() != normalized.size()) {
            throw new BadRequestException("Giỏ hàng chứa thuộc tính không tồn tại.");
        }
        jdbc.update("DELETE FROM customer_cart_items WHERE user_id=?", userId);
        LocalDateTime now = LocalDateTime.now(clock);
        normalized.forEach((variantId, quantity) -> jdbc.update(
                "INSERT INTO customer_cart_items(user_id,variant_id,quantity,updated_at) VALUES(?,?,?,?)",
                userId, variantId, quantity, Timestamp.valueOf(now)));
        heartbeat(userId);
    }
}
