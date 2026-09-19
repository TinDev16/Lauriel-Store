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
import com.lauriel.store.entity.*;
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
class InventoryPurchaseApiTest {
    private static final String PREFIX = "/api/staff";
    private static final String HASH = new BCryptPasswordEncoder().encode("TestPassword123");
    @Autowired private WebApplicationContext context;
    @Autowired private SupplierRepository suppliers;
    @MockitoSpyBean private PurchaseReceiptRepository receipts;
    @Autowired private PurchaseReceiptDetailRepository items;
    @MockitoSpyBean private ProductVariantRepository variants;
    @MockitoSpyBean private InventoryTransactionRepository history;
    @Autowired private ProductRepository products;
    @Autowired private SizeRepository sizes;
    @Autowired private ColorRepository colors;
    @Autowired private UserRepository users;
    @Autowired private RoleRepository roles;
    @Autowired private JwtService jwt;
    @Autowired private PurchaseReceiptService service;
    @Autowired private InventoryService inventory;
    @Autowired private SupplierService supplierService;
    @Autowired private ObjectMapper mapper;
    @Autowired private PlatformTransactionManager manager;
    @Autowired private EntityManager entityManager;
    @Autowired private EntityManagerFactory entityManagerFactory;
    private MockMvc mvc;
    private TransactionTemplate tx;
    private Supplier supplier;
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
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        tx = new TransactionTemplate(manager);
        // Committed fixtures in this test's private H2 database: HTTP tests exercise real service commits/rollbacks.
        tx.executeWithoutResult(status -> {
            history.deleteAllInBatch();
            items.deleteAllInBatch();
            receipts.deleteAllInBatch();
            variants.deleteAllInBatch();
            products.deleteAllInBatch();
            sizes.deleteAllInBatch();
            colors.deleteAllInBatch();
            suppliers.deleteAllInBatch();
            users.deleteAllInBatch();
            for (String role : List.of("ADMIN", "STAFF", "CUSTOMER")) {
                var user = new User();
                user.setUsername("inventory-" + role);
                user.setFullName(role);
                user.setPasswordHash(HASH);
                user.setRole(roles.findByRoleName(role).orElseThrow());
                user.setStatus(UserStatus.ACTIVE);
                accounts.put(role, users.saveAndFlush(user));
            }
            var size = new Size();
            size.setSizeName("S");
            sizes.saveAndFlush(size);
            var color = new Color();
            color.setColorName("Black");
            colors.saveAndFlush(color);
            var product = new Product();
            product.setProductName("Stock Shirt");
            product.setPrice(new BigDecimal("100"));
            product.setStatus(ActiveStatus.ACTIVE);
            products.saveAndFlush(product);
            first = variant(product, size, color, "STOCK-ONE", 5);
            var size2 = new Size();
            size2.setSizeName("M");
            sizes.saveAndFlush(size2);
            second = variant(product, size2, color, "STOCK-TWO", 10);
            supplier = new Supplier();
            supplier.setSupplierName("Local Supplier");
            supplier.setStatus(ActiveStatus.ACTIVE);
            suppliers.saveAndFlush(supplier);
        });
        clearInvocations(variants, receipts, history);
    }

    private ProductVariant variant(Product product, Size size, Color color, String sku, int quantity) {
        var variant = new ProductVariant();
        variant.setProduct(product);
        variant.setSize(size);
        variant.setColor(color);
        variant.setSku(sku);
        variant.setQuantity(quantity);
        variant.setPrice(new BigDecimal("120"));
        variant.setStatus(ActiveStatus.ACTIVE);
        return variants.saveAndFlush(variant);
    }

    private String token(String role) {
        return "Bearer " + jwt.issue(new StoreUserDetails(accounts.get(role))).token();
    }

    private Long actor(String role) {
        return accounts.get(role).getUserId();
    }

    private String json(Object value) {
        return mapper.writeValueAsString(value);
    }

    private Long draft() {
        return service.create(new PurchaseReceiptCreateRequest(supplier.getSupplierId()), actor("STAFF")).receiptId();
    }

    private void add(Long receipt, ProductVariant variant, int quantity, String price) {
        service.addItem(receipt, new PurchaseReceiptDetailRequest(variant.getVariantId(), quantity, new BigDecimal(price)));
    }

    private int quantity(ProductVariant variant) {
        return variants.findById(variant.getVariantId()).orElseThrow().getQuantity();
    }

    private ResultActions itemRequest(Long id, String body) throws Exception {
        return mvc.perform(post(PREFIX + "/purchase-receipts/" + id + "/items")
                .header("Authorization", token("STAFF")).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions completeRequest(Long id) throws Exception {
        return mvc.perform(post(PREFIX + "/purchase-receipts/" + id + "/complete")
                .header("Authorization", token("ADMIN")));
    }

    @Test
    void staffListsAndReadsSupplier() throws Exception {
        mvc.perform(get(PREFIX + "/suppliers").header("Authorization", token("STAFF")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].supplierName").value("Local Supplier"));
        mvc.perform(get(PREFIX + "/suppliers/" + supplier.getSupplierId()).header("Authorization", token("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.supplierId").value(supplier.getSupplierId()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "STAFF"})
    void authorizedRolesCreateSupplier(String role) throws Exception {
        mvc.perform(post(PREFIX + "/suppliers").header("Authorization", token(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"supplierName\":\"New Supplier\",\"phone\":\"0901234567\",\"email\":\"supplier@example.com\",\"address\":\"City\",\"status\":\"ACTIVE\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void supplierUpdateCanDeactivateWithoutBreakingReceiptReferences() throws Exception {
        Long id = draft();
        mvc.perform(put(PREFIX + "/suppliers/" + supplier.getSupplierId()).header("Authorization", token("STAFF"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"supplierName\":\"Renamed\",\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk());
        mvc.perform(get(PREFIX + "/purchase-receipts/" + id).header("Authorization", token("STAFF")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.supplier.supplierName").value("Renamed"));
        assertThat(receipts.count()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{\"supplierName\":\" \",\"status\":\"ACTIVE\"}",
        "{\"supplierName\":\"X\",\"status\":\"WRONG\"}",
        "{\"supplierName\":\"X\",\"status\":\"ACTIVE\",\"email\":\"wrong\"}",
        "{\"supplierName\":\"X\",\"status\":\"ACTIVE\",\"supplierId\":1}"
    })
    void invalidSupplierIs400(String body) throws Exception {
        mvc.perform(post(PREFIX + "/suppliers").header("Authorization", token("STAFF"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        assertThat(suppliers.count()).isEqualTo(1);
    }

    @Test
    void supplierStorageLimitsAndMissingLookup() throws Exception {
        for (Map<String, Object> body : List.of(
                Map.<String, Object>of("supplierName", "X".repeat(201), "status", "ACTIVE"),
                Map.<String, Object>of("supplierName", "X", "status", "ACTIVE", "phone", "1".repeat(21)),
                Map.<String, Object>of("supplierName", "X", "status", "ACTIVE", "address", "đ".repeat(32768)))) {
            mvc.perform(post(PREFIX + "/suppliers").header("Authorization", token("STAFF"))
                            .contentType(MediaType.APPLICATION_JSON).content(json(body)))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(get(PREFIX + "/suppliers/999999").header("Authorization", token("STAFF")))
                .andExpect(status().isNotFound());
        mvc.perform(put(PREFIX + "/suppliers/999999").header("Authorization", token("STAFF"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"supplierName\":\"X\",\"status\":\"ACTIVE\"}"))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/suppliers", "/purchase-receipts", "/inventory", "/inventory/low-stock", "/inventory/transactions"})
    void customerAndAnonymousCannotReadManagement(String path) throws Exception {
        mvc.perform(get(PREFIX + path)).andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Location")).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        mvc.perform(get(PREFIX + path).header("Authorization", token("CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousAndCustomerCannotMutateManagement() throws Exception {
        Long id = draft();
        for (String role : List.of("ANONYMOUS", "CUSTOMER")) {
            int expected = role.equals("ANONYMOUS") ? 401 : 403;
            var create = post(PREFIX + "/purchase-receipts").contentType(MediaType.APPLICATION_JSON)
                    .content(json(Map.of("supplierId", supplier.getSupplierId())));
            var complete = post(PREFIX + "/purchase-receipts/" + id + "/complete");
            var supplierPost = post(PREFIX + "/suppliers").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"supplierName\":\"X\",\"status\":\"ACTIVE\"}");
            if (!role.equals("ANONYMOUS")) {
                create.header("Authorization", token(role));
                complete.header("Authorization", token(role));
                supplierPost.header("Authorization", token(role));
            }
            mvc.perform(create).andExpect(status().is(expected));
            mvc.perform(complete).andExpect(status().is(expected));
            mvc.perform(supplierPost).andExpect(status().is(expected));
        }
        assertThat(quantity(first)).isEqualTo(5);
        assertThat(history.count()).isZero();
    }

    @Test
    void receiptCreatorComesFromJwtAndCreationDoesNotChangeStock() throws Exception {
        mvc.perform(post(PREFIX + "/purchase-receipts").header("Authorization", token("STAFF"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("supplierId", supplier.getSupplierId()))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.createdBy.userId").value(actor("STAFF")))
                .andExpect(jsonPath("$.totalAmount").value(0)).andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.createdBy.passwordHash").doesNotExist());
        assertThat(quantity(first)).isEqualTo(5);
        assertThat(history.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"createdByUserId", "userId", "status", "totalAmount"})
    void clientCannotSetReceiptCreatorStateOrTotal(String field) throws Exception {
        mvc.perform(post(PREFIX + "/purchase-receipts").header("Authorization", token("STAFF"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(Map.of("supplierId", supplier.getSupplierId(), field, 1))))
                .andExpect(status().isBadRequest());
        assertThat(receipts.count()).isZero();
    }

    @Test
    void missingSupplierAndReceiptAre404() throws Exception {
        mvc.perform(post(PREFIX + "/purchase-receipts").header("Authorization", token("STAFF"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"supplierId\":999999}"))
                .andExpect(status().isNotFound());
        mvc.perform(get(PREFIX + "/purchase-receipts/999999").header("Authorization", token("STAFF")))
                .andExpect(status().isNotFound());
        itemRequest(999999L, json(Map.of("variantId", first.getVariantId(), "quantity", 2, "unitPrice", 10)))
                .andExpect(status().isNotFound());
        completeRequest(999999L).andExpect(status().isNotFound());
    }

    @Test
    void addItemComputesServerAmountsAndDoesNotChangeStock() throws Exception {
        Long id = draft();
        itemRequest(id, json(Map.of("variantId", first.getVariantId(), "quantity", 3, "unitPrice", new BigDecimal("12.50"))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.totalAmount").value(37.5))
                .andExpect(jsonPath("$.items[0].lineTotal").value(37.5))
                .andExpect(jsonPath("$.items[0].quantity").value(3))
                .andExpect(jsonPath("$.items[0].productName").value("Stock Shirt"))
                .andExpect(jsonPath("$.items[0].size.sizeName").value("S"));
        assertThat(quantity(first)).isEqualTo(5);
        assertThat(history.count()).isZero();
    }

    @Test
    void duplicateItemIs409AndKeepsOriginalTotal() throws Exception {
        Long id = draft();
        add(id, first, 2, "10");
        itemRequest(id, json(Map.of("variantId", first.getVariantId(), "quantity", 8, "unitPrice", 20)))
                .andExpect(status().isConflict());
        assertThat(service.detail(id).totalAmount()).isEqualByComparingTo("20");
        assertThat(service.detail(id).items()).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "2147483648"})
    void invalidItemQuantityIs400(String quantity) throws Exception {
        Long id = draft();
        itemRequest(id, "{\"variantId\":" + first.getVariantId() + ",\"quantity\":" + quantity + ",\"unitPrice\":10}")
                .andExpect(status().isBadRequest());
        assertThat(items.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "1.001", "10000000000"})
    void invalidItemPriceIs400(String price) throws Exception {
        Long id = draft();
        itemRequest(id, "{\"variantId\":" + first.getVariantId() + ",\"quantity\":1,\"unitPrice\":" + price + "}")
                .andExpect(status().isBadRequest());
        assertThat(items.count()).isZero();
    }

    @Test
    void zeroUnitPriceIsAllowedAndClientSubtotalRejected() throws Exception {
        Long id = draft();
        itemRequest(id, json(Map.of("variantId", first.getVariantId(), "quantity", 2, "unitPrice", 0)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.totalAmount").value(0));
        itemRequest(id, json(Map.of("variantId", second.getVariantId(), "quantity", 2, "unitPrice", 1, "subtotal", 2)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingVariantAndMissingItemAre404() throws Exception {
        Long id = draft();
        itemRequest(id, "{\"variantId\":999999,\"quantity\":2,\"unitPrice\":10}")
                .andExpect(status().isNotFound());
        mvc.perform(put(PREFIX + "/purchase-receipts/" + id + "/items/" + first.getVariantId())
                        .header("Authorization", token("STAFF")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":2,\"unitPrice\":10}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void draftItemCanBeUpdatedAndDeletedAndTotalIsRecomputed() throws Exception {
        Long id = draft();
        add(id, first, 2, "10");
        add(id, second, 3, "20");
        mvc.perform(put(PREFIX + "/purchase-receipts/" + id + "/items/" + first.getVariantId())
                        .header("Authorization", token("STAFF")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":4,\"unitPrice\":12.50}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalAmount").value(110));
        mvc.perform(delete(PREFIX + "/purchase-receipts/" + id + "/items/" + second.getVariantId())
                        .header("Authorization", token("STAFF")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalAmount").value(50))
                .andExpect(jsonPath("$.items.length()").value(1));
        assertThat(quantity(first)).isEqualTo(5);
        assertThat(quantity(second)).isEqualTo(10);
        assertThat(history.count()).isZero();
    }

    @Test
    void itemUpdateCannotReplacePathVariantId() throws Exception {
        Long id = draft();
        add(id, first, 2, "10");
        mvc.perform(put(PREFIX + "/purchase-receipts/" + id + "/items/" + first.getVariantId())
                        .header("Authorization", token("STAFF")).contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("variantId", second.getVariantId(), "quantity", 3, "unitPrice", 20))))
                .andExpect(status().isBadRequest());
        assertThat(service.detail(id).totalAmount()).isEqualByComparingTo("20");
    }

    @Test
    void lineTotalOverflowIs400AndRollsBackAdd() throws Exception {
        Long id = draft();
        itemRequest(id, json(Map.of("variantId", first.getVariantId(), "quantity", 101, "unitPrice", new BigDecimal("9999999999.99"))))
                .andExpect(status().isBadRequest());
        assertThat(items.count()).isZero();
        assertThat(service.detail(id).totalAmount()).isEqualByComparingTo("0");
    }

    @Test
    void aggregateTotalOverflowRollsBackSecondLineAndUpdate() throws Exception {
        Long id = draft();
        add(id, first, 60, "9999999999.99");
        itemRequest(id, json(Map.of("variantId", second.getVariantId(), "quantity", 60, "unitPrice", new BigDecimal("9999999999.99"))))
                .andExpect(status().isBadRequest());
        add(id, second, 1, "1");
        mvc.perform(put(PREFIX + "/purchase-receipts/" + id + "/items/" + second.getVariantId())
                        .header("Authorization", token("STAFF")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":60,\"unitPrice\":9999999999.99}"))
                .andExpect(status().isBadRequest());
        assertThat(service.detail(id).items().get(1).quantity()).isEqualTo(1);
        assertThat(service.detail(id).totalAmount()).isEqualByComparingTo("600000000000.40");
    }

    @Test
    void completingMultipleItemsUpdatesStockAndHistoryAndRecordsCompleter() throws Exception {
        Long id = draft();
        add(id, second, 4, "20");
        add(id, first, 3, "10");
        completeRequest(id).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.totalAmount").value(110))
                .andExpect(jsonPath("$.createdBy.userId").value(actor("STAFF")));
        assertThat(quantity(first)).isEqualTo(8);
        assertThat(quantity(second)).isEqualTo(14);
        tx.executeWithoutResult(status -> {
            assertThat(history.findAll()).hasSize(2).allSatisfy(row -> {
                assertThat(row.getTransactionType()).isEqualTo(InventoryTransactionType.IMPORT);
                assertThat(row.getQuantity()).isPositive();
                assertThat(row.getReferenceType()).isEqualTo("PURCHASE_RECEIPT");
                assertThat(row.getReferenceId()).isEqualTo(id);
                assertThat(row.getUser().getUserId()).isEqualTo(actor("ADMIN"));
            });
        });
    }

    @Test
    void completeTwiceReturns409AndCannotImportAgain() throws Exception {
        Long id = draft();
        add(id, first, 3, "10");
        completeRequest(id).andExpect(status().isOk());
        completeRequest(id).andExpect(status().isConflict());
        assertThat(quantity(first)).isEqualTo(8);
        assertThat(history.count()).isEqualTo(1);
        assertThat(service.detail(id).status()).isEqualTo(PurchaseReceiptStatus.COMPLETED);
    }

    @Test
    void emptyReceiptCannotBeCompleted() throws Exception {
        Long id = draft();
        completeRequest(id).andExpect(status().isConflict());
        assertThat(service.detail(id).status()).isEqualTo(PurchaseReceiptStatus.DRAFT);
        assertThat(history.count()).isZero();
    }

    @Test
    void completedReceiptCannotAddEditDeleteOrCancel() throws Exception {
        Long id = draft();
        add(id, first, 3, "10");
        service.complete(id, actor("STAFF"));
        itemRequest(id, json(Map.of("variantId", second.getVariantId(), "quantity", 2, "unitPrice", 10)))
                .andExpect(status().isConflict());
        mvc.perform(put(PREFIX + "/purchase-receipts/" + id + "/items/" + first.getVariantId())
                        .header("Authorization", token("STAFF")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":8,\"unitPrice\":20}"))
                .andExpect(status().isConflict());
        mvc.perform(delete(PREFIX + "/purchase-receipts/" + id + "/items/" + first.getVariantId())
                        .header("Authorization", token("STAFF")))
                .andExpect(status().isConflict());
        mvc.perform(post(PREFIX + "/purchase-receipts/" + id + "/cancel").header("Authorization", token("STAFF")))
                .andExpect(status().isConflict());
        assertThat(quantity(first)).isEqualTo(8);
        assertThat(history.count()).isEqualTo(1);
        assertThat(service.detail(id).totalAmount()).isEqualByComparingTo("30");
    }

    @Test
    void cancellingDraftDoesNotImportAndBlocksFurtherChanges() throws Exception {
        Long id = draft();
        add(id, first, 3, "10");
        mvc.perform(post(PREFIX + "/purchase-receipts/" + id + "/cancel").header("Authorization", token("STAFF")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        completeRequest(id).andExpect(status().isConflict());
        itemRequest(id, json(Map.of("variantId", second.getVariantId(), "quantity", 2, "unitPrice", 10)))
                .andExpect(status().isConflict());
        assertThat(quantity(first)).isEqualTo(5);
        assertThat(history.count()).isZero();
    }

    @Test
    void failureAfterFirstHistoryInsertRollsBackAllStockHistoryAndStatus() throws Exception {
        Long id = draft();
        add(id, first, 3, "10");
        add(id, second, 4, "20");
        doThrow(new IllegalStateException("Simulated history failure")).when(history)
                .saveAndFlush(argThat(row -> row.getProductVariant().getVariantId().equals(second.getVariantId())));
        completeRequest(id).andExpect(status().isInternalServerError())
                .andExpect(content().string(not(containsString("Simulated history failure"))));
        verify(history).saveAndFlush(argThat(row -> row.getProductVariant().getVariantId().equals(first.getVariantId())));
        assertThat(quantity(first)).isEqualTo(5);
        assertThat(quantity(second)).isEqualTo(10);
        assertThat(history.count()).isZero();
        assertThat(service.detail(id).status()).isEqualTo(PurchaseReceiptStatus.DRAFT);
        assertThat(service.detail(id).totalAmount()).isEqualByComparingTo("110");
    }

    @Test
    void stockOverflowIs409AndDoesNotPartiallyImportOtherLines() throws Exception {
        tx.executeWithoutResult(status -> {
            var variant = variants.findByIdForUpdate(second.getVariantId()).orElseThrow();
            variant.setQuantity(Integer.MAX_VALUE);
        });
        Long id = draft();
        add(id, first, 3, "10");
        add(id, second, 1, "20");
        completeRequest(id).andExpect(status().isConflict());
        assertThat(quantity(first)).isEqualTo(5);
        assertThat(quantity(second)).isEqualTo(Integer.MAX_VALUE);
        assertThat(history.count()).isZero();
        assertThat(service.detail(id).status()).isEqualTo(PurchaseReceiptStatus.DRAFT);
    }

    @Test
    void lockingMethodsExistAndCompletionAcquiresReceiptThenVariantsInIdOrder() throws Exception {
        assertThat(ProductVariantRepository.class.getMethod("findByIdForUpdate", Long.class)
                .getAnnotation(Lock.class).value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
        assertThat(PurchaseReceiptRepository.class.getMethod("findByIdForUpdate", Long.class)
                .getAnnotation(Lock.class).value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
        Long id = draft();
        add(id, second, 2, "20");
        add(id, first, 3, "10");
        clearInvocations(receipts, variants);
        service.complete(id, actor("STAFF"));
        var order = inOrder(receipts, variants);
        order.verify(receipts).findByIdForUpdate(id);
        order.verify(variants).findByIdForUpdate(first.getVariantId());
        order.verify(variants).findByIdForUpdate(second.getVariantId());
    }

    @Test
    void internalImportRequiresAnExistingTransaction() {
        assertThatThrownBy(() -> inventory.importReceipt(1L, List.of(), accounts.get("STAFF")))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    private int concurrentComplete(Long id, CountDownLatch start) throws Exception {
        start.await(10, TimeUnit.SECONDS);
        try {
            service.complete(id, actor("STAFF"));
            return 200;
        } catch (ResourceConflictException conflict) {
            return 409;
        }
    }

    @Test
    void twoConcurrentCompletionsOfSameReceiptImportOnlyOnce() throws Exception {
        Long id = draft();
        add(id, first, 3, "10");
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(() -> concurrentComplete(id, start));
            var b = executor.submit(() -> concurrentComplete(id, start));
            start.countDown();
            assertThat(List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 409);
        }
        assertThat(quantity(first)).isEqualTo(8);
        assertThat(history.count()).isEqualTo(1);
    }

    @Test
    void twoReceiptsImportingSameVariantsDoNotLoseStockUpdates() throws Exception {
        Long aId = draft();
        Long bId = draft();
        add(aId, second, 4, "20");
        add(aId, first, 3, "10");
        add(bId, first, 7, "10");
        add(bId, second, 2, "20");
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(() -> concurrentComplete(aId, start));
            var b = executor.submit(() -> concurrentComplete(bId, start));
            start.countDown();
            assertThat(a.get(15, TimeUnit.SECONDS)).isEqualTo(200);
            assertThat(b.get(15, TimeUnit.SECONDS)).isEqualTo(200);
        }
        assertThat(quantity(first)).isEqualTo(15);
        assertThat(quantity(second)).isEqualTo(16);
        assertThat(history.count()).isEqualTo(4);
    }

    @Test
    void inventoryReturnsActualQuantityAndLowStockUsesInclusiveThreshold() throws Exception {
        mvc.perform(get(PREFIX + "/inventory").header("Authorization", token("STAFF"))
                        .param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content[0].quantity").value(5))
                .andExpect(jsonPath("$.content[0].productName").value("Stock Shirt"));
        mvc.perform(get(PREFIX + "/inventory/" + second.getVariantId()).header("Authorization", token("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.quantity").value(10));
        mvc.perform(get(PREFIX + "/inventory/low-stock").header("Authorization", token("STAFF")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get(PREFIX + "/inventory/low-stock").header("Authorization", token("STAFF")).param("threshold", "10"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get(PREFIX + "/inventory/low-stock").header("Authorization", token("STAFF")).param("threshold", "0"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty());
        mvc.perform(get(PREFIX + "/inventory/999999").header("Authorization", token("STAFF")))
                .andExpect(status().isNotFound());
    }

    @Test
    void historyPaginationAndFiltersWorkAndDoNotExposeUserSecrets() throws Exception {
        Long id = draft();
        add(id, first, 3, "10");
        add(id, second, 4, "20");
        service.complete(id, actor("ADMIN"));
        mvc.perform(get(PREFIX + "/inventory/transactions").header("Authorization", token("STAFF")).param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2)).andExpect(jsonPath("$.content[0].type").value("IMPORT"))
                .andExpect(jsonPath("$.content[0].performedBy.userId").value(actor("ADMIN")))
                .andExpect(jsonPath("$.content[0].performedBy.passwordHash").doesNotExist());
        mvc.perform(get(PREFIX + "/inventory/transactions").header("Authorization", token("STAFF"))
                        .param("variantId", first.getVariantId().toString()).param("type", "IMPORT")
                        .param("from", "2000-01-01T00:00:00").param("to", "2100-01-01T00:00:00"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].quantity").value(3));
        mvc.perform(get(PREFIX + "/inventory/transactions").header("Authorization", token("STAFF")).param("type", "SALE"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/inventory?size=101", "/inventory?page=-1", "/inventory?size=0",
        "/inventory?size=100&page=2147483647", "/inventory/low-stock?threshold=-1",
        "/inventory/transactions?type=WRONG", "/inventory/transactions?variantId=0",
        "/inventory/transactions?from=bad", "/inventory/transactions?from=2100-01-01T00:00:00&to=2000-01-01T00:00:00",
        "/suppliers?size=101", "/purchase-receipts?size=0"
    })
    void badReadParametersAre400(String path) throws Exception {
        mvc.perform(get(PREFIX + path).header("Authorization", token("STAFF")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void receiptListIsPaginatedAndDoesNotLoadItems() throws Exception {
        draft();
        Long id = draft();
        add(id, first, 3, "10");
        mvc.perform(get(PREFIX + "/purchase-receipts").header("Authorization", token("STAFF")).param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].receiptId").value(id))
                .andExpect(jsonPath("$.content[0].items").doesNotExist());
    }

    @Test
    void fetchedReadsAvoidNPlusOne() {
        Long id = draft();
        add(id, first, 3, "10");
        add(id, second, 4, "20");
        service.complete(id, actor("STAFF"));
        draft();
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        assertThat(inventory.list(0, 1, null).totalElements()).isEqualTo(2);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
        statistics.clear();
        assertThat(service.list(0, 1).totalElements()).isEqualTo(2);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
        statistics.clear();
        assertThat(service.detail(id).items()).hasSize(2);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
        statistics.clear();
        assertThat(inventory.history(0, 1, null, null, null, null).totalElements()).isEqualTo(2);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
    }

    @Test
    void noSupplierReceiptDeleteOrDirectStockMutationEndpoint() throws Exception {
        Long id = draft();
        mvc.perform(delete(PREFIX + "/suppliers/" + supplier.getSupplierId()).header("Authorization", token("STAFF")))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(delete(PREFIX + "/purchase-receipts/" + id).header("Authorization", token("STAFF")))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(put(PREFIX + "/inventory/" + first.getVariantId()).header("Authorization", token("STAFF"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":999}"))
                .andExpect(status().isMethodNotAllowed());
        assertThat(quantity(first)).isEqualTo(5);
    }
}
