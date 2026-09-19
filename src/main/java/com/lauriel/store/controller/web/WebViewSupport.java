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
        return amount == null ? "—"
                : NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(amount) + " ₫";
    }

    public String date(LocalDateTime value) {
        return value == null ? "—" : value.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    public String label(Object value) {
        if (value == null) return "—";
        return switch (value.toString()) {
            case "ACTIVE" -> "Đang hoạt động";
            case "INACTIVE" -> "Ngừng hoạt động";
            case "LOCKED" -> "Đã khóa";
            case "PERCENT" -> "Theo phần trăm";
            case "FIXED" -> "Số tiền cố định";
            case "IMPORT" -> "Nhập kho";
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
            case "PURCHASE_RECEIPT" -> "Phiếu nhập kho";
            case "ADMIN" -> "Quản trị viên";
            case "STAFF" -> "Nhân viên";
            case "CUSTOMER" -> "Khách hàng";
            default -> value.toString();
        };
    }}
