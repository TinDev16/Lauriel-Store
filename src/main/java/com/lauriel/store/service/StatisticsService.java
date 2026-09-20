package com.lauriel.store.service;

import com.lauriel.store.dto.statistics.StatisticsReport;
import com.lauriel.store.entity.enums.OrderStatus;
import com.lauriel.store.exception.BadRequestException;
import com.lauriel.store.repository.StatisticsRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatisticsService {
    private final StatisticsRepository repository;
    private final Clock clock;

    public StatisticsService(StatisticsRepository repository, @Qualifier("pricingClock") Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public StatisticsReport report(String preset, LocalDate fromDate, LocalDate toDate,
                                   String groupBy, int threshold, int top) {
        LocalDate today = LocalDate.now(clock);
        StatisticsReport.Range range = resolve(preset, fromDate, toDate, groupBy, threshold, top, today);
        LocalDateTime from = range.fromDate().atStartOfDay();
        LocalDateTime until = range.toDate().plusDays(1).atStartOfDay();
        var orders = repository.orders(from, until);
        BigDecimal aov = orders.delivered() == 0 ? BigDecimal.ZERO :
                orders.revenue().divide(BigDecimal.valueOf(orders.delivered()), 2, RoundingMode.HALF_UP);
        BigDecimal cancelRate = percent(orders.cancelled(), orders.total());
        var sales = new StatisticsReport.Sales(orders.total(), orders.delivered(), orders.cancelled(),
                orders.revenue(), aov, cancelRate);

        var counts = repository.statuses(from, until);
        List<StatisticsReport.StatusPoint> statuses = new ArrayList<>();
        for (OrderStatus status : OrderStatus.values()) {
            long count = counts.getOrDefault(status, 0L);
            statuses.add(new StatisticsReport.StatusPoint(status, count, percent(count, orders.total())));
        }

        var inventory = repository.inventory(threshold);
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate yearStart = today.withDayOfYear(1);
        var todayOrders = repository.orders(today.atStartOfDay(), today.plusDays(1).atStartOfDay());
        var monthOrders = repository.orders(monthStart.atStartOfDay(), today.plusDays(1).atStartOfDay());
        var yearOrders = repository.orders(yearStart.atStartOfDay(), today.plusDays(1).atStartOfDay());
        var allOrders = repository.orders(LocalDate.of(1970, 1, 1).atStartOfDay(), today.plusDays(1).atStartOfDay());
        var monthPurchase = repository.purchases(monthStart.atStartOfDay(), today.plusDays(1).atStartOfDay());
        var kpis = new StatisticsReport.Kpis(todayOrders.revenue(), monthOrders.revenue(),
                yearOrders.revenue(), todayOrders.total(), monthOrders.total(), allOrders.delivered(),
                allOrders.cancelled(), repository.customersCount(), repository.activeProducts(),
                inventory.activeVariants(), inventory.totalQuantity(), inventory.lowStock(),
                repository.draftReceipts(), monthPurchase.amount());

        var promotion = repository.promotions(from, until);
        var discounts = new StatisticsReport.Discounts(orders.voucherDiscount(), promotion.discount(),
                orders.voucherDiscount().add(promotion.discount()), orders.voucherOrders(), promotion.quantity());
        long newCustomersThisMonth = repository.customersCreated(
                monthStart.atStartOfDay(), today.plusDays(1).atStartOfDay());

        return new StatisticsReport(range, kpis, sales,
                revenueSeries(range, repository.revenueDaily(from, until)),
                statuses,
                repository.topProducts(from, until, top, false),
                repository.topProducts(from, until, top, true),
                repository.topVariants(from, until, top),
                repository.topCustomers(from, until, top),
                inventory,
                repository.stockVariants(threshold, top, true),
                repository.stockVariants(threshold, top, false),
                repository.purchases(from, until),
                purchaseSeries(range, repository.purchasesDaily(from, until)),
                repository.topSuppliers(from, until, top),
                discounts,
                repository.topVouchers(from, until, top, false),
                repository.topVouchers(from, until, top, true),
                repository.topPromotionProducts(from, until, top),
                newCustomersThisMonth,
                customerSeries(range, repository.customersDaily(from, until)));
    }

    public StatisticsReport.Range resolve(String preset, LocalDate from, LocalDate to, String groupBy,
                                          int threshold, int top, LocalDate today) {
        if (threshold < 0 || threshold > 100000) {
            throw new BadRequestException("Ngưỡng tồn kho phải từ 0 đến 100.000.");
        }
        if (top != 5 && top != 10) {
            throw new BadRequestException("Chỉ hỗ trợ Top 5 hoặc Top 10.");
        }
        String selected = preset == null || preset.isBlank() ? "LAST_30" : preset.toUpperCase(Locale.ROOT);
        if (from != null || to != null) {
            if (from == null || to == null) throw new BadRequestException("Cần nhập đủ ngày bắt đầu và ngày kết thúc.");
            selected = "CUSTOM";
        } else {
            switch (selected) {
                case "TODAY" -> { from = today; to = today; }
                case "LAST_7" -> { from = today.minusDays(6); to = today; }
                case "LAST_30" -> { from = today.minusDays(29); to = today; }
                case "THIS_MONTH" -> { from = today.withDayOfMonth(1); to = today; }
                case "THIS_YEAR" -> { from = today.withDayOfYear(1); to = today; }
                default -> throw new BadRequestException("Bộ lọc thời gian không hợp lệ.");
            }
        }
        if (from.isAfter(to)) throw new BadRequestException("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc.");
        if (to.isAfter(today)) throw new BadRequestException("Ngày kết thúc không được ở tương lai.");
        if (ChronoUnit.DAYS.between(from, to) > 365) {
            throw new BadRequestException("Khoảng thống kê tối đa 366 ngày.");
        }
        String unit = groupBy == null || groupBy.isBlank()
                ? (ChronoUnit.DAYS.between(from, to) > 90 ? "MONTH" : "DAY")
                : groupBy.toUpperCase(Locale.ROOT);
        if (!List.of("DAY", "MONTH", "QUARTER", "YEAR").contains(unit)) {
            throw new BadRequestException("Kiểu nhóm thời gian không hợp lệ.");
        }
        return new StatisticsReport.Range(selected, from, to, unit, threshold, top);
    }

    private static BigDecimal percent(long part, long whole) {
        if (whole == 0) return BigDecimal.ZERO;
        return BigDecimal.valueOf(part).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(whole), 2, RoundingMode.HALF_UP);
    }

    private static String label(LocalDate day, String unit) {
        return switch (unit) {
            case "MONTH" -> String.format(Locale.ROOT, "%04d-%02d", day.getYear(), day.getMonthValue());
            case "QUARTER" -> day.getYear() + "-Q" + ((day.getMonthValue() - 1) / 3 + 1);
            case "YEAR" -> String.valueOf(day.getYear());
            default -> day.toString();
        };
    }

    private static List<StatisticsReport.RevenuePoint> revenueSeries(
            StatisticsReport.Range range, List<StatisticsRepository.DailyRevenue> daily) {
        Map<String, BigDecimal> revenue = new LinkedHashMap<>();
        Map<String, Long> counts = new LinkedHashMap<>();
        for (LocalDate day = range.fromDate(); !day.isAfter(range.toDate()); day = day.plusDays(1)) {
            String key = label(day, range.groupBy());
            revenue.putIfAbsent(key, BigDecimal.ZERO);
            counts.putIfAbsent(key, 0L);
        }
        for (var row : daily) {
            String key = label(row.date(), range.groupBy());
            revenue.merge(key, row.revenue(), BigDecimal::add);
            counts.merge(key, row.orders(), Long::sum);
        }
        List<StatisticsReport.RevenuePoint> points = new ArrayList<>();
        revenue.forEach((key, value) -> points.add(new StatisticsReport.RevenuePoint(key, value, counts.get(key))));
        return points;
    }

    private static List<StatisticsReport.PurchasePoint> purchaseSeries(
            StatisticsReport.Range range, List<StatisticsRepository.DailyPurchase> daily) {
        Map<String, StatisticsReport.PurchasePoint> points = new LinkedHashMap<>();
        for (LocalDate day = range.fromDate(); !day.isAfter(range.toDate()); day = day.plusDays(1)) {
            String key = label(day, range.groupBy());
            points.putIfAbsent(key, new StatisticsReport.PurchasePoint(key, 0, BigDecimal.ZERO, 0));
        }
        for (var row : daily) {
            String key = label(row.date(), range.groupBy());
            var old = points.get(key);
            points.put(key, new StatisticsReport.PurchasePoint(key,
                    old.receipts() + row.receipts(), old.amount().add(row.amount()),
                    old.quantity() + row.quantity()));
        }
        return new ArrayList<>(points.values());
    }
    private static List<StatisticsReport.CustomerPoint> customerSeries(
            StatisticsReport.Range range, List<StatisticsRepository.DailyCustomers> daily) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (LocalDate day = range.fromDate(); !day.isAfter(range.toDate()); day = day.plusDays(1)) {
            counts.putIfAbsent(label(day, range.groupBy()), 0L);
        }
        for (var row : daily) counts.merge(label(row.date(), range.groupBy()), row.count(), Long::sum);
        List<StatisticsReport.CustomerPoint> points = new ArrayList<>();
        counts.forEach((key, value) -> points.add(new StatisticsReport.CustomerPoint(key, value)));
        return points;
    }
}