package com.lauriel.store.service;

import com.lauriel.store.event.OrderCreatedEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class TelegramOrderMessageFormatter {
    private static final int MAX_ITEMS = 8;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public String format(OrderCreatedEvent event) {
        var message = new StringBuilder()
                .append("🛍 ĐƠN HÀNG MỚI\n\n")
                .append("Mã đơn: #").append(event.orderId()).append('\n')
                .append("Khách hàng: ").append(safe(event.customerName(), "Không rõ", 150)).append('\n')
                .append("SĐT: ").append(safe(event.phone(), "Không có", 30)).append('\n')
                .append("Địa chỉ: ").append(safe(event.shippingAddress(), "Không có", 300)).append("\n\n")
                .append("Sản phẩm:\n");

        int displayed = Math.min(event.items().size(), MAX_ITEMS);
        for (int index = 0; index < displayed; index++) {
            var item = event.items().get(index);
            message.append("• ")
                    .append(safe(item.productName(), "Sản phẩm", 120))
                    .append(" - ").append(safe(item.size(), "N/A", 30))
                    .append(" / ").append(safe(item.color(), "N/A", 60))
                    .append(" × ").append(item.quantity() == null ? 0 : item.quantity())
                    .append('\n');
        }
        int remaining = event.items().size() - displayed;
        if (remaining > 0) message.append("... và ").append(remaining).append(" sản phẩm khác\n");
        if (event.items().isEmpty()) message.append("• Không có thông tin sản phẩm\n");

        return message.append('\n')
                .append("Tạm tính: ").append(money(event.totalAmount())).append('\n')
                .append("Khuyến mãi đơn hàng: ").append(money(event.orderPromotionDiscount())).append('\n')
                .append("Giảm bằng voucher: ").append(money(event.discountAmount())).append('\n')
                .append("Phí ship: ").append(money(event.shippingFee())).append('\n')
                .append("Tổng thanh toán: ").append(money(event.finalAmount())).append("\n\n")
                .append("Trạng thái: ").append(event.status() == null ? "Không rõ" : event.status().name()).append('\n')
                .append("Thời gian: ").append(event.createdAt() == null ? "Không rõ" : TIME.format(event.createdAt()))
                .toString();
    }

    private String money(BigDecimal amount) {
        var symbols = DecimalFormatSymbols.getInstance(Locale.ROOT);
        symbols.setGroupingSeparator('.');
        var formatter = new DecimalFormat("#,##0", symbols);
        formatter.setRoundingMode(RoundingMode.HALF_UP);
        return formatter.format(amount == null ? BigDecimal.ZERO : amount) + " ₫";
    }

    private String safe(String value, String fallback, int maxLength) {
        if (value == null || value.isBlank()) return fallback;
        String normalized = value.replaceAll("\\s+", " ").strip();
        return normalized.length() <= maxLength
                ? normalized
                : normalized.substring(0, maxLength - 1) + "…";
    }
}
