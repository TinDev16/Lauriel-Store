package com.lauriel.store.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.lauriel.store.entity.enums.OrderStatus;
import com.lauriel.store.event.OrderCreatedEvent;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class TelegramOrderMessageFormatterTest {

    private final TelegramOrderMessageFormatter formatter =
        new TelegramOrderMessageFormatter();

    @Test
    void messageContainsOrderAllItemsAndVietnameseMoneyBreakdown() {
        var event = new OrderCreatedEvent(
            125L,
            "Nguyễn Văn A",
            "0901234567",
            "123 Nguyễn Trãi, TP.HCM",
            new BigDecimal("850000"),
            new BigDecimal("50000"),
            new BigDecimal("20000"),
            new BigDecimal("820000"),
            OrderStatus.PENDING,
            LocalDateTime.of(2026, 9, 30, 20, 15),
            List.of(
                new OrderCreatedEvent.Item("Áo thun Lauriel", "M", "Đen", 2),
                new OrderCreatedEvent.Item("Quần Cargo", "L", "Đen", 1)
            )
        );

        String message = formatter.format(event);

        assertThat(message)
            .contains(
                "ĐƠN HÀNG MỚI",
                "Mã đơn: #125",
                "Nguyễn Văn A",
                "0901234567"
            )
            .contains("Áo thun Lauriel - M / Đen × 2")
            .contains("Quần Cargo - L / Đen × 1")
            .contains("Khuyến mãi đơn hàng: 0", "Giảm bằng voucher: 50.000")
            .contains("Khuyến mãi đơn hàng: 0", "Giảm bằng voucher: 50.000")
            .contains("Phí ship: 20.000 ₫")
            .contains("Tổng thanh toán: 820.000 ₫")
            .contains("Trạng thái: PENDING", "30/09/2026 20:15");
    }

    @Test
    void messageHandlesNullsAndLimitsLongItemLists() {
        var items = java.util.stream.IntStream.rangeClosed(1, 10)
            .mapToObj(index ->
                new OrderCreatedEvent.Item(
                    "Sản phẩm " + index,
                    null,
                    null,
                    index
                )
            )
            .toList();
        var event = new OrderCreatedEvent(
            9L,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            items
        );

        assertThat(formatter.format(event))
            .contains(
                "Khách hàng: Không rõ",
                "SĐT: Không có",
                "... và 2 sản phẩm khác"
            )
            .doesNotContain("Sản phẩm 9", "Sản phẩm 10");
    }
}
