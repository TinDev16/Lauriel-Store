package com.lauriel.store.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.lauriel.store.dto.request.*;
import com.lauriel.store.entity.*;
import com.lauriel.store.entity.Order;
import com.lauriel.store.entity.enums.*;
import com.lauriel.store.exception.BadRequestException;
import com.lauriel.store.repository.*;
import com.lauriel.store.security.*;
import com.lauriel.store.service.*;
import com.lauriel.store.support.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class MarketingApiTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 18, 10, 0);
    private static final String HASH = new BCryptPasswordEncoder().encode("TestPassword123");
    @Autowired private WebApplicationContext context;
    @MockitoSpyBean private VoucherRepository vouchers;
    @Autowired private PromotionRepository promotions;
    @Autowired private PromotionProductRepository associations;
    @MockitoSpyBean private ProductVariantRepository variants;
    @MockitoSpyBean private OrderRepository orders;
    @MockitoSpyBean private OrderDetailRepository details;
    @MockitoSpyBean private InventoryTransactionRepository history;
    @Autowired private ProductRepository products;
    @Autowired private CategoryRepository categories;
    @Autowired private SizeRepository sizes;
    @Autowired private ColorRepository colors;
    @Autowired private CustomerRepository customers;
    @Autowired private UserRepository users;
    @Autowired private RoleRepository roles;
    @Autowired private JwtService jwt;
    @Autowired private OrderService orderService;
    @Autowired private ProductService catalog;
    @Autowired private VoucherService voucherService;
    @Autowired private ObjectMapper mapper;
    @Autowired private PlatformTransactionManager manager;
    @Autowired private EntityManagerFactory entityManagerFactory;
    @MockitoBean private Clock clock;
    private MockMvc mvc;
    private TransactionTemplate tx;
    private Product product;
    private ProductVariant first, second;
    private final Map<String, User> accounts = new HashMap<>();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry properties) throws Exception {
        String url = TestSchema.createIsolatedDatabase();
        String secret = TestSecrets.randomBase64();
        properties.add("spring.datasource.url", () -> url);
        properties.add("spring.datasource.username", () -> "sa");
        properties.add("spring.datasource.password", () -> "");
        properties.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        properties.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        properties.add("spring.sql.init.mode", () -> "never");
        properties.add("spring.jpa.properties.hibernate.generate_statistics", () -> "true");
        properties.add("app.jwt.secret", () -> secret);
        properties.add("app.jwt.expiration", () -> "PT1H");
        properties.add("app.roles.initialize", () -> "true");
    }

    @BeforeEach
    void fixtures() {
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        when(clock.instant()).thenReturn(NOW.toInstant(ZoneOffset.UTC));
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        tx = new TransactionTemplate(manager);
        tx.executeWithoutResult(status -> {
            history.deleteAllInBatch();
            details.deleteAllInBatch();
            orders.deleteAllInBatch();
            associations.deleteAllInBatch();
            promotions.deleteAllInBatch();
            vouchers.deleteAllInBatch();
            variants.deleteAllInBatch();
            products.deleteAllInBatch();
            categories.deleteAllInBatch();
            sizes.deleteAllInBatch();
            colors.deleteAllInBatch();
            customers.deleteAllInBatch();
            users.deleteAllInBatch();
            for (String name : List.of("A", "B", "ADMIN", "STAFF")) {
                var user = new User();
                user.setUsername("marketing-" + name);
                user.setFullName(name);
                user.setPasswordHash(HASH);
                user.setRole(roles.findByRoleName(name.equals("A") || name.equals("B") ? "CUSTOMER" : name).orElseThrow());
                user.setStatus(UserStatus.ACTIVE);
                accounts.put(name, users.saveAndFlush(user));
                if (name.equals("A") || name.equals("B")) {
                    var customer = new Customer();
                    customer.setUser(user);
                    customer.setCustomerName(name);
                    customers.saveAndFlush(customer);
                }
            }
            var category = new Category();
            category.setCategoryName("Clothing"); category.setStatus(ActiveStatus.ACTIVE); categories.saveAndFlush(category);
            product = new Product();
            product.setCategory(category);
            product.setProductName("Marketing Shirt");
            product.setPrice(new BigDecimal("50"));
            product.setStatus(ActiveStatus.ACTIVE);
            products.saveAndFlush(product);
            var color = new Color();
            color.setColorName("Black");
            colors.saveAndFlush(color);
            var size = new Size();
            size.setSizeName("S");
            sizes.saveAndFlush(size);
            first = variant(size, color, "MARKETING-ONE", "300", 5);
            var size2 = new Size();
            size2.setSizeName("M");
            sizes.saveAndFlush(size2);
            second = variant(size2, color, "MARKETING-TWO", "200", 10);
        });
        clearInvocations(vouchers, variants, orders, details, history);
    }

    private ProductVariant variant(Size size, Color color, String sku, String price, int quantity) {
        var variant = new ProductVariant();
        variant.setProduct(product);
        variant.setSize(size);
        variant.setColor(color);
        variant.setSku(sku);
        variant.setPrice(new BigDecimal(price));
        variant.setQuantity(quantity);
        variant.setStatus(ActiveStatus.ACTIVE);
        return variants.saveAndFlush(variant);
    }

    private Promotion promotion(String name, String percent, boolean linked) {
        return tx.execute(status -> {
            var promotion = new Promotion();
            promotion.setPromotionName(name);
            promotion.setDiscountPercent(new BigDecimal(percent));
            promotion.setStartDate(NOW.minusDays(1));
            promotion.setEndDate(NOW.plusDays(1));
            promotion.setStatus(ActiveStatus.ACTIVE);
            promotions.saveAndFlush(promotion);
            if (linked) {
                var association = new PromotionProduct();
                association.setId(new PromotionProductId(promotion.getPromotionId(), product.getProductId()));
                association.setPromotion(promotion);
                association.setProduct(products.findById(product.getProductId()).orElseThrow());
                associations.saveAndFlush(association);
            }
            return promotion;
        });
    }

    private Voucher voucher(String code, DiscountType type, String discount, int remaining) {
        var voucher = new Voucher();
        voucher.setCode(code);
        voucher.setVoucherName(code);
        voucher.setDiscountType(type);
        voucher.setDiscountValue(new BigDecimal(discount));
        voucher.setMinOrderAmount(BigDecimal.ZERO);
        voucher.setQuantity(remaining);
        voucher.setStartDate(NOW.minusDays(1));
        voucher.setEndDate(NOW.plusDays(1));
        voucher.setStatus(ActiveStatus.ACTIVE);
        return vouchers.saveAndFlush(voucher);
    }

    private String token(String name) {
        return "Bearer " + jwt.issue(new StoreUserDetails(accounts.get(name))).token();
    }
    private Long actor(String name) { return accounts.get(name).getUserId(); }
    private String json(Object value) { return mapper.writeValueAsString(value); }
    private CheckoutRequest checkoutRequest(String code, ProductVariant variant, int quantity) {
        return new CheckoutRequest(List.of(new CheckoutItemRequest(variant.getVariantId(), quantity)), "City", code);
    }
    private ResultActions checkout(CheckoutRequest request) throws Exception {
        return mvc.perform(post("/api/orders").header("Authorization", token("A"))
                .contentType(MediaType.APPLICATION_JSON).content(json(request)));
    }
    private ResultActions preview(String code) throws Exception {
        return mvc.perform(post("/api/vouchers/validate").header("Authorization", token("A"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(new VoucherValidationRequest(code, List.of(new CheckoutItemRequest(first.getVariantId(), 2))))));
    }
    private ResultActions cancel(Long id) throws Exception {
        return mvc.perform(post("/api/orders/" + id + "/cancel").header("Authorization", token("A")));
    }
    private VoucherRequest voucherRequest(String code, String type, String value, int quantity) {
        return new VoucherRequest(code, "Voucher", DiscountType.valueOf(type), new BigDecimal(value),
                BigDecimal.ZERO, null, quantity, NOW.minusDays(1), NOW.plusDays(1), ActiveStatus.ACTIVE);
    }
    private PromotionRequest promotionRequest(String value) {
        return new PromotionRequest("Campaign", "Description", new BigDecimal(value),
                NOW.minusDays(1), NOW.plusDays(1), ActiveStatus.ACTIVE);
    }
    private int remaining(Voucher voucher) {
        return vouchers.findById(voucher.getVoucherId()).orElseThrow().getQuantity();
    }
    private int quantity(ProductVariant variant) {
        return variants.findById(variant.getVariantId()).orElseThrow().getQuantity();
    }
    private void noOrderWrites() {
        assertThat(orders.count()).isZero();
        assertThat(details.count()).isZero();
        assertThat(history.count()).isZero();
        assertThat(quantity(first)).isEqualTo(5);
        assertThat(quantity(second)).isEqualTo(10);
    }

    @Test
    void publicPromotionIsActivePagedAndCatalogHasEffectivePricePerVariant() throws Exception {
        var promotion = promotion("Sale", "10", true);
        mvc.perform(get("/api/promotions/active")).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].promotionId").value(promotion.getPromotionId()));
        mvc.perform(get("/api/products/" + product.getProductId())).andExpect(status().isOk())
                .andExpect(jsonPath("$.basePrice").value(50))
                .andExpect(jsonPath("$.variants[0].price").value(300))
                .andExpect(jsonPath("$.variants[0].effectivePrice").value(270))
                .andExpect(jsonPath("$.variants[1].effectivePrice").value(180))
                .andExpect(jsonPath("$.variants[0].promotion.promotionId").value(promotion.getPromotionId()));
        assertThat(variants.findById(first.getVariantId()).orElseThrow().getPrice()).isEqualByComparingTo("300");
    }

    @ParameterizedTest
    @ValueSource(strings = {"future", "expired", "inactive", "unlinked"})
    void unusablePromotionDoesNotAffectCatalogOrCheckout(String kind) throws Exception {
        var promotion = promotion("Unavailable", "50", !kind.equals("unlinked"));
        tx.executeWithoutResult(status -> {
            var managed = promotions.findById(promotion.getPromotionId()).orElseThrow();
            if (kind.equals("future")) managed.setStartDate(NOW.plusHours(1));
            if (kind.equals("expired")) managed.setEndDate(NOW.minusHours(1));
            if (kind.equals("inactive")) managed.setStatus(ActiveStatus.INACTIVE);
        });
        mvc.perform(get("/api/products/" + product.getProductId() + "/variants"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].effectivePrice").value(300))
                .andExpect(jsonPath("$[0].promotion").isEmpty());
        checkout(checkoutRequest(null, first, 1)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.items[0].unitPrice").value(300));
    }

    @Test
    void multipleCandidatesChooseBestThenLowestIdWithoutDuplicateVariants() throws Exception {
        var lower = promotion("First best", "20", true);
        promotion("Second best", "20", true);
        promotion("Lower discount", "10", true);
        mvc.perform(get("/api/products/" + product.getProductId() + "/variants"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].effectivePrice").value(240))
                .andExpect(jsonPath("$[0].promotion.promotionId").value(lower.getPromotionId()));
    }

    @Test
    void previewAppliesPromotionThenVoucherAndDoesNotConsumeOrReserve() throws Exception {
        promotion("Sale", "10", true);
        var voucher = voucher("SAVE10", DiscountType.PERCENT, "10", 1);
        preview(" save10 ").andExpect(status().isOk()).andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.code").value("SAVE10")).andExpect(jsonPath("$.orderAmount").value(540))
                .andExpect(jsonPath("$.discountAmount").value(54))
                .andExpect(jsonPath("$.shippingFee").value(20000))
                .andExpect(jsonPath("$.finalAmount").value(20486));
        assertThat(remaining(voucher)).isEqualTo(1);
        verify(variants, never()).findByIdForUpdate(anyLong());
        verify(vouchers, never()).findByCodeForUpdate(anyString());
        noOrderWrites();
    }

    @Test
    void previewUsesServerAmountsEvenWhenRequestedQuantityExceedsStock() throws Exception {
        voucher("SAVE", DiscountType.FIXED, "50", 1);
        mvc.perform(post("/api/vouchers/validate").header("Authorization", token("A"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new VoucherValidationRequest("SAVE", List.of(new CheckoutItemRequest(first.getVariantId(), 20))))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.orderAmount").value(6000));
        mvc.perform(post("/api/vouchers/validate").header("Authorization", token("A"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"SAVE\",\"orderAmount\":1}"))
                .andExpect(status().isBadRequest());
        noOrderWrites();
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "inactive", "future", "expired", "exhausted", "minimum"})
    void invalidVoucherRejectsPreviewAndCheckoutWithoutWrites(String condition) throws Exception {
        var voucher = voucher("INVALID", DiscountType.PERCENT, "10", 1);
        tx.executeWithoutResult(status -> {
            var managed = vouchers.findByIdForUpdate(voucher.getVoucherId()).orElseThrow();
            switch (condition) {
                case "inactive" -> managed.setStatus(ActiveStatus.INACTIVE);
                case "future" -> managed.setStartDate(NOW.plusHours(1));
                case "expired" -> managed.setEndDate(NOW.minusHours(1));
                case "exhausted" -> managed.setQuantity(0);
                case "minimum" -> managed.setMinOrderAmount(new BigDecimal("1000"));
            }
        });
        String code = condition.equals("missing") ? "MISSING" : "INVALID";
        preview(code).andExpect(status().isBadRequest());
        checkout(checkoutRequest(code, first, 2)).andExpect(status().isBadRequest());
        assertThat(remaining(voucher)).isEqualTo(condition.equals("exhausted") ? 0 : 1);
        noOrderWrites();
    }

    @Test
    void minimumIsCheckedAfterPromotionAndMaxDiscountIsApplied() throws Exception {
        promotion("Sale", "50", true);
        var voucher = voucher("MIN", DiscountType.PERCENT, "50", 2);
        tx.executeWithoutResult(status -> vouchers.findByIdForUpdate(voucher.getVoucherId()).orElseThrow().setMinOrderAmount(new BigDecimal("400")));
        preview("MIN").andExpect(status().isBadRequest()); // Original 600, post-promotion amount 300.
        tx.executeWithoutResult(status -> {
            var managed = vouchers.findByIdForUpdate(voucher.getVoucherId()).orElseThrow();
            managed.setMinOrderAmount(new BigDecimal("300"));
            managed.setMaxDiscountAmount(new BigDecimal("30"));
        });
        checkout(checkoutRequest("MIN", first, 2)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmount").value(300)).andExpect(jsonPath("$.discountAmount").value(30))
                .andExpect(jsonPath("$.finalAmount").value(20270));
    }

    @Test
    void fixedVoucherCannotMakeNegativeFinalAmount() throws Exception {
        voucher("FIXED", DiscountType.FIXED, "5000", 1);
        checkout(checkoutRequest("FIXED", first, 1)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmount").value(300)).andExpect(jsonPath("$.discountAmount").value(300))
                .andExpect(jsonPath("$.finalAmount").value(20000));
    }

    @Test
    void successfulCheckoutSnapshotsPromotionAndVoucherAndDoesNotMutateCatalogPrice() throws Exception {
        var promotion = promotion("Sale", "10", true);
        var voucher = voucher("SAVE10", DiscountType.PERCENT, "10", 2);
        var result = orderService.checkout(checkoutRequest("save10", first, 2), actor("A"));
        assertThat(result.totalAmount()).isEqualByComparingTo("540");
        assertThat(result.discountAmount()).isEqualByComparingTo("54");
        assertThat(result.shippingFee()).isEqualByComparingTo("20000");
        assertThat(result.finalAmount()).isEqualByComparingTo("20486");
        assertThat(result.voucherCode()).isEqualTo("SAVE10");
        assertThat(result.items().getFirst().unitPrice()).isEqualByComparingTo("270");
        assertThat(result.items().getFirst().discountAmount()).isEqualByComparingTo("30"); // Per unit.
        assertThat(remaining(voucher)).isEqualTo(1);
        tx.executeWithoutResult(status -> {
            assertThat(orders.findById(result.orderId()).orElseThrow().getVoucher().getVoucherId()).isEqualTo(voucher.getVoucherId());
            promotions.findByIdForUpdate(promotion.getPromotionId()).orElseThrow().setDiscountPercent(new BigDecimal("50"));
            var managed = vouchers.findByIdForUpdate(voucher.getVoucherId()).orElseThrow();
            managed.setDiscountValue(new BigDecimal("99"));
            managed.setStatus(ActiveStatus.INACTIVE);
            variants.findByIdForUpdate(first.getVariantId()).orElseThrow().setPrice(new BigDecimal("999"));
        });
        mvc.perform(get("/api/orders/" + result.orderId()).header("Authorization", token("A")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].unitPrice").value(270))
                .andExpect(jsonPath("$.items[0].discountAmount").value(30))
                .andExpect(jsonPath("$.shippingFee").value(20000))
                .andExpect(jsonPath("$.finalAmount").value(20486));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    void blankVoucherCodeIsOptionalAndDoesNotConsumeAnyVoucher(String code) throws Exception {
        var voucher = voucher("UNUSED", DiscountType.PERCENT, "10", 1);
        checkout(checkoutRequest(code, first, 1)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.discountAmount").value(0)).andExpect(jsonPath("$.voucherCode").isEmpty());
        assertThat(remaining(voucher)).isEqualTo(1);
    }

    @Test
    void stockFailureDoesNotConsumeVoucher() throws Exception {
        var voucher = voucher("SAVE", DiscountType.PERCENT, "10", 1);
        checkout(checkoutRequest("SAVE", first, 6)).andExpect(status().isConflict());
        assertThat(remaining(voucher)).isEqualTo(1);
        verify(vouchers, never()).findByCodeForUpdate(anyString());
        noOrderWrites();
    }

    @Test
    void failureAfterVoucherDecrementRollsBackUsageAndAllCheckoutWrites() throws Exception {
        var voucher = voucher("SAVE", DiscountType.PERCENT, "10", 1);
        doThrow(new IllegalStateException("Simulated second detail failure")).when(details)
                .saveAndFlush(argThat(row -> row.getProductVariant().getVariantId().equals(second.getVariantId())));
        checkout(new CheckoutRequest(List.of(new CheckoutItemRequest(second.getVariantId(), 1),
                        new CheckoutItemRequest(first.getVariantId(), 1)), "City", "SAVE"))
                .andExpect(status().isInternalServerError());
        verify(vouchers).findByCodeForUpdate("SAVE");
        assertThat(remaining(voucher)).isEqualTo(1);
        noOrderWrites();
    }

    @Test
    void historyFailureAfterVoucherConsumptionAlsoRollsBackUsage() throws Exception {
        var voucher = voucher("SAVE", DiscountType.PERCENT, "10", 1);
        doThrow(new IllegalStateException("Simulated SALE failure")).when(history).saveAndFlush(any(InventoryTransaction.class));
        checkout(checkoutRequest("SAVE", first, 1)).andExpect(status().isInternalServerError());
        assertThat(remaining(voucher)).isEqualTo(1);
        noOrderWrites();
    }

    @Test
    void cancelRestoresExpiredInactiveVoucherOnlyOnce() throws Exception {
        var voucher = voucher("SAVE", DiscountType.PERCENT, "10", 1);
        Long id = orderService.checkout(checkoutRequest("SAVE", first, 2), actor("A")).orderId();
        tx.executeWithoutResult(status -> {
            var managed = vouchers.findByIdForUpdate(voucher.getVoucherId()).orElseThrow();
            managed.setStatus(ActiveStatus.INACTIVE);
            managed.setEndDate(NOW.minusHours(1));
        });
        cancel(id).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        cancel(id).andExpect(status().isConflict());
        assertThat(remaining(voucher)).isEqualTo(1);
        assertThat(quantity(first)).isEqualTo(5);
        assertThat(history.count()).isEqualTo(2); // SALE + RETURN.
    }

    @Test
    void failureAfterVoucherRestorationRollsBackStockUsageReturnsAndOrderState() throws Exception {
        var voucher = voucher("SAVE", DiscountType.PERCENT, "10", 2);
        Long id = orderService.checkout(checkoutRequest("SAVE", first, 2), actor("A")).orderId();
        doThrow(new IllegalStateException("Simulated cancel order save failure")).when(orders)
                .saveAndFlush(argThat(order -> order.getStatus() == OrderStatus.CANCELLED));
        cancel(id).andExpect(status().isInternalServerError());
        assertThat(remaining(voucher)).isEqualTo(1);
        assertThat(quantity(first)).isEqualTo(3);
        assertThat(orderService.staffDetail(id).status()).isEqualTo(OrderStatus.PENDING);
        assertThat(history.count()).isEqualTo(1);
    }

    @Test
    void voucherRestoreOverflowRollsBackRestocking() throws Exception {
        var voucher = voucher("SAVE", DiscountType.PERCENT, "10", 1);
        Long id = orderService.checkout(checkoutRequest("SAVE", first, 2), actor("A")).orderId();
        tx.executeWithoutResult(status -> vouchers.findByIdForUpdate(voucher.getVoucherId()).orElseThrow().setQuantity(Integer.MAX_VALUE));
        cancel(id).andExpect(status().isConflict());
        assertThat(quantity(first)).isEqualTo(3);
        assertThat(remaining(voucher)).isEqualTo(Integer.MAX_VALUE);
        assertThat(history.count()).isEqualTo(1);
        assertThat(orderService.staffDetail(id).status()).isEqualTo(OrderStatus.PENDING);
    }

    private int competingCheckout(String customer, ProductVariant variant, CountDownLatch start) throws Exception {
        start.await(10, TimeUnit.SECONDS);
        try { orderService.checkout(checkoutRequest("LAST", variant, 1), actor(customer)); return 201; }
        catch (BadRequestException invalidVoucher) { return 400; }
    }

    @Test
    void twoCustomersOnDifferentVariantsCompeteForLastVoucherWithoutSilentFallback() throws Exception {
        var voucher = voucher("LAST", DiscountType.PERCENT, "10", 1);
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(() -> competingCheckout("A", first, start));
            var b = executor.submit(() -> competingCheckout("B", second, start));
            start.countDown();
            assertThat(List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 400);
        }
        assertThat(remaining(voucher)).isZero();
        assertThat(orders.count()).isEqualTo(1);
        assertThat(history.count()).isEqualTo(1);
        assertThat(quantity(first) + quantity(second)).isEqualTo(14);
    }

    @Test
    void twoOrdersRestoreSameVoucherCounterWithoutLostUpdates() throws Exception {
        var voucher = voucher("SAVE", DiscountType.PERCENT, "10", 2);
        Long aId = orderService.checkout(checkoutRequest("SAVE", first, 1), actor("A")).orderId();
        Long bId = orderService.checkout(checkoutRequest("SAVE", second, 1), actor("B")).orderId();
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(() -> { start.await(10, TimeUnit.SECONDS); return orderService.customerCancel(aId, actor("A")); });
            var b = executor.submit(() -> { start.await(10, TimeUnit.SECONDS); return orderService.customerCancel(bId, actor("B")); });
            start.countDown();
            assertThat(a.get(15, TimeUnit.SECONDS).status()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(b.get(15, TimeUnit.SECONDS).status()).isEqualTo(OrderStatus.CANCELLED);
        }
        assertThat(remaining(voucher)).isEqualTo(2);
        assertThat(quantity(first)).isEqualTo(5);
        assertThat(quantity(second)).isEqualTo(10);
    }

    @Test
    void checkoutAcquiresVariantsThenVoucherAndVoucherLocksAreDeclared() throws Exception {
        var voucher = voucher("SAVE", DiscountType.PERCENT, "10", 1);
        clearInvocations(vouchers, variants);
        orderService.checkout(checkoutRequest("SAVE", first, 1), actor("A"));
        var order = inOrder(variants, vouchers);
        order.verify(variants).findByIdForUpdate(first.getVariantId());
        order.verify(vouchers).findByCodeForUpdate("SAVE");
        assertThat(VoucherRepository.class.getMethod("findByCodeForUpdate", String.class).getAnnotation(Lock.class).value())
                .isEqualTo(LockModeType.PESSIMISTIC_WRITE);
        assertThat(VoucherRepository.class.getMethod("findByIdForUpdate", Long.class).getAnnotation(Lock.class).value())
                .isEqualTo(LockModeType.PESSIMISTIC_WRITE);
        assertThat(remaining(voucher)).isZero();
    }

    @Test
    void adminCreatesReadsUpdatesAndDeactivatesMarketingAndNormalizesCodes() throws Exception {
        mvc.perform(post("/api/admin/vouchers").header("Authorization", token("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                        .content(json(voucherRequest(" sale10 ", "PERCENT", "10", 3))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.code").value("SALE10"));
        var voucher = vouchers.findByCodeIgnoreCase("SALE10").orElseThrow();
        mvc.perform(get("/api/admin/vouchers").header("Authorization", token("ADMIN"))).andExpect(status().isOk());
        mvc.perform(get("/api/admin/vouchers/" + voucher.getVoucherId()).header("Authorization", token("ADMIN"))).andExpect(status().isOk());
        var updated = new VoucherRequest("SALE10", "Edited", DiscountType.FIXED, new BigDecimal("20"),
                BigDecimal.ZERO, null, 3, NOW.minusDays(1), NOW.plusDays(1), ActiveStatus.INACTIVE);
        mvc.perform(put("/api/admin/vouchers/" + voucher.getVoucherId()).header("Authorization", token("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(updated)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("INACTIVE"));
        mvc.perform(post("/api/admin/promotions").header("Authorization", token("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(promotionRequest("10")))).andExpect(status().isCreated());
        var promotion = promotions.findAll().getFirst();
        mvc.perform(get("/api/admin/promotions/" + promotion.getPromotionId()).header("Authorization", token("ADMIN"))).andExpect(status().isOk());
        mvc.perform(put("/api/admin/promotions/" + promotion.getPromotionId()).header("Authorization", token("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(promotionRequest("20"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.discountPercent").value(20));
    }

    @Test
    void duplicateCodeCreateOrUpdateReturns409() throws Exception {
        var one = voucher("SALE", DiscountType.PERCENT, "10", 1);
        var other = voucher("OTHER", DiscountType.FIXED, "10", 1);
        mvc.perform(post("/api/admin/vouchers").header("Authorization", token("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                        .content(json(voucherRequest(" sale ", "PERCENT", "20", 1)))).andExpect(status().isConflict());
        mvc.perform(put("/api/admin/vouchers/" + other.getVoucherId()).header("Authorization", token("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(voucherRequest("sale", "PERCENT", "10", 1))))
                .andExpect(status().isConflict());
        assertThat(remaining(one)).isEqualTo(1);
    }

    @Test
    void promotionAssociationsAreValidatedAndOnlyJoinRecordsAreDeleted() throws Exception {
        var promotion = promotion("Campaign", "10", false);
        String path = "/api/admin/promotions/" + promotion.getPromotionId() + "/products/";
        mvc.perform(post(path + product.getProductId()).header("Authorization", token("ADMIN"))).andExpect(status().isCreated());
        mvc.perform(post(path + product.getProductId()).header("Authorization", token("ADMIN"))).andExpect(status().isConflict());
        mvc.perform(post(path + "999999").header("Authorization", token("ADMIN"))).andExpect(status().isNotFound());
        mvc.perform(get(path.substring(0, path.length() - 1)).header("Authorization", token("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(delete(path + product.getProductId()).header("Authorization", token("ADMIN"))).andExpect(status().isNoContent());
        assertThat(products.existsById(product.getProductId())).isTrue();
        assertThat(associations.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "100.01", "150"})
    void invalidPromotionAndVoucherPercentAreRejectedOnCreateAndUpdate(String percent) throws Exception {
        var promotion = promotion("Existing", "10", false);
        var voucher = voucher("EXISTING", DiscountType.PERCENT, "10", 1);
        for (var builder : List.of(post("/api/admin/promotions"), put("/api/admin/promotions/" + promotion.getPromotionId()))) {
            mvc.perform(builder.header("Authorization", token("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                            .content(json(promotionRequest(percent)))).andExpect(status().isBadRequest());
        }
        for (var builder : List.of(post("/api/admin/vouchers"), put("/api/admin/vouchers/" + voucher.getVoucherId()))) {
            mvc.perform(builder.header("Authorization", token("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                            .content(json(voucherRequest("EXISTING", "PERCENT", percent, 1)))).andExpect(status().isBadRequest());
        }
    }

    @Test
    void invalidMarketingTimeAndUnknownFieldsAre400() throws Exception {
        var voucher = new VoucherRequest("TEST", "Voucher", DiscountType.FIXED, new BigDecimal("10"), BigDecimal.ZERO,
                null, 1, NOW, NOW, ActiveStatus.ACTIVE);
        mvc.perform(post("/api/admin/vouchers").header("Authorization", token("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(voucher))).andExpect(status().isBadRequest());
        var promotion = new PromotionRequest("Bad", null, new BigDecimal("10"), NOW.plusDays(1), NOW, ActiveStatus.ACTIVE);
        mvc.perform(post("/api/admin/promotions").header("Authorization", token("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(promotion))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/admin/promotions").header("Authorization", token("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"promotionName\":\"X\",\"discountType\":\"FIXED\"}"))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"A", "STAFF"})
    void customerAndStaffCannotReadOrMutateAdminMarketing(String role) throws Exception {
        mvc.perform(get("/api/admin/vouchers").header("Authorization", token(role))).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/vouchers").header("Authorization", token(role)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(voucherRequest("TEST", "FIXED", "10", 1)))).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/promotions").header("Authorization", token(role)).contentType(MediaType.APPLICATION_JSON)
                        .content(json(promotionRequest("10")))).andExpect(status().isForbidden());
    }

    @Test
    void previewRequiresCustomerAndAnonymousAdminIs401() throws Exception {
        String body = json(new VoucherValidationRequest("TEST", List.of(new CheckoutItemRequest(first.getVariantId(), 1))));
        mvc.perform(post("/api/vouchers/validate").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        for (String role : List.of("STAFF", "ADMIN")) {
            mvc.perform(post("/api/vouchers/validate").header("Authorization", token(role))
                            .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/admin/vouchers")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/promotions/active").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void noHardDeleteAndMissingAdminResourcesAre404() throws Exception {
        var voucher = voucher("TEST", DiscountType.FIXED, "10", 1);
        var promotion = promotion("Test", "10", false);
        mvc.perform(delete("/api/admin/vouchers/" + voucher.getVoucherId()).header("Authorization", token("ADMIN")))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(delete("/api/admin/promotions/" + promotion.getPromotionId()).header("Authorization", token("ADMIN")))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(get("/api/admin/vouchers/999999").header("Authorization", token("ADMIN"))).andExpect(status().isNotFound());
        mvc.perform(get("/api/admin/promotions/999999").header("Authorization", token("ADMIN"))).andExpect(status().isNotFound());
    }

    @Test
    void catalogWithMultiplePromotionCandidatesAndOldOrderViewsAvoidNPlusOne() {
        promotion("One", "10", true);
        promotion("Two", "20", true);
        var voucher = voucher("TEST", DiscountType.PERCENT, "10", 1);
        var result = orderService.checkout(new CheckoutRequest(List.of(new CheckoutItemRequest(first.getVariantId(), 1),
                new CheckoutItemRequest(second.getVariantId(), 1)), "City", "TEST"), actor("A"));
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        assertThat(catalog.getPublicDetail(product.getProductId()).variants()).hasSize(2);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(3);
        statistics.clear();
        assertThat(orderService.staffDetail(result.orderId()).items()).hasSize(2);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
        assertThat(remaining(voucher)).isZero();
    }
}
