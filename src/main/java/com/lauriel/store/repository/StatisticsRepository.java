package com.lauriel.store.repository;

import com.lauriel.store.dto.statistics.StatisticsReport;
import com.lauriel.store.entity.enums.OrderStatus;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class StatisticsRepository {
    public record OrderAggregate(long total, long delivered, long cancelled, BigDecimal revenue,
                                 BigDecimal voucherDiscount, long voucherOrders) {}
    public record DailyRevenue(LocalDate date, BigDecimal revenue, long orders) {}
    public record DailyCustomers(LocalDate date, long count) {}
    public record DailyPurchase(LocalDate date, long receipts, BigDecimal amount, long quantity) {}
    public record PromotionAggregate(BigDecimal discount, long quantity) {}

    private final NamedParameterJdbcTemplate jdbc;

    public StatisticsRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static MapSqlParameterSource range(LocalDateTime from, LocalDateTime until) {
        return new MapSqlParameterSource().addValue("from", from).addValue("until", until);
    }

    private static BigDecimal money(ResultSet rs, String column) throws SQLException {
        BigDecimal value = rs.getBigDecimal(column);
        return value == null ? BigDecimal.ZERO : value;
    }

    public OrderAggregate orders(LocalDateTime from, LocalDateTime until) {
        String sql = """
                SELECT COUNT(*) total,
                  COALESCE(SUM(CASE WHEN status='DELIVERED' THEN 1 ELSE 0 END),0) delivered,
                  COALESCE(SUM(CASE WHEN status='CANCELLED' THEN 1 ELSE 0 END),0) cancelled,
                  COALESCE(SUM(CASE WHEN status='DELIVERED' THEN final_amount ELSE 0 END),0) revenue,
                  COALESCE(SUM(CASE WHEN status='DELIVERED' THEN discount_amount ELSE 0 END),0) voucher_discount,
                  COALESCE(SUM(CASE WHEN status='DELIVERED' AND discount_amount>0 THEN 1 ELSE 0 END),0) voucher_orders
                FROM orders WHERE created_at >= :from AND created_at < :until
                """;
        return jdbc.queryForObject(sql, range(from, until), (rs, n) ->
                new OrderAggregate(rs.getLong("total"), rs.getLong("delivered"), rs.getLong("cancelled"),
                        money(rs, "revenue"), money(rs, "voucher_discount"), rs.getLong("voucher_orders")));
    }

    public Map<OrderStatus, Long> statuses(LocalDateTime from, LocalDateTime until) {
        String sql = "SELECT status, COUNT(*) total FROM orders "
                + "WHERE created_at >= :from AND created_at < :until GROUP BY status";
        Map<OrderStatus, Long> result = new EnumMap<>(OrderStatus.class);
        jdbc.query(sql, range(from, until), (RowCallbackHandler) rs ->
                result.put(OrderStatus.valueOf(rs.getString("status")), rs.getLong("total")));
        return result;
    }

    public List<DailyRevenue> revenueDaily(LocalDateTime from, LocalDateTime until) {
        String sql = """
                SELECT DATE(created_at) bucket_date, SUM(final_amount) revenue, COUNT(*) order_count
                FROM orders WHERE status='DELIVERED' AND created_at >= :from AND created_at < :until
                GROUP BY DATE(created_at) ORDER BY bucket_date
                """;
        return jdbc.query(sql, range(from, until), (rs, n) ->
                new DailyRevenue(rs.getDate("bucket_date").toLocalDate(), money(rs, "revenue"), rs.getLong("order_count")));
    }

    public List<StatisticsReport.TopProduct> topProducts(LocalDateTime from, LocalDateTime until,
                                                          int limit, boolean byRevenue) {
        String sql = """
                SELECT p.product_id, p.product_name, SUM(d.quantity) quantity, SUM(d.subtotal) revenue
                FROM order_details d JOIN orders o ON o.order_id=d.order_id
                  JOIN product_variants v ON v.variant_id=d.variant_id
                  JOIN products p ON p.product_id=v.product_id
                WHERE o.status='DELIVERED' AND o.created_at >= :from AND o.created_at < :until
                GROUP BY p.product_id, p.product_name
                ORDER BY
                """ + (byRevenue ? " revenue DESC, quantity DESC" : " quantity DESC, revenue DESC")
                + ", p.product_id LIMIT :limit";
        var params = range(from, until).addValue("limit", limit);
        return jdbc.query(sql, params, (rs, n) -> new StatisticsReport.TopProduct(
                rs.getLong("product_id"), rs.getString("product_name"),
                rs.getLong("quantity"), money(rs, "revenue")));
    }

    public List<StatisticsReport.TopVariant> topVariants(LocalDateTime from, LocalDateTime until, int limit) {
        String sql = """
                SELECT v.variant_id, v.sku, p.product_name, s.size_name, c.color_name,
                       SUM(d.quantity) quantity, SUM(d.subtotal) revenue
                FROM order_details d JOIN orders o ON o.order_id=d.order_id
                  JOIN product_variants v ON v.variant_id=d.variant_id
                  JOIN products p ON p.product_id=v.product_id
                  JOIN sizes s ON s.size_id=v.size_id JOIN colors c ON c.color_id=v.color_id
                WHERE o.status='DELIVERED' AND o.created_at >= :from AND o.created_at < :until
                GROUP BY v.variant_id, v.sku, p.product_name, s.size_name, c.color_name
                ORDER BY quantity DESC, revenue DESC, v.variant_id LIMIT :limit
                """;
        return jdbc.query(sql, range(from, until).addValue("limit", limit), (rs, n) ->
                new StatisticsReport.TopVariant(rs.getLong("variant_id"), rs.getString("sku"),
                        rs.getString("product_name"), rs.getString("size_name"), rs.getString("color_name"),
                        rs.getLong("quantity"), money(rs, "revenue")));
    }

    public List<StatisticsReport.TopCustomer> topCustomers(LocalDateTime from, LocalDateTime until, int limit) {
        String sql = """
                SELECT c.customer_id, c.customer_name, COUNT(*) order_count, SUM(o.final_amount) spent
                FROM orders o JOIN customers c ON c.customer_id=o.customer_id
                WHERE o.status='DELIVERED' AND o.created_at >= :from AND o.created_at < :until
                GROUP BY c.customer_id, c.customer_name
                ORDER BY spent DESC, order_count DESC, c.customer_id LIMIT :limit
                """;
        return jdbc.query(sql, range(from, until).addValue("limit", limit), (rs, n) ->
                new StatisticsReport.TopCustomer(rs.getLong("customer_id"), rs.getString("customer_name"),
                        rs.getLong("order_count"), money(rs, "spent")));
    }

    public StatisticsReport.Inventory inventory(int threshold) {
        String sql = """
                SELECT COALESCE(SUM(quantity),0) stock,
                       COALESCE(SUM(CASE WHEN status='ACTIVE' THEN 1 ELSE 0 END),0) active_variants,
                       COALESCE(SUM(CASE WHEN quantity>0 AND quantity<=:threshold THEN 1 ELSE 0 END),0) low_stock,
                       COALESCE(SUM(CASE WHEN quantity=0 THEN 1 ELSE 0 END),0) out_of_stock,
                       COALESCE(SUM(quantity*price),0) selling_value
                FROM product_variants
                """;
        return jdbc.queryForObject(sql, new MapSqlParameterSource("threshold", threshold), (rs, n) ->
                new StatisticsReport.Inventory(rs.getLong("stock"), rs.getLong("active_variants"),
                        rs.getLong("low_stock"), rs.getLong("out_of_stock"), money(rs, "selling_value")));
    }

    public List<StatisticsReport.StockVariant> stockVariants(int threshold, int limit, boolean low) {
        String sql = """
                SELECT v.variant_id, v.sku, p.product_name, s.size_name, c.color_name, v.quantity
                FROM product_variants v JOIN products p ON p.product_id=v.product_id
                  JOIN sizes s ON s.size_id=v.size_id JOIN colors c ON c.color_id=v.color_id
                """ + (low ? "WHERE v.quantity > 0 AND v.quantity <= :threshold " : "")
                + "ORDER BY v.quantity " + (low ? "ASC" : "DESC") + ", v.variant_id LIMIT :limit";
        var params = new MapSqlParameterSource().addValue("threshold", threshold).addValue("limit", limit);
        return jdbc.query(sql, params, (rs, n) -> new StatisticsReport.StockVariant(
                rs.getLong("variant_id"), rs.getString("sku"), rs.getString("product_name"),
                rs.getString("size_name"), rs.getString("color_name"), rs.getLong("quantity")));
    }

    public StatisticsReport.Purchase purchases(LocalDateTime from, LocalDateTime until) {
        var params = range(from, until);
        String sql = """
                SELECT COUNT(*) receipts, COALESCE(SUM(total_amount),0) amount
                FROM purchase_receipts
                WHERE status='COMPLETED' AND created_at >= :from AND created_at < :until
                """;
        long[] count = new long[1];
        BigDecimal[] amount = new BigDecimal[1];
        jdbc.query(sql, params, rs -> { count[0] = rs.getLong("receipts"); amount[0] = money(rs, "amount"); });
        String quantitySql = """
                SELECT COALESCE(SUM(d.quantity),0) quantity
                FROM purchase_receipt_details d JOIN purchase_receipts r ON r.receipt_id=d.receipt_id
                WHERE r.status='COMPLETED' AND r.created_at >= :from AND r.created_at < :until
                """;
        Long quantity = jdbc.queryForObject(quantitySql, params, Long.class);
        return new StatisticsReport.Purchase(count[0], amount[0], quantity == null ? 0 : quantity);
    }

    public List<DailyPurchase> purchasesDaily(LocalDateTime from, LocalDateTime until) {
        String sql = """
                SELECT DATE(r.created_at) bucket_date, COUNT(*) receipt_count,
                       SUM(r.total_amount) amount, COALESCE(SUM(d.quantity),0) quantity
                FROM purchase_receipts r
                LEFT JOIN (SELECT receipt_id, SUM(quantity) quantity FROM purchase_receipt_details
                           GROUP BY receipt_id) d ON d.receipt_id=r.receipt_id
                WHERE r.status='COMPLETED' AND r.created_at >= :from AND r.created_at < :until
                GROUP BY DATE(r.created_at) ORDER BY bucket_date
                """;
        return jdbc.query(sql, range(from, until), (rs, n) -> new DailyPurchase(
                rs.getDate("bucket_date").toLocalDate(), rs.getLong("receipt_count"),
                money(rs, "amount"), rs.getLong("quantity")));
    }
    public List<StatisticsReport.Supplier> topSuppliers(LocalDateTime from, LocalDateTime until, int limit) {
        String sql = """
                SELECT s.supplier_id, s.supplier_name, COUNT(*) receipts, SUM(r.total_amount) amount
                FROM purchase_receipts r JOIN suppliers s ON s.supplier_id=r.supplier_id
                WHERE r.status='COMPLETED' AND r.created_at >= :from AND r.created_at < :until
                GROUP BY s.supplier_id, s.supplier_name
                ORDER BY amount DESC, receipts DESC, s.supplier_id LIMIT :limit
                """;
        return jdbc.query(sql, range(from, until).addValue("limit", limit), (rs, n) ->
                new StatisticsReport.Supplier(rs.getLong("supplier_id"), rs.getString("supplier_name"),
                        rs.getLong("receipts"), money(rs, "amount")));
    }

    public List<StatisticsReport.Voucher> topVouchers(LocalDateTime from, LocalDateTime until,
                                                       int limit, boolean byDiscount) {
        String sql = """
                SELECT v.voucher_id, v.code, COUNT(*) order_count, SUM(o.discount_amount) discount
                FROM orders o JOIN vouchers v ON v.voucher_id=o.voucher_id
                WHERE o.status='DELIVERED' AND o.created_at >= :from AND o.created_at < :until
                GROUP BY v.voucher_id, v.code
                ORDER BY
                """ + (byDiscount ? " discount DESC, order_count DESC" : " order_count DESC, discount DESC")
                + ", v.voucher_id LIMIT :limit";
        return jdbc.query(sql, range(from, until).addValue("limit", limit), (rs, n) ->
                new StatisticsReport.Voucher(rs.getLong("voucher_id"), rs.getString("code"),
                        rs.getLong("order_count"), money(rs, "discount")));
    }

    public PromotionAggregate promotions(LocalDateTime from, LocalDateTime until) {
        String sql = """
                SELECT COALESCE(SUM(d.discount_amount*d.quantity),0) discount,
                       COALESCE(SUM(CASE WHEN d.discount_amount>0 THEN d.quantity ELSE 0 END),0) quantity
                FROM order_details d JOIN orders o ON o.order_id=d.order_id
                WHERE o.status='DELIVERED' AND o.created_at >= :from AND o.created_at < :until
                """;
        return jdbc.queryForObject(sql, range(from, until), (rs, n) ->
                new PromotionAggregate(money(rs, "discount"), rs.getLong("quantity")));
    }

    public List<StatisticsReport.PromotionProduct> topPromotionProducts(LocalDateTime from,
                                                                         LocalDateTime until, int limit) {
        String sql = """
                SELECT p.product_id, p.product_name, SUM(d.quantity) quantity,
                       SUM(d.discount_amount*d.quantity) discount
                FROM order_details d JOIN orders o ON o.order_id=d.order_id
                  JOIN product_variants v ON v.variant_id=d.variant_id
                  JOIN products p ON p.product_id=v.product_id
                WHERE o.status='DELIVERED' AND d.discount_amount>0
                  AND o.created_at >= :from AND o.created_at < :until
                GROUP BY p.product_id, p.product_name
                ORDER BY discount DESC, quantity DESC, p.product_id LIMIT :limit
                """;
        return jdbc.query(sql, range(from, until).addValue("limit", limit), (rs, n) ->
                new StatisticsReport.PromotionProduct(rs.getLong("product_id"),
                        rs.getString("product_name"), rs.getLong("quantity"), money(rs, "discount")));
    }

    public long customersCount() {
        Long count = jdbc.getJdbcTemplate().queryForObject("SELECT COUNT(*) FROM customers", Long.class);
        return count == null ? 0 : count;
    }

    public long customersCreated(LocalDateTime from, LocalDateTime until) {
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM customers WHERE created_at >= :from AND created_at < :until",
                range(from, until), Long.class);
        return count == null ? 0 : count;
    }

    public List<DailyCustomers> customersDaily(LocalDateTime from, LocalDateTime until) {
        String sql = """
                SELECT DATE(created_at) bucket_date, COUNT(*) customers FROM customers
                WHERE created_at >= :from AND created_at < :until
                GROUP BY DATE(created_at) ORDER BY bucket_date
                """;
        return jdbc.query(sql, range(from, until), (rs, n) ->
                new DailyCustomers(rs.getDate("bucket_date").toLocalDate(), rs.getLong("customers")));
    }

    public long activeProducts() {
        Long count = jdbc.getJdbcTemplate().queryForObject(
                "SELECT COUNT(*) FROM products WHERE status='ACTIVE'", Long.class);
        return count == null ? 0 : count;
    }

    public long draftReceipts() {
        Long count = jdbc.getJdbcTemplate().queryForObject(
                "SELECT COUNT(*) FROM purchase_receipts WHERE status='DRAFT'", Long.class);
        return count == null ? 0 : count;
    }

    public StatisticsReport.Engagement engagement(LocalDateTime onlineSince) {
        String sql = """
                SELECT
                  (SELECT COUNT(*) FROM customer_activity a
                   JOIN customers c ON c.user_id=a.user_id WHERE a.last_seen_at>=:onlineSince) online_customers,
                  (SELECT COUNT(DISTINCT user_id) FROM customer_cart_items) cart_customers,
                  (SELECT COUNT(DISTINCT v.product_id) FROM customer_cart_items i
                   JOIN product_variants v ON v.variant_id=i.variant_id) cart_products,
                  (SELECT COALESCE(SUM(quantity),0) FROM customer_cart_items) cart_quantity
                """;
        return jdbc.queryForObject(sql, new MapSqlParameterSource("onlineSince", onlineSince), (rs, n) ->
                new StatisticsReport.Engagement(rs.getLong("online_customers"),
                        rs.getLong("cart_customers"), rs.getLong("cart_products"),
                        rs.getLong("cart_quantity")));
    }

    public List<StatisticsReport.OnlineCustomer> onlineCustomers(LocalDateTime onlineSince, int limit) {
        String sql = """
                SELECT c.customer_id, c.customer_name, u.username, a.last_seen_at,
                       COUNT(DISTINCT i.variant_id) distinct_items, COALESCE(SUM(i.quantity),0) quantity
                FROM customer_activity a
                JOIN users u ON u.user_id=a.user_id
                JOIN customers c ON c.user_id=a.user_id
                LEFT JOIN customer_cart_items i ON i.user_id=a.user_id
                WHERE a.last_seen_at>=:onlineSince
                GROUP BY c.customer_id,c.customer_name,u.username,a.last_seen_at
                ORDER BY a.last_seen_at DESC LIMIT :limit
                """;
        var params = new MapSqlParameterSource("onlineSince", onlineSince).addValue("limit", limit);
        return jdbc.query(sql, params, (rs, n) -> new StatisticsReport.OnlineCustomer(
                rs.getLong("customer_id"), rs.getString("customer_name"), rs.getString("username"),
                rs.getTimestamp("last_seen_at").toLocalDateTime(),
                rs.getLong("distinct_items"), rs.getLong("quantity")));
    }

    public List<StatisticsReport.CartInterest> cartInterest(int limit) {
        String sql = """
                SELECT p.product_id,p.product_name,COUNT(DISTINCT i.user_id) customer_count,
                       SUM(i.quantity) quantity
                FROM customer_cart_items i
                JOIN product_variants v ON v.variant_id=i.variant_id
                JOIN products p ON p.product_id=v.product_id
                GROUP BY p.product_id,p.product_name
                ORDER BY customer_count DESC,quantity DESC,p.product_id
                LIMIT :limit
                """;
        return jdbc.query(sql, new MapSqlParameterSource("limit", limit), (rs, n) ->
                new StatisticsReport.CartInterest(rs.getLong("product_id"),
                        rs.getString("product_name"), rs.getLong("customer_count"),
                        rs.getLong("quantity")));
    }
}
