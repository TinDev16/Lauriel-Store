package com.lauriel.store.controller.web;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component("view")
public class WebViewSupport {

    public String money(BigDecimal amount) {
        return amount == null ? "—" : number(amount) + " ₫";
    }

    public String number(Number value) {
        if (value == null) return "—";
        NumberFormat format = NumberFormat.getNumberInstance(
            Locale.forLanguageTag("vi-VN")
        );
        format.setMinimumFractionDigits(0);
        format.setMaximumFractionDigits(2);
        format.setGroupingUsed(true);
        return format.format(value);
    }

    public String date(LocalDateTime value) {
        return value == null
            ? "—"
            : value.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    public String label(Object value) {
        if (value == null) return "—";
        return switch (value.toString()) {
            case "ACTIVE" -> "Đang hoạt động";
            case "INACTIVE" -> "Ngừng hoạt động";
            case "LOCKED" -> "Đã khóa";
            case "PERCENT" -> "Theo phần trăm";
            case "FIXED" -> "Số tiền cố định";
            case "IMPORT" -> "Nhập hàng";
            case "SALE" -> "Bán hàng";
            case "RETURN" -> "Hoàn hàng";
            case "ADJUSTMENT" -> "Điều chỉnh";
            case "PENDING" -> "Chờ xác nhận";
            case "CONFIRMED" -> "Đã xác nhận";
            case "PROCESSING" -> "Đang xử lý";
            case "SHIPPING" -> "Đang giao";
            case "DELIVERED" -> "Đã giao";
            case "CANCELLED" -> "Đã hủy";
            case "RETURNED" -> "Đã trả hàng";
            case "DRAFT" -> "Bản nháp";
            case "COMPLETED" -> "Đã hoàn tất";
            case "PAID" -> "Đã thanh toán";
            case "FAILED" -> "Thất bại";
            case "REFUNDED" -> "Đã hoàn tiền";
            case "COD" -> "Thanh toán khi nhận hàng";
            case "BANKING" -> "Chuyển khoản";
            case "ORDER" -> "Đơn hàng";
            case "PURCHASE_RECEIPT" -> "Phiếu nhập hàng";
            case "ADMIN" -> "Quản trị viên";
            case "STAFF" -> "Nhân viên";
            case "CUSTOMER" -> "Khách hàng";
            default -> value.toString();
        };
    }

    public String marketingStatusText(Object status, Integer quantity, LocalDateTime startDate, LocalDateTime endDate) {
        if (status == null || "INACTIVE".equals(status.toString())) {
            return "Ngừng hoạt động";
        }
        LocalDateTime now = LocalDateTime.now();
        if (endDate != null && now.isAfter(endDate)) {
            return "Đã hết hạn";
        }
        if (quantity != null && quantity <= 0) {
            return "Hết lượt dùng";
        }
        if (startDate != null && now.isBefore(startDate)) {
            return "Sắp diễn ra";
        }
        return "Đang diễn ra";
    }

    public String marketingStatusClass(Object status, Integer quantity, LocalDateTime startDate, LocalDateTime endDate) {
        if (status == null || "INACTIVE".equals(status.toString())) {
            return "text-bg-secondary";
        }
        LocalDateTime now = LocalDateTime.now();
        if (endDate != null && now.isAfter(endDate)) {
            return "text-bg-danger";
        }
        if (quantity != null && quantity <= 0) {
            return "text-bg-warning text-dark";
        }
        if (startDate != null && now.isBefore(startDate)) {
            return "text-bg-info text-dark";
        }
        return "text-bg-success";
    }

    public String marketingStatusText(Object status, LocalDateTime startDate, LocalDateTime endDate) {
        return marketingStatusText(status, null, startDate, endDate);
    }

    public String marketingStatusClass(Object status, LocalDateTime startDate, LocalDateTime endDate) {
        return marketingStatusClass(status, null, startDate, endDate);
    }
}
