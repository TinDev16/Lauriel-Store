package com.lauriel.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.lauriel.store.dto.statistics.StatisticsReport;
import com.lauriel.store.entity.enums.OrderStatus;
import com.lauriel.store.support.TestSchema;
import com.lauriel.store.support.TestSecrets;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class StatisticsServiceTest {
    @Autowired StatisticsService statistics;
    @Autowired JdbcTemplate jdbc;
    @Autowired WebApplicationContext context;
    private MockMvc mvc;
    private LocalDate today;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry properties) throws Exception {
        String url = TestSchema.createIsolatedDatabase();
        properties.add("spring.datasource.url", () -> url);
        properties.add("spring.datasource.username", () -> "sa");
        properties.add("spring.datasource.password", () -> "");
        properties.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        properties.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        properties.add("app.jwt.secret", TestSecrets::randomBase64);
    }

    @BeforeEach
    void reset() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        today = LocalDate.now(ZoneId.systemDefault());
        for (String table : new String[] {"purchase_receipt_details", "purchase_receipts",
                "order_details", "orders", "product_variants", "products", "categories", "sizes", "colors",
                "customers", "vouchers", "suppliers", "users"}) {
            jdbc.execute("DELETE FROM " + table);
        }
    }

    private Timestamp at(LocalDate date) {
        return Timestamp.valueOf(date.atTime(12, 0));
    }

    private void seed() {
        LocalDate yesterday = today.minusDays(1);
        jdbc.update("INSERT INTO customers(customer_id,customer_name,created_at) VALUES(101,'An',?)", at(yesterday));
        jdbc.update("INSERT INTO customers(customer_id,customer_name,created_at) VALUES(102,'Binh',?)", at(today));
        jdbc.update("INSERT INTO categories(category_id,category_name,status) VALUES(101,'Clothing','ACTIVE')");
        jdbc.update("INSERT INTO products(product_id,category_id,product_name,price,status) VALUES(101,101,'Ao',50,'ACTIVE')");
        jdbc.update("INSERT INTO products(product_id,category_id,product_name,price,status) VALUES(102,101,'Quan',60,'ACTIVE')");
        jdbc.update("INSERT INTO sizes(size_id,size_name) VALUES(101,'M')");
        jdbc.update("INSERT INTO sizes(size_id,size_name) VALUES(102,'L')");
        jdbc.update("INSERT INTO colors(color_id,color_name) VALUES(101,'Den')");
        jdbc.update("INSERT INTO colors(color_id,color_name) VALUES(102,'Trang')");
        jdbc.update("INSERT INTO product_variants(variant_id,product_id,size_id,color_id,sku,price,quantity,status) VALUES(201,101,101,101,'A-M-D',50,3,'ACTIVE')");
        jdbc.update("INSERT INTO product_variants(variant_id,product_id,size_id,color_id,sku,price,quantity,status) VALUES(202,102,101,101,'Q-M-D',60,0,'ACTIVE')");
        jdbc.update("INSERT INTO product_variants(variant_id,product_id,size_id,color_id,sku,price,quantity,status) VALUES(203,101,102,102,'A-L-T',30,20,'INACTIVE')");
        jdbc.update("INSERT INTO vouchers(voucher_id,code,voucher_name,discount_type,discount_value,min_order_amount,quantity,start_date,end_date,status) VALUES(101,'SALE10','Sale','FIXED',10,0,10,?,?,'ACTIVE')",
                at(today.minusDays(10)), at(today.plusDays(10)));
        jdbc.update("INSERT INTO orders(order_id,customer_id,voucher_id,total_amount,discount_amount,final_amount,status,shipping_address,created_at) VALUES(101,101,101,100,10,90,'DELIVERED','A',?)", at(yesterday));
        jdbc.update("INSERT INTO orders(order_id,customer_id,voucher_id,total_amount,discount_amount,final_amount,status,shipping_address,created_at) VALUES(102,102,101,220,20,200,'CANCELLED','B',?)", at(yesterday));
        jdbc.update("INSERT INTO orders(order_id,customer_id,total_amount,discount_amount,final_amount,status,shipping_address,created_at) VALUES(103,102,50,0,50,'PENDING','B',?)", at(today));
        jdbc.update("INSERT INTO orders(order_id,customer_id,total_amount,discount_amount,final_amount,status,shipping_address,created_at) VALUES(104,101,60,0,60,'DELIVERED','A',?)", at(today));
        jdbc.update("INSERT INTO order_details(order_id,variant_id,quantity,unit_price,discount_amount,subtotal) VALUES(101,201,2,45,5,90)");
        jdbc.update("INSERT INTO order_details(order_id,variant_id,quantity,unit_price,discount_amount,subtotal) VALUES(102,201,4,50,0,200)");
        jdbc.update("INSERT INTO order_details(order_id,variant_id,quantity,unit_price,discount_amount,subtotal) VALUES(103,201,1,50,0,50)");
        jdbc.update("INSERT INTO order_details(order_id,variant_id,quantity,unit_price,discount_amount,subtotal) VALUES(104,202,1,60,0,60)");
        jdbc.update("INSERT INTO suppliers(supplier_id,supplier_name,status) VALUES(101,'Nha cung cap','ACTIVE')");
        jdbc.update("INSERT INTO users(user_id,role_id,username,password_hash,full_name,status) SELECT 101,role_id,'stats-staff','hash','Staff','ACTIVE' FROM roles WHERE role_name='STAFF'");
        jdbc.update("INSERT INTO purchase_receipts(receipt_id,supplier_id,user_id,total_amount,status,created_at) VALUES(101,101,101,40,'COMPLETED',?)", at(yesterday));
        jdbc.update("INSERT INTO purchase_receipts(receipt_id,supplier_id,user_id,total_amount,status,created_at) VALUES(102,101,101,100,'DRAFT',?)", at(today));
        jdbc.update("INSERT INTO purchase_receipt_details(receipt_id,variant_id,quantity,unit_price,subtotal) VALUES(101,201,4,10,40)");
    }

    private StatisticsReport report() {
        return statistics.report(null, today.minusDays(2), today, "DAY", 5, 5);
    }

    @Test
    void emptyDatabaseReturnsZerosAndChartPoints() {
        var result = report();
        assertThat(result.sales().revenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.sales().averageOrderValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.sales().cancelRate()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.inventory().totalQuantity()).isZero();
        assertThat(result.purchases().amount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.discounts().total()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.revenue()).hasSize(3);
        assertThat(result.statuses()).allMatch(s -> s.count() == 0);
    }

    @Test
    void deliveredOnlyRevenueStatusAovAndDateRange() {
        seed();
        var result = report();
        assertThat(result.sales().totalOrders()).isEqualTo(4);
        assertThat(result.sales().delivered()).isEqualTo(2);
        assertThat(result.sales().cancelled()).isEqualTo(1);
        assertThat(result.sales().revenue()).isEqualByComparingTo("150");
        assertThat(result.sales().averageOrderValue()).isEqualByComparingTo("75");
        assertThat(result.sales().cancelRate()).isEqualByComparingTo("25");
        assertThat(result.statuses()).filteredOn(s -> s.status() == OrderStatus.DELIVERED)
                .singleElement().satisfies(s -> assertThat(s.count()).isEqualTo(2));
        assertThat(result.revenue().get(1).revenue()).isEqualByComparingTo("90");
        assertThat(result.revenue().get(2).revenue()).isEqualByComparingTo("60");
        var todayOnly = statistics.report(null, today, today, "DAY", 5, 5);
        assertThat(todayOnly.sales().revenue()).isEqualByComparingTo("60");
        assertThat(todayOnly.sales().cancelled()).isZero();
    }

    @Test
    void productCustomerInventoryPurchaseAndDiscountAggregates() {
        seed();
        var result = report();
        assertThat(result.topProducts().get(0).productName()).isEqualTo("Ao");
        assertThat(result.topProducts().get(0).quantity()).isEqualTo(2);
        assertThat(result.topProductsByRevenue().get(0).revenue()).isEqualByComparingTo("90");
        assertThat(result.topVariants().get(0).sku()).isEqualTo("A-M-D");
        assertThat(result.topCustomers().get(0).orderCount()).isEqualTo(2);
        assertThat(result.topCustomers().get(0).totalSpent()).isEqualByComparingTo("150");
        assertThat(result.inventory().totalQuantity()).isEqualTo(23);
        assertThat(result.inventory().activeVariants()).isEqualTo(2);
        assertThat(result.inventory().lowStock()).isEqualTo(1);
        assertThat(result.inventory().outOfStock()).isEqualTo(1);
        assertThat(result.lowStock()).hasSize(1);
        assertThat(result.lowStock().get(0).quantity()).isEqualTo(3);
        assertThat(result.inventory().valueBySellingPrice()).isEqualByComparingTo("750");
        assertThat(result.purchases().receipts()).isEqualTo(1);
        assertThat(result.purchases().amount()).isEqualByComparingTo("40");
        assertThat(result.purchases().quantity()).isEqualTo(4);
        assertThat(result.purchaseTrend().stream().map(StatisticsReport.PurchasePoint::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("40");
        assertThat(result.topSuppliers().get(0).receiptCount()).isEqualTo(1);
        assertThat(result.discounts().voucher()).isEqualByComparingTo("10");
        assertThat(result.discounts().promotion()).isEqualByComparingTo("10");
        assertThat(result.discounts().total()).isEqualByComparingTo("20");
        assertThat(result.discounts().voucherOrders()).isEqualTo(1);
        assertThat(result.topVouchersByUses().get(0).orderCount()).isEqualTo(1);
        assertThat(result.topPromotionProducts().get(0).discount()).isEqualByComparingTo("10");
        assertThat(result.newCustomersThisMonth()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void groupingAndFilterValidationAreBounded() {
        seed();
        for (String group : new String[] {"MONTH", "QUARTER", "YEAR"}) {
            var result = statistics.report(null, today.minusDays(2), today, group, 5, 10);
            assertThat(result.revenue().stream().map(StatisticsReport.RevenuePoint::revenue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("150");
            assertThat(result.purchaseTrend().stream().map(StatisticsReport.PurchasePoint::amount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("40");
        }
        assertThat(statistics.report(null, today.minusDays(2), today, "DAY", 0, 5)
                .inventory().lowStock()).isZero();
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                statistics.report(null, today, today.minusDays(1), "DAY", 5, 5))
                .isInstanceOf(com.lauriel.store.exception.BadRequestException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                statistics.report(null, today.minusDays(366), today, "DAY", 5, 5))
                .isInstanceOf(com.lauriel.store.exception.BadRequestException.class);
    }
    @Test
    void dateValidationAndSecurityAndChartModel() throws Exception {
        seed();
        mvc.perform(get("/admin/statistics")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        mvc.perform(get("/admin/statistics").with(user("buyer").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/admin/statistics").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(model().attributeExists("report"))
                .andExpect(content().string(containsString("revenue-point")));
        mvc.perform(get("/admin/statistics").with(user("staff").roles("STAFF")))
                .andExpect(status().isOk());
        mvc.perform(get("/admin/statistics").with(user("admin").roles("ADMIN"))
                .param("fromDate", today.toString()).param("toDate", today.minusDays(1).toString()))
                .andExpect(status().isBadRequest());
    }
}