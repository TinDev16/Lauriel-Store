package com.lauriel.mobile.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class Formatters {

    private Formatters() {}

    public static String money(BigDecimal value) {
        if (value == null) return "—";
        NumberFormat format = NumberFormat.getNumberInstance(
            Locale.forLanguageTag("vi-VN")
        );
        format.setMinimumFractionDigits(0);
        format.setMaximumFractionDigits(2);
        format.setGroupingUsed(true);
        return format.format(value) + " ₫";
    }

    public static String date(String iso) {
        if (iso == null || iso.isEmpty()) return "—";
        try {
            return LocalDateTime.parse(iso).format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            );
        } catch (RuntimeException ignored) {
            return iso;
        }
    }

    public static String status(String raw) {
        if (raw == null) return "—";
        switch (raw) {
            case "PENDING":
                return "Chờ xác nhận";
            case "CONFIRMED":
                return "Đã xác nhận";
            case "PROCESSING":
                return "Đang xử lý";
            case "SHIPPING":
                return "Đang giao";
            case "DELIVERED":
                return "Đã giao";
            case "CANCELLED":
                return "Đã hủy";
            case "RETURNED":
                return "Đã trả";
            default:
                return raw;
        }
    }

    public static String role(String raw) {
        if (raw == null) return "—";
        switch (raw) {
            case "ADMIN":
                return "Quản trị viên";
            case "STAFF":
                return "Nhân viên";
            case "CUSTOMER":
                return "Khách hàng";
            default:
                return raw;
        }
    }

    public static boolean canCancel(String raw) {
        return "PENDING".equals(raw) || "CONFIRMED".equals(raw);
    }
}
