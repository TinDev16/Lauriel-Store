package com.lauriel.store.controller;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.lauriel.store.dto.request.*;
import com.lauriel.store.dto.response.OrderResponse;
import com.lauriel.store.entity.*;
import com.lauriel.store.entity.Order;
import com.lauriel.store.entity.enums.*;
import com.lauriel.store.exception.ResourceConflictException;
import com.lauriel.store.repository.*;
import com.lauriel.store.security.*;
import com.lauriel.store.service.*;
import com.lauriel.store.support.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class OrderCheckoutApiTest {
    private static final String HASH = new BCryptPasswordEncoder().encode("TestPassword123");
    @Autowired private WebApplicationContext context;
    @MockitoSpyBean private OrderRepository orders;
    @MockitoSpyBean private OrderDetailRepository details;
    @MockitoSpyBean private ProductVariantRepository variants;
    @MockitoSpyBean private InventoryTransactionRepository history;
    @Autowired private CustomerRepository customers;
    @Autowired private PaymentRepository payments;
    @Autowired private ProductRepository products;
    @Autowired private SizeRepository sizes;
    @Autowired private ColorRepository colors;
    @Autowired private UserRepository users;
    @Autowired private RoleRepository roles;
    @Autowired private JwtService jwt;
    @Autowired private OrderService service;
    @Autowired private InventoryService inventory;
    @Autowired private ObjectMapper mapper;
    @Autowired private PlatformTransactionManager manager;
    @Autowired private EntityManager entityManager;
    @Autowired private EntityManagerFactory entityManagerFactory;
    private MockMvc mvc;
    private TransactionTemplate tx;
    private ProductVariant first, second;
    private final Map<String, User> accounts = new HashMap<>();
    private final Map<String, Customer> profiles = new HashMap<>();

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
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        tx = new TransactionTemplate(manager);
        // Real committed fixtures, isolated from MySQL and from every other test class.
        tx.executeWithoutResult(status -> {
            history.deleteAllInBatch();
            payments.deleteAllInBatch();
            details.deleteAllInBatch();
            orders.deleteAllInBatch();
            variants.deleteAllInBatch();
            products.deleteAllInBatch();
            sizes.deleteAllInBatch();
            colors.deleteAllInBatch();
            customers.deleteAllInBatch();
            users.deleteAllInBatch();
            for (String name : List.of("A", "B", "NO_PROFILE", "ADMIN", "STAFF")) {
                var user = new User();
                user.setUsername("checkout-" + name);
                user.setFullName(name);
                user.setPasswordHash(HASH);
                user.setRole(roles.findByRoleName(name.equals("ADMIN") || name.equals("STAFF") ? name : "CUSTOMER").orElseThrow());
                user.setStatus(UserStatus.ACTIVE);
                users.saveAndFlush(user);
                accounts.put(name, user);
                if (name.equals("A") || name.equals("B")) {
                    var customer = new Customer();
                    customer.setUser(user);
                    customer.setCustomerName(name);
                    customer.setAddress("Old Customer Address");
                    customer.setPhone("0901234567");
                    profiles.put(name, customers.saveAndFlush(customer));
                }
            }
            var size = new Size();
            size.setSizeName("S");
            sizes.saveAndFlush(size);
            var color = new Color();
            color.setColorName("Black");
            colors.saveAndFlush(color);
            var product = new Product();
            product.setProductName("Checkout Shirt");
            product.setPrice(new BigDecimal("50")); // Intentionally differs from the real variant sale price.
            product.setStatus(ActiveStatus.ACTIVE);
            products.saveAndFlush(product);
            first = variant(product, size, color, "BUY-ONE", "120.50", 5);
            var size2 = new Size();
            size2.setSizeName("M");
            sizes.saveAndFlush(size2);
            second = variant(product, size2, color, "BUY-TWO", "200", 10);
        });
        clearInvocations(orders, details, variants, history);
    }

    private ProductVariant variant(Product product, Size size, Color color, String sku, String price, int quantity) {
        var variant = new ProductVariant();
        variant.setProduct(product);
        variant.setSize(size);
        variant.setColor(color);
        variant.setSku(sku);
        variant.setQuantity(quantity);
        variant.setPrice(new BigDecimal(price));
        variant.setStatus(ActiveStatus.ACTIVE);
        return variants.saveAndFlush(variant);
    }

    private String token(String name) {
        return "Bearer " + jwt.issue(new StoreUserDetails(accounts.get(name))).token();
    }

    private Long actor(String name) { return accounts.get(name).getUserId(); }
    private String json(Object value) { return mapper.writeValueAsString(value); }
    private CheckoutItemRequest item(ProductVariant variant, int quantity) {
        return new CheckoutItemRequest(variant.getVariantId(), quantity);
    }
    private CheckoutRequest request(CheckoutItemRequest... items) {
        return new CheckoutRequest(List.of(items), "  Shipping Snapshot  ");
    }
    private Long buy(String customer, CheckoutItemRequest... items) {
        return service.checkout(request(items), actor(customer)).orderId();
    }
    private int quantity(ProductVariant variant) {
        return variants.findById(variant.getVariantId()).orElseThrow().getQuantity();
    }
    private ResultActions checkout(String body, String customer) throws Exception {
        return mvc.perform(post("/api/orders").header("Authorization", token(customer))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }
    private ResultActions cancel(Long id, String customer) throws Exception {
        return mvc.perform(post("/api/orders/" + id + "/cancel").header("Authorization", token(customer)));
    }
    private ResultActions changeStatus(Long id, String target, String role) throws Exception {
        return mvc.perform(patch("/api/staff/orders/" + id + "/status").header("Authorization", token(role))
                .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("status", target))));
    }
    private long movements(InventoryTransactionType type) {
        return history.count((root, query, builder) -> builder.equal(root.get("transactionType"), type));
    }
    private void noCheckoutWrites() {
        assertThat(orders.count()).isZero();
        assertThat(details.count()).isZero();
        assertThat(history.count()).isZero();
        assertThat(quantity(first)).isEqualTo(5);
        assertThat(quantity(second)).isEqualTo(10);
    }

    @Test
    void customerCheckoutPricesFromDatabaseAndAtomicallyCreatesDetailsStockAndSale() throws Exception {
        checkout(json(request(item(second, 3), item(first, 2))), "A")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.customerId").value(profiles.get("A").getCustomerId()))
                .andExpect(jsonPath("$.shippingAddress").value("Shipping Snapshot"))
                .andExpect(jsonPath("$.totalAmount").value(841)).andExpect(jsonPath("$.finalAmount").value(841))
                .andExpect(jsonPath("$.discountAmount").value(0))
                .andExpect(jsonPath("$.items[0].unitPrice").value(120.5))
                .andExpect(jsonPath("$.items[0].subtotal").value(241))
                .andExpect(jsonPath("$.items[0].size.sizeName").value("S"))
                .andExpect(jsonPath("$.items[0].discountAmount").value(0))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        assertThat(quantity(first)).isEqualTo(3);
        assertThat(quantity(second)).isEqualTo(7);
        assertThat(details.count()).isEqualTo(2);
        assertThat(payments.count()).isZero();
        tx.executeWithoutResult(status -> {
            var order = orders.findAll().getFirst();
            assertThat(order.getVoucher()).isNull();
            assertThat(history.findAll()).hasSize(2).allSatisfy(row -> {
                assertThat(row.getTransactionType()).isEqualTo(InventoryTransactionType.SALE);
                assertThat(row.getQuantity()).isPositive();
                assertThat(row.getReferenceType()).isEqualTo("ORDER");
                assertThat(row.getReferenceId()).isEqualTo(order.getOrderId());
                assertThat(row.getUser().getUserId()).isEqualTo(actor("A"));
            });
        });
    }

    @Test
    void anonymousCannotCheckoutAndAdminStaffCannotUseCustomerOrderApi() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(json(request(item(first, 1)))))
                .andExpect(status().isUnauthorized()).andExpect(header().doesNotExist("Location"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        for (String role : List.of("ADMIN", "STAFF")) {
            checkout(json(request(item(first, 1))), role).andExpect(status().isForbidden());
            mvc.perform(get("/api/orders/my-orders").header("Authorization", token(role)))
                    .andExpect(status().isForbidden());
        }
        noCheckoutWrites();
    }

    @Test
    void missingVariantIs404AndInsufficientStockIs409() throws Exception {
        checkout(json(request(new CheckoutItemRequest(999999L, 1))), "A").andExpect(status().isNotFound());
        checkout(json(request(item(first, 6))), "A").andExpect(status().isConflict())
                .andExpect(content().string(containsString("tồn kho")));
        noCheckoutWrites();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "2147483648", "null"})
    void invalidQuantityIs400(String count) throws Exception {
        checkout("{\"items\":[{\"variantId\":" + first.getVariantId() + ",\"quantity\":" + count + "}],\"shippingAddress\":\"City\"}", "A")
                .andExpect(status().isBadRequest());
        noCheckoutWrites();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{\"items\":[],\"shippingAddress\":\"City\"}",
        "{\"shippingAddress\":\"City\"}",
        "{\"items\":[null],\"shippingAddress\":\"City\"}",
        "{\"items\":[{\"variantId\":0,\"quantity\":1}],\"shippingAddress\":\"City\"}",
        "{\"items\":[{\"quantity\":1}],\"shippingAddress\":\"City\"}"
    })
    void invalidCartIs400(String body) throws Exception {
        checkout(body, "A").andExpect(status().isBadRequest());
        noCheckoutWrites();
    }

    @ParameterizedTest
    @ValueSource(strings = {"customerId", "userId", "unitPrice", "subtotal", "totalAmount", "discountAmount",
            "finalAmount", "voucherCode", "voucherId", "phone", "note", "status"})
    void clientCannotSupplyOwnerPricesDiscountsOrUnsupportedOrderFields(String field) throws Exception {
        var body = new HashMap<String, Object>();
        body.put("items", List.of(item(first, 1)));
        body.put("shippingAddress", "City");
        body.put(field, 1);
        checkout(json(body), "A").andExpect(status().isBadRequest());
        noCheckoutWrites();
    }

    @ParameterizedTest
    @ValueSource(strings = {"unitPrice", "subtotal", "discountAmount", "price"})
    void clientPricesAreRejectedInsideItems(String field) throws Exception {
        checkout(json(Map.of("items", List.of(Map.of("variantId", first.getVariantId(), "quantity", 1, field, 1)),
                "shippingAddress", "City")), "A").andExpect(status().isBadRequest());
        noCheckoutWrites();
    }

    @Test
    void addressAndCartLimitsAreValidated() throws Exception {
        for (String address : List.of("", " ", "đ".repeat(32768))) {
            checkout(json(new CheckoutRequest(List.of(item(first, 1)), address)), "A").andExpect(status().isBadRequest());
        }
        checkout(json(new CheckoutRequest(Collections.nCopies(101, item(first, 1)), "City")), "A")
                .andExpect(status().isBadRequest());
        noCheckoutWrites();
    }

    @Test
    void duplicateItemsAreMergedAndLockedOnlyOnce() throws Exception {
        checkout(json(request(item(first, 2), item(first, 3))), "A")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].quantity").value(5))
                .andExpect(jsonPath("$.totalAmount").value(602.5));
        verify(variants, times(1)).findByIdForUpdate(first.getVariantId());
        assertThat(quantity(first)).isZero();
        assertThat(details.count()).isEqualTo(1);
        assertThat(movements(InventoryTransactionType.SALE)).isEqualTo(1);
    }

    @Test
    void duplicateQuantityOverflowIs400BeforeLockingOrCreatingRows() throws Exception {
        checkout(json(request(item(first, Integer.MAX_VALUE), item(first, 1))), "A").andExpect(status().isBadRequest());
        verify(variants, never()).findByIdForUpdate(anyLong());
        noCheckoutWrites();
    }

    @Test
    void missingCustomerProfileIs404AndNoProfileIsCreatedImplicitly() throws Exception {
        checkout(json(request(item(first, 1))), "NO_PROFILE").andExpect(status().isNotFound());
        assertThat(customers.count()).isEqualTo(2);
        noCheckoutWrites();
    }

    @Test
    void oldOrderKeepsPriceAndShippingSnapshotAfterCurrentDataChanges() throws Exception {
        Long id = buy("A", item(first, 2));
        tx.executeWithoutResult(status -> {
            variants.findByIdForUpdate(first.getVariantId()).orElseThrow().setPrice(new BigDecimal("999"));
            customers.findById(profiles.get("A").getCustomerId()).orElseThrow().setAddress("New Address");
        });
        mvc.perform(get("/api/orders/" + id).header("Authorization", token("A")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].unitPrice").value(120.5))
                .andExpect(jsonPath("$.items[0].subtotal").value(241))
                .andExpect(jsonPath("$.shippingAddress").value("Shipping Snapshot"))
                .andExpect(jsonPath("$.finalAmount").value(241));
    }

    @Test
    void zeroVariantPriceIsAllowedAndFinalStockCannotBeSoldAgain() throws Exception {
        tx.executeWithoutResult(status -> variants.findByIdForUpdate(first.getVariantId()).orElseThrow().setPrice(BigDecimal.ZERO));
        checkout(json(request(item(first, 5))), "A").andExpect(status().isCreated())
                .andExpect(jsonPath("$.finalAmount").value(0));
        checkout(json(request(item(first, 1))), "B").andExpect(status().isConflict());
        assertThat(quantity(first)).isZero();
        assertThat(orders.count()).isEqualTo(1);
    }

    @Test
    void inactiveVariantOrProductCannotBeCheckedOut() throws Exception {
        tx.executeWithoutResult(status -> variants.findByIdForUpdate(first.getVariantId()).orElseThrow().setStatus(ActiveStatus.INACTIVE));
        checkout(json(request(item(first, 1))), "A").andExpect(status().isConflict());
        tx.executeWithoutResult(status -> products.findById(second.getProduct().getProductId()).orElseThrow().setStatus(ActiveStatus.INACTIVE));
        checkout(json(request(item(second, 1))), "A").andExpect(status().isConflict());
        noCheckoutWrites();
    }

    @Test
    void lineAmountOverflowIs400WithoutCreatingOrder() throws Exception {
        tx.executeWithoutResult(status -> {
            var variant = variants.findByIdForUpdate(first.getVariantId()).orElseThrow();
            variant.setPrice(new BigDecimal("9999999999.99"));
            variant.setQuantity(200);
        });
        checkout(json(request(item(first, 101))), "A").andExpect(status().isBadRequest());
        assertThat(orders.count()).isZero();
        assertThat(history.count()).isZero();
        assertThat(quantity(first)).isEqualTo(200);
    }

    @Test
    void aggregateAmountOverflowIs400BeforeStockChanges() throws Exception {
        tx.executeWithoutResult(status -> {
            for (var seed : List.of(first, second)) {
                var variant = variants.findByIdForUpdate(seed.getVariantId()).orElseThrow();
                variant.setPrice(new BigDecimal("9999999999.99"));
                variant.setQuantity(60);
            }
        });
        checkout(json(request(item(first, 60), item(second, 60))), "A").andExpect(status().isBadRequest());
        assertThat(orders.count()).isZero();
        assertThat(details.count()).isZero();
        assertThat(history.count()).isZero();
        assertThat(quantity(first)).isEqualTo(60);
        assertThat(quantity(second)).isEqualTo(60);
    }

    @Test
    void failureOnSecondDetailRollsBackOrderAndFirstDetail() throws Exception {
        doThrow(new IllegalStateException("Simulated second detail failure")).when(details)
                .saveAndFlush(argThat(row -> row.getProductVariant().getVariantId().equals(second.getVariantId())));
        checkout(json(request(item(first, 2), item(second, 3))), "A").andExpect(status().isInternalServerError())
                .andExpect(content().string(not(containsString("Simulated second detail failure"))));
        verify(details).saveAndFlush(argThat(row -> row.getProductVariant().getVariantId().equals(first.getVariantId())));
        noCheckoutWrites();
    }

    @Test
    void historyFailureAfterManagedStockDecreaseRollsBackAllCheckoutRows() throws Exception {
        doAnswer(invocation -> {
            InventoryTransaction movement = invocation.getArgument(0);
            assertThat(movement.getProductVariant().getQuantity()).isEqualTo(3);
            throw new IllegalStateException("Simulated SALE failure");
        }).when(history).saveAndFlush(any(InventoryTransaction.class));
        checkout(json(request(item(first, 2))), "A").andExpect(status().isInternalServerError());
        noCheckoutWrites();
    }

    @Test
    void failureAfterFirstSaleWasFlushedRollsBackStockHistoryAndEntireOrder() throws Exception {
        doThrow(new IllegalStateException("Simulated second SALE failure")).when(history)
                .saveAndFlush(argThat(row -> row.getProductVariant().getVariantId().equals(second.getVariantId())));
        checkout(json(request(item(second, 3), item(first, 2))), "A").andExpect(status().isInternalServerError());
        verify(history).saveAndFlush(argThat(row -> row.getProductVariant().getVariantId().equals(first.getVariantId())));
        noCheckoutWrites();
    }

    @Test
    void customerListIsScopedAndPaginatedAndOtherCustomersGet404() throws Exception {
        Long aId = buy("A", item(first, 1));
        buy("A", item(second, 1));
        buy("B", item(second, 1));
        mvc.perform(get("/api/orders/my-orders").header("Authorization", token("A")).param("size", "1")
                        .param("customerId", profiles.get("B").getCustomerId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].customerId").value(profiles.get("A").getCustomerId()))
                .andExpect(jsonPath("$.content[0].items").doesNotExist());
        mvc.perform(get("/api/orders/" + aId).header("Authorization", token("A"))).andExpect(status().isOk());
        mvc.perform(get("/api/orders/" + aId).header("Authorization", token("B"))).andExpect(status().isNotFound());
        mvc.perform(get("/api/orders/999999").header("Authorization", token("A"))).andExpect(status().isNotFound());
        cancel(aId, "B").andExpect(status().isNotFound());
        assertThat(quantity(first)).isEqualTo(4);
        assertThat(movements(InventoryTransactionType.RETURN)).isZero();
    }

    @Test
    void customerCancelRestoresStockAndReturnHistoryOnlyOnce() throws Exception {
        Long id = buy("A", item(second, 3), item(first, 2));
        cancel(id, "A").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        cancel(id, "A").andExpect(status().isConflict());
        assertThat(quantity(first)).isEqualTo(5);
        assertThat(quantity(second)).isEqualTo(10);
        assertThat(movements(InventoryTransactionType.SALE)).isEqualTo(2);
        assertThat(movements(InventoryTransactionType.RETURN)).isEqualTo(2);
        tx.executeWithoutResult(status -> {
            history.findAll().stream().filter(row -> row.getTransactionType() == InventoryTransactionType.RETURN).forEach(row -> {
                assertThat(row.getQuantity()).isPositive();
                assertThat(row.getReferenceType()).isEqualTo("ORDER");
                assertThat(row.getReferenceId()).isEqualTo(id);
                assertThat(row.getUser().getUserId()).isEqualTo(actor("A"));
            });
        });
    }

    @Test
    void confirmedCanBeCancelledByCustomerAndProcessingOnlyByStaff() throws Exception {
        Long confirmed = buy("A", item(first, 1));
        changeStatus(confirmed, "CONFIRMED", "STAFF").andExpect(status().isOk());
        cancel(confirmed, "A").andExpect(status().isOk());
        Long processing = buy("A", item(first, 2));
        changeStatus(processing, "CONFIRMED", "ADMIN").andExpect(status().isOk());
        changeStatus(processing, "PROCESSING", "STAFF").andExpect(status().isOk());
        cancel(processing, "A").andExpect(status().isConflict());
        changeStatus(processing, "CANCELLED", "STAFF").andExpect(status().isOk());
        assertThat(quantity(first)).isEqualTo(5);
        assertThat(movements(InventoryTransactionType.RETURN)).isEqualTo(2);
    }

    @Test
    void validForwardLifecycleDoesNotTouchStockOrHistoryAgain() throws Exception {
        Long id = buy("A", item(first, 1));
        for (String next : List.of("CONFIRMED", "PROCESSING", "SHIPPING", "DELIVERED")) {
            changeStatus(id, next, "STAFF").andExpect(status().isOk()).andExpect(jsonPath("$.status").value(next));
        }
        cancel(id, "A").andExpect(status().isConflict());
        changeStatus(id, "CANCELLED", "ADMIN").andExpect(status().isConflict());
        changeStatus(id, "PENDING", "STAFF").andExpect(status().isConflict());
        assertThat(quantity(first)).isEqualTo(4);
        assertThat(history.count()).isEqualTo(1);
    }

    @Test
    void shippingCannotBeCancelledByEitherRole() throws Exception {
        Long id = buy("A", item(first, 1));
        service.updateStatus(id, OrderStatus.CONFIRMED, actor("STAFF"));
        service.updateStatus(id, OrderStatus.PROCESSING, actor("STAFF"));
        service.updateStatus(id, OrderStatus.SHIPPING, actor("STAFF"));
        cancel(id, "A").andExpect(status().isConflict());
        changeStatus(id, "CANCELLED", "STAFF").andExpect(status().isConflict());
        assertThat(quantity(first)).isEqualTo(4);
        assertThat(movements(InventoryTransactionType.RETURN)).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"PENDING", "PROCESSING", "SHIPPING", "DELIVERED", "RETURNED"})
    void invalidForwardTransitionsReturn409(String next) throws Exception {
        Long id = buy("A", item(first, 1));
        changeStatus(id, next, "STAFF").andExpect(status().isConflict());
        assertThat(service.staffDetail(id).status()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void cancelledCannotBeReactivatedAndCustomerCannotSetStatus() throws Exception {
        Long id = buy("A", item(first, 1));
        changeStatus(id, "CONFIRMED", "A").andExpect(status().isForbidden());
        changeStatus(id, "CANCELLED", "ADMIN").andExpect(status().isOk());
        changeStatus(id, "CONFIRMED", "STAFF").andExpect(status().isConflict());
        changeStatus(id, "CANCELLED", "ADMIN").andExpect(status().isConflict());
        assertThat(quantity(first)).isEqualTo(5);
        assertThat(movements(InventoryTransactionType.RETURN)).isEqualTo(1);
    }

    @Test
    void failureDuringRestockRollsBackReturnHistoryStockAndStatus() throws Exception {
        Long id = buy("A", item(first, 2), item(second, 3));
        doThrow(new IllegalStateException("Simulated second RETURN failure")).when(history)
                .saveAndFlush(argThat(row -> row.getTransactionType() == InventoryTransactionType.RETURN
                        && row.getProductVariant().getVariantId().equals(second.getVariantId())));
        cancel(id, "A").andExpect(status().isInternalServerError());
        assertThat(quantity(first)).isEqualTo(3);
        assertThat(quantity(second)).isEqualTo(7);
        assertThat(service.staffDetail(id).status()).isEqualTo(OrderStatus.PENDING);
        assertThat(movements(InventoryTransactionType.SALE)).isEqualTo(2);
        assertThat(movements(InventoryTransactionType.RETURN)).isZero();
    }

    @Test
    void restockOverflowReturns409WithoutPartialStockRestoration() throws Exception {
        Long id = buy("A", item(first, 2), item(second, 3));
        tx.executeWithoutResult(status -> variants.findByIdForUpdate(second.getVariantId()).orElseThrow().setQuantity(Integer.MAX_VALUE));
        cancel(id, "A").andExpect(status().isConflict());
        assertThat(quantity(first)).isEqualTo(3);
        assertThat(quantity(second)).isEqualTo(Integer.MAX_VALUE);
        assertThat(service.staffDetail(id).status()).isEqualTo(OrderStatus.PENDING);
        assertThat(movements(InventoryTransactionType.RETURN)).isZero();
    }

    @Test
    void staffListFiltersAndDetailWorkAndCustomerCannotUseStaffEndpoints() throws Exception {
        Long id = buy("A", item(first, 1));
        Long other = buy("B", item(second, 1));
        service.updateStatus(other, OrderStatus.CONFIRMED, actor("STAFF"));
        mvc.perform(get("/api/staff/orders").header("Authorization", token("STAFF")).param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get("/api/staff/orders").header("Authorization", token("ADMIN")).param("status", "PENDING")
                        .param("from", "2000-01-01T00:00:00").param("to", "2100-01-01T00:00:00"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/staff/orders/" + id).header("Authorization", token("STAFF"))).andExpect(status().isOk());
        mvc.perform(get("/api/staff/orders").header("Authorization", token("A"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/staff/orders/" + id).header("Authorization", token("A"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/staff/orders")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/orders/my-orders")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/api/orders/my-orders?page=-1", "/api/orders/my-orders?size=101",
        "/api/orders/my-orders?size=0", "/api/orders/my-orders?size=100&page=2147483647",
        "/api/staff/orders?status=WRONG", "/api/staff/orders?from=bad",
        "/api/staff/orders?from=2100-01-01T00:00:00&to=2000-01-01T00:00:00"
    })
    void badReadParametersAre400(String path) throws Exception {
        mvc.perform(get(path).header("Authorization", token(path.startsWith("/api/staff") ? "STAFF" : "A")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidStatusRequestAndMissingStaffOrderReturnCorrectErrors() throws Exception {
        Long id = buy("A", item(first, 1));
        changeStatus(id, "WRONG", "STAFF").andExpect(status().isBadRequest());
        changeStatus(999999L, "CONFIRMED", "STAFF").andExpect(status().isNotFound());
        mvc.perform(patch("/api/staff/orders/" + id + "/status").header("Authorization", token("STAFF"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CONFIRMED\",\"totalAmount\":1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void lockConfigurationAndSortedCheckoutAndRestockAreVerified() throws Exception {
        assertThat(OrderRepository.class.getMethod("findByIdForUpdate", Long.class).getAnnotation(Lock.class).value())
                .isEqualTo(LockModeType.PESSIMISTIC_WRITE);
        assertThat(OrderRepository.class.getMethod("findOwnedByIdForUpdate", Long.class, Long.class).getAnnotation(Lock.class).value())
                .isEqualTo(LockModeType.PESSIMISTIC_WRITE);
        Long id = buy("A", item(second, 3), item(first, 2));
        var checkoutOrder = inOrder(variants);
        checkoutOrder.verify(variants).findByIdForUpdate(first.getVariantId());
        checkoutOrder.verify(variants).findByIdForUpdate(second.getVariantId());
        verify(variants, times(1)).findByIdForUpdate(first.getVariantId());
        verify(variants, times(1)).findByIdForUpdate(second.getVariantId());
        clearInvocations(orders, variants);
        service.customerCancel(id, actor("A"));
        var cancellationOrder = inOrder(orders, variants);
        cancellationOrder.verify(orders).findOwnedByIdForUpdate(id, profiles.get("A").getCustomerId());
        cancellationOrder.verify(variants).findByIdForUpdate(first.getVariantId());
        cancellationOrder.verify(variants).findByIdForUpdate(second.getVariantId());
    }

    @Test
    void internalSaleOperationsRequireTransactionAndWriteLock() {
        assertThatThrownBy(() -> inventory.lockForSale(Map.of(first.getVariantId(), 1)))
                .isInstanceOf(IllegalTransactionStateException.class);
        tx.executeWithoutResult(status -> {
            var unlocked = variants.findById(first.getVariantId()).orElseThrow();
            var line = new OrderDetail();
            line.setId(new OrderDetailId(1L, first.getVariantId()));
            line.setProductVariant(unlocked);
            line.setQuantity(1);
            assertThatThrownBy(() -> inventory.decreaseForSale(1L, List.of(line), accounts.get("A")))
                    .isInstanceOf(IllegalStateException.class);
            status.setRollbackOnly();
        });
        noCheckoutWrites();
    }

    private int attemptCheckout(String customer, CountDownLatch start) throws Exception {
        start.await(10, TimeUnit.SECONDS);
        try {
            buy(customer, item(first, 1));
            return 201;
        } catch (ResourceConflictException conflict) {
            return 409;
        }
    }

    @Test
    void twoCustomersCompetingForFinalUnitProduceOnlyOneOrder() throws Exception {
        tx.executeWithoutResult(status -> variants.findByIdForUpdate(first.getVariantId()).orElseThrow().setQuantity(1));
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(() -> attemptCheckout("A", start));
            var b = executor.submit(() -> attemptCheckout("B", start));
            start.countDown();
            assertThat(List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
        }
        assertThat(quantity(first)).isZero();
        assertThat(orders.count()).isEqualTo(1);
        assertThat(details.count()).isEqualTo(1);
        assertThat(movements(InventoryTransactionType.SALE)).isEqualTo(1);
    }

    private int attemptCancel(Long id, CountDownLatch start) throws Exception {
        start.await(10, TimeUnit.SECONDS);
        try {
            service.customerCancel(id, actor("A"));
            return 200;
        } catch (ResourceConflictException conflict) {
            return 409;
        }
    }

    @Test
    void concurrentCancelsRestoreStockOnlyOnce() throws Exception {
        Long id = buy("A", item(first, 2));
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(() -> attemptCancel(id, start));
            var b = executor.submit(() -> attemptCancel(id, start));
            start.countDown();
            assertThat(List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 409);
        }
        assertThat(quantity(first)).isEqualTo(5);
        assertThat(movements(InventoryTransactionType.RETURN)).isEqualTo(1);
    }

    @Test
    void cancellationRacingShippingTransitionLeavesAConsistentState() throws Exception {
        Long id = buy("A", item(first, 2));
        service.updateStatus(id, OrderStatus.CONFIRMED, actor("STAFF"));
        service.updateStatus(id, OrderStatus.PROCESSING, actor("STAFF"));
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var cancel = executor.submit(() -> {
                start.await(10, TimeUnit.SECONDS);
                try { service.updateStatus(id, OrderStatus.CANCELLED, actor("ADMIN")); return 200; }
                catch (ResourceConflictException conflict) { return 409; }
            });
            var ship = executor.submit(() -> {
                start.await(10, TimeUnit.SECONDS);
                try { service.updateStatus(id, OrderStatus.SHIPPING, actor("STAFF")); return 200; }
                catch (ResourceConflictException conflict) { return 409; }
            });
            start.countDown();
            assertThat(List.of(cancel.get(15, TimeUnit.SECONDS), ship.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 409);
        }
        var state = service.staffDetail(id).status();
        assertThat(state).isIn(OrderStatus.SHIPPING, OrderStatus.CANCELLED);
        assertThat(quantity(first)).isEqualTo(state == OrderStatus.CANCELLED ? 5 : 3);
        assertThat(movements(InventoryTransactionType.RETURN)).isEqualTo(state == OrderStatus.CANCELLED ? 1L : 0L);
    }

    @Test
    void customerCancelRacingConfirmationRemainsConsistentWithoutDoubleRestock() throws Exception {
        Long id = buy("A", item(first, 2));
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var cancel = executor.submit(() -> attemptCancel(id, start));
            var confirm = executor.submit(() -> {
                start.await(10, TimeUnit.SECONDS);
                try { service.updateStatus(id, OrderStatus.CONFIRMED, actor("STAFF")); return 200; }
                catch (ResourceConflictException conflict) { return 409; }
            });
            start.countDown();
            assertThat(cancel.get(15, TimeUnit.SECONDS)).isEqualTo(200);
            assertThat(confirm.get(15, TimeUnit.SECONDS)).isIn(200, 409);
        }
        assertThat(service.staffDetail(id).status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(quantity(first)).isEqualTo(5);
        assertThat(movements(InventoryTransactionType.RETURN)).isEqualTo(1);
    }

    @Test
    void orderWithoutRecordedSaleCannotAddStockThroughCancellation() throws Exception {
        Long id = tx.execute(status -> {
            var order = new Order();
            order.setCustomer(customers.findById(profiles.get("A").getCustomerId()).orElseThrow());
            order.setShippingAddress("Legacy Address");
            order.setStatus(OrderStatus.PENDING);
            order.setTotalAmount(new BigDecimal("120.50"));
            order.setFinalAmount(new BigDecimal("120.50"));
            orders.saveAndFlush(order);
            var line = new OrderDetail();
            line.setId(new OrderDetailId(order.getOrderId(), first.getVariantId()));
            line.setOrder(order);
            line.setProductVariant(variants.findById(first.getVariantId()).orElseThrow());
            line.setQuantity(1);
            line.setUnitPrice(new BigDecimal("120.50"));
            line.setSubtotal(new BigDecimal("120.50"));
            details.saveAndFlush(line);
            return order.getOrderId();
        });
        clearInvocations(variants);
        cancel(id, "A").andExpect(status().isConflict());
        verify(variants, never()).findByIdForUpdate(anyLong());
        assertThat(quantity(first)).isEqualTo(5);
        assertThat(history.count()).isZero();
        assertThat(service.staffDetail(id).status()).isEqualTo(OrderStatus.PENDING);
    }
    @Test
    void orderReadQueryCountsStayConstantWithMultipleItems() {
        Long id = buy("A", item(first, 1), item(second, 1));
        buy("A", item(second, 1));
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        assertThat(service.staffList(0, 1, null, null, null).totalElements()).isEqualTo(2);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2); // Orders/customer graph + count.
        statistics.clear();
        assertThat(service.staffDetail(id).items()).hasSize(2);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2); // Order + graph items.
        statistics.clear();
        assertThat(service.myOrders(actor("A"), 0, 1).totalElements()).isEqualTo(2);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(3); // Profile + orders + count.
        statistics.clear();
        assertThat(service.myDetail(id, actor("A")).items()).hasSize(2);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(3); // Profile + order + graph items.
    }
}
