package com.lauriel.store.controller;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.lauriel.store.dto.request.*;
import com.lauriel.store.entity.*;
import com.lauriel.store.entity.enums.*;
import com.lauriel.store.repository.*;
import com.lauriel.store.security.StoreUserDetails;
import com.lauriel.store.service.*;
import com.lauriel.store.support.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class WebAdminTest {

    private static Path imageDirectory;
    private static final String PASSWORD = "WebFixturePassword123";
    private static final String HASH = new BCryptPasswordEncoder().encode(
        PASSWORD
    );

    @Autowired
    WebApplicationContext context;

    @Autowired
    ProductRepository products;

    @Autowired
    ProductImageRepository productImages;

    @Autowired
    CategoryRepository categories;

    @Autowired
    ProductVariantRepository variants;

    @Autowired
    SizeRepository sizes;

    @Autowired
    ColorRepository colors;

    @Autowired
    SupplierRepository suppliers;

    @Autowired
    PurchaseReceiptRepository receipts;

    @Autowired
    PurchaseReceiptDetailRepository receiptItems;

    @Autowired
    InventoryTransactionRepository history;

    @Autowired
    OrderRepository orders;

    @Autowired
    OrderDetailRepository orderItems;

    @Autowired
    VoucherRepository vouchers;

    @Autowired
    PromotionRepository promotions;

    @Autowired
    PromotionProductRepository associations;

    @Autowired
    PromotionVariantRepository variantPromotions;

    @Autowired
    CustomerRepository customers;

    @Autowired
    UserRepository users;

    @Autowired
    RoleRepository roles;

    @Autowired
    OrderService orderService;

    @Autowired
    PurchaseReceiptService receiptService;

    @Autowired
    PlatformTransactionManager manager;

    private MockMvc mvc;
    private TransactionTemplate tx;
    private final Map<String, User> accounts = new HashMap<>();
    private Category category;
    private Product product;
    private ProductVariant variant;
    private Supplier supplier;
    private Voucher voucher;
    private Promotion promotion;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry p) throws Exception {
        String url = TestSchema.createIsolatedDatabase(),
            secret = TestSecrets.randomBase64();
        p.add("spring.datasource.url", () -> url);
        p.add("spring.datasource.username", () -> "sa");
        p.add("spring.datasource.password", () -> "");
        p.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        p.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        p.add("spring.jpa.open-in-view", () -> "false");
        p.add("app.jwt.secret", () -> secret);
        imageDirectory = Files.createTempDirectory("lauriel-product-images-");
        p.add("app.images.directory", () -> imageDirectory.toString());
    }

    @BeforeEach
    void fixtures() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
            .apply(springSecurity())
            .build();
        tx = new TransactionTemplate(manager);
        tx.executeWithoutResult(s -> {
            history.deleteAllInBatch();
            orderItems.deleteAllInBatch();
            orders.deleteAllInBatch();
            receiptItems.deleteAllInBatch();
            receipts.deleteAllInBatch();
            suppliers.deleteAllInBatch();
            variantPromotions.deleteAllInBatch();
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
            for (String role : List.of("ADMIN", "STAFF", "CUSTOMER")) {
                var u = new User();
                u.setUsername("web-" + role);
                u.setFullName("Fixture " + role);
                u.setPasswordHash(HASH);
                u.setRole(roles.findByRoleName(role).orElseThrow());
                u.setStatus(UserStatus.ACTIVE);
                accounts.put(role, users.saveAndFlush(u));
                if (role.equals("CUSTOMER")) {
                    var c = new Customer();
                    c.setUser(u);
                    c.setCustomerName("Web customer");
                    c.setPhone("0900000000");
                    customers.saveAndFlush(c);
                }
            }
            var walkIn = new Customer();
            walkIn.setCustomerName("Walk-in customer");
            customers.saveAndFlush(walkIn);
            category = new Category();
            category.setCategoryName("Clothing");
            category.setStatus(ActiveStatus.ACTIVE);
            category = categories.saveAndFlush(category);
            product = new Product();
            product.setCategory(category);
            product.setProductName("Web shirt");
            product.setPrice(new BigDecimal("250000"));
            product.setStatus(ActiveStatus.INACTIVE);
            products.saveAndFlush(product);
            var size = new Size();
            size.setSizeName("M");
            sizes.saveAndFlush(size);
            var color = new Color();
            color.setColorName("Green");
            colors.saveAndFlush(color);
            variant = new ProductVariant();
            variant.setProduct(product);
            variant.setSize(size);
            variant.setColor(color);
            variant.setSku("WEB-SKU");
            variant.setPrice(new BigDecimal("250000"));
            variant.setQuantity(5);
            variant.setStatus(ActiveStatus.INACTIVE);
            variants.saveAndFlush(variant);
            supplier = new Supplier();
            supplier.setSupplierName("Web supplier");
            supplier.setStatus(ActiveStatus.ACTIVE);
            suppliers.saveAndFlush(supplier);
            voucher = new Voucher();
            voucher.setCode("WEB10");
            voucher.setVoucherName("Web voucher");
            voucher.setDiscountType(DiscountType.PERCENT);
            voucher.setDiscountValue(new BigDecimal("10"));
            voucher.setMinOrderAmount(BigDecimal.ZERO);
            voucher.setQuantity(2);
            voucher.setStartDate(LocalDateTime.now().minusDays(1));
            voucher.setEndDate(LocalDateTime.now().plusDays(1));
            voucher.setStatus(ActiveStatus.ACTIVE);
            vouchers.saveAndFlush(voucher);
            promotion = new Promotion();
            promotion.setPromotionName("Web campaign");
            promotion.setDiscountPercent(new BigDecimal("20"));
            promotion.setStartDate(LocalDateTime.now().minusDays(1));
            promotion.setEndDate(LocalDateTime.now().plusDays(1));
            promotion.setStatus(ActiveStatus.INACTIVE);
            promotions.saveAndFlush(promotion);
        });
    }

    private MockHttpServletRequestBuilder admin(
        MockHttpServletRequestBuilder r
    ) {
        return r.with(user(new StoreUserDetails(accounts.get("ADMIN"))));
    }

    private org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder admin(
        org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder r
    ) {
        return r.with(user(new StoreUserDetails(accounts.get("ADMIN"))));
    }

    private MockHttpServletRequestBuilder staff(
        MockHttpServletRequestBuilder r
    ) {
        return r.with(user(new StoreUserDetails(accounts.get("STAFF"))));
    }

    private String productPath() {
        return "/admin/products/" + product.getProductId();
    }

    private String voucherPath() {
        return "/admin/vouchers/" + voucher.getVoucherId();
    }

    private String promotionPath() {
        return "/admin/promotions/" + promotion.getPromotionId();
    }

    private String receipt() {
        return (
            "/admin/purchase-receipts/" +
            receiptService
                .create(
                    new PurchaseReceiptCreateRequest(supplier.getSupplierId()),
                    accounts.get("STAFF").getUserId()
                )
                .receiptId()
        );
    }

    private Long checkout(boolean useVoucher) {
        tx.executeWithoutResult(s -> {
            var p = products.findById(product.getProductId()).orElseThrow();
            p.setStatus(ActiveStatus.ACTIVE);
            products.saveAndFlush(p);
            var v = variants.findById(variant.getVariantId()).orElseThrow();
            v.setStatus(ActiveStatus.ACTIVE);
            variants.saveAndFlush(v);
        });
        return orderService
            .checkout(
                new CheckoutRequest(
                    List.of(new CheckoutItemRequest(variant.getVariantId(), 2)),
                    "12 Web Street",
                    useVoucher ? "WEB10" : null
                ),
                accounts.get("CUSTOMER").getUserId()
            )
            .orderId();
    }

    private MockHttpServletRequestBuilder productForm(
        String path,
        String name
    ) {
        return productForm(path, name, "300000");
    }

    private MockHttpServletRequestBuilder productForm(
        String path,
        String name,
        String price
    ) {
        return admin(
            post(path)
                .with(csrf())
                .param("productName", name)
                .param("description", "Web description")
                .param("categoryId", category.getCategoryId().toString())
                .param("price", price)
                .param("imgUrl", "")
                .param("status", "ACTIVE")
        );
    }

    private MockHttpServletRequestBuilder voucherForm(
        String path,
        String code
    ) {
        return voucherForm(path, code, "15", "2027-01-01T00:00");
    }

    private MockHttpServletRequestBuilder voucherForm(
        String path,
        String code,
        String discount,
        String end
    ) {
        return admin(
            post(path)
                .with(csrf())
                .param("code", code)
                .param("voucherName", "Updated voucher")
                .param("discountType", "PERCENT")
                .param("discountValue", discount)
                .param("minOrderAmount", "0")
                .param("quantity", "3")
                .param("startDate", "2026-01-01T00:00")
                .param("endDate", end)
                .param("status", "ACTIVE")
        );
    }

    private MockHttpServletRequestBuilder promotionForm(
        String path,
        String name
    ) {
        return admin(
            post(path)
                .with(csrf())
                .param("promotionName", name)
                .param("description", "Campaign")
                .param("discountPercent", "25")
                .param("startDate", "2026-01-01T00:00")
                .param("endDate", "2027-01-01T00:00")
                .param("status", "ACTIVE")
        );
    }

    @Test
    void productImageUploadCanBeViewedReplacedAndRetained() throws Exception {
        byte[] png = new byte[] {
            (byte) 0x89,
            'P',
            'N',
            'G',
            13,
            10,
            26,
            10,
            1,
            2,
            3,
        };
        MockMultipartFile first = new MockMultipartFile(
            "imageFile",
            "first.png",
            "image/png",
            png
        );
        mvc.perform(
            admin(
                multipart("/admin/products")
                    .file(first)
                    .with(csrf())
                    .param("productName", "Image product")
                    .param("categoryId", category.getCategoryId().toString())
                    .param("price", "300000")
                    .param("status", "ACTIVE")
            )
        ).andExpect(redirectedUrl("/admin/products"));
        var created = products
            .findAll()
            .stream()
            .filter(p -> p.getProductName().equals("Image product"))
            .findFirst()
            .orElseThrow();
        String firstUrl = created.getImgUrl();
        assertThat(firstUrl).startsWith("/uploads/products/");
        mvc.perform(get(firstUrl))
            .andExpect(status().isOk())
            .andExpect(content().bytes(png));
        mvc.perform(admin(get("/admin/products")))
            .andExpect(status().isOk())
            .andExpect(
                content().string(containsString("src=\"" + firstUrl + "\""))
            );
        mvc.perform(
            admin(get("/admin/products/" + created.getProductId() + "/edit"))
        )
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(firstUrl)));

        String editUrl = "/admin/products/" + created.getProductId();
        mvc.perform(
            admin(
                multipart(editUrl)
                    .with(csrf())
                    .param("productName", "Image product")
                    .param("categoryId", category.getCategoryId().toString())
                    .param("price", "300000")
                    .param("status", "ACTIVE")
            )
        ).andExpect(redirectedUrl("/admin/products"));
        assertThat(
            products.findById(created.getProductId()).orElseThrow().getImgUrl()
        ).isEqualTo(firstUrl);

        MockMultipartFile replacement = new MockMultipartFile(
            "imageFile",
            "second.png",
            "image/png",
            png
        );
        mvc.perform(
            admin(
                multipart(editUrl)
                    .file(replacement)
                    .with(csrf())
                    .param("productName", "Image product")
                    .param("categoryId", category.getCategoryId().toString())
                    .param("price", "300000")
                    .param("status", "ACTIVE")
            )
        ).andExpect(redirectedUrl("/admin/products"));
        String secondUrl = products
            .findById(created.getProductId())
            .orElseThrow()
            .getImgUrl();
        assertThat(secondUrl).isNotEqualTo(firstUrl);
        mvc.perform(get(secondUrl))
            .andExpect(status().isOk())
            .andExpect(content().bytes(png));
        assertThat(
            Files.exists(
                imageDirectory.resolve(
                    firstUrl.substring("/uploads/products/".length())
                )
            )
        ).isFalse();
    }

    @Test
    void invalidProductImageShowsFormWithoutCreatingProduct() throws Exception {
        MockMultipartFile invalid = new MockMultipartFile(
            "imageFile",
            "fake.png",
            "image/png",
            new byte[] { 1, 2, 3 }
        );
        mvc.perform(
            admin(
                multipart("/admin/products")
                    .file(invalid)
                    .with(csrf())
                    .param("productName", "Invalid image product")
                    .param("categoryId", category.getCategoryId().toString())
                    .param("price", "300000")
                    .param("status", "ACTIVE")
            )
        )
            .andExpect(status().isOk())
            .andExpect(model().attributeHasErrors("form"));
        assertThat(products.findAll()).noneMatch(p ->
            p.getProductName().equals("Invalid image product")
        );
    }

    @Test
    void loginPageIsPublicWithCsrfAndRealForm() throws Exception {
        mvc.perform(get("/login"))
            .andExpect(status().isOk())
            .andExpect(view().name("auth/login"))
            .andExpect(content().string(containsString("name=\"username\"")))
            .andExpect(content().string(containsString("name=\"password\"")))
            .andExpect(content().string(containsString("name=\"_csrf\"")));
        mvc.perform(get("/login?error")).andExpect(
            content().string(containsString("Không thể đăng nhập"))
        );
        mvc.perform(get("/login?logout")).andExpect(
            content().string(containsString("Bạn đã đăng xuất"))
        );
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "/assets/css/main.css",
            "/assets/css/admin.css",
            "/assets/js/admin.js",
            "/assets/libs/bootstrap/css/bootstrap.min.css",
            "/assets/libs/bootstrap/js/bootstrap.bundle.min.js",
            "/assets/libs/bootstrap-icons/bootstrap-icons.css",
            "/assets/libs/bootstrap-icons/fonts/bootstrap-icons.woff2",
            "/assets/images/favicon.ico",
            "/assets/js/receipt-item-picker.js",
            "/assets/js/inventory-variant-filter.js",
        }
    )
    void assetsArePublic(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isOk());
    }

    @Test
    void anonymousRedirectsToLogin() throws Exception {
        mvc.perform(get("/admin"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login"));
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "/admin",
            "/admin/products",
            "/admin/inventory",
            "/admin/purchase-receipts",
            "/admin/orders",
            "/admin/customers",
        }
    )
    void customerCannotUseWeb(String path) throws Exception {
        mvc.perform(
            get(path).with(user(new StoreUserDetails(accounts.get("CUSTOMER"))))
        ).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = { "ADMIN", "STAFF" })
    void actualFormLoginCreatesSessionAndRedirectsDashboard(String role)
        throws Exception {
        var result = mvc
            .perform(
                formLogin()
                    .user(accounts.get(role).getUsername())
                    .password(PASSWORD)
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin"))
            .andReturn();
        var session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();
        mvc.perform(get("/admin").session(session))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/dashboard"));
        mvc.perform(get("/api/auth/me").session(session)).andExpect(
            status().isUnauthorized()
        );
    }

    @Test
    void invalidPasswordFails() throws Exception {
        mvc.perform(
            formLogin().user("web-ADMIN").password("incorrect")
        ).andExpect(redirectedUrl("/login?error"));
    }

    @ParameterizedTest
    @EnumSource(value = UserStatus.class, names = { "INACTIVE", "LOCKED" })
    void disabledAccountCannotLogin(UserStatus state) throws Exception {
        tx.executeWithoutResult(s -> {
            var u = users
                .findById(accounts.get("ADMIN").getUserId())
                .orElseThrow();
            u.setStatus(state);
            users.saveAndFlush(u);
        });
        mvc.perform(formLogin().user("web-ADMIN").password(PASSWORD)).andExpect(
            redirectedUrl("/login?error")
        );
    }

    @Test
    void logoutRequiresCsrfAndInvalidatesSession() throws Exception {
        var result = mvc
            .perform(formLogin().user("web-STAFF").password(PASSWORD))
            .andReturn();
        var session = (MockHttpSession) result.getRequest().getSession(false);
        mvc.perform(post("/logout").session(session)).andExpect(
            status().isForbidden()
        );
        mvc.perform(post("/logout").session(session).with(csrf())).andExpect(
            redirectedUrl("/login?logout")
        );
        assertThat(session.isInvalid()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "/admin/vouchers",
            "/admin/promotions",
            "/admin/vouchers/new",
            "/admin/promotions/new",
        }
    )
    void staffCannotReadMarketing(String path) throws Exception {
        mvc.perform(staff(get(path))).andExpect(status().isForbidden());
    }

    @Test
    void staffCannotWriteMarketingEvenWithCsrf() throws Exception {
        mvc.perform(staff(post("/admin/vouchers").with(csrf()))).andExpect(
            status().isForbidden()
        );
        mvc.perform(
            staff(
                post(promotionPath() + "/products")
                    .with(csrf())
                    .param("productId", product.getProductId().toString())
            )
        ).andExpect(status().isForbidden());
    }

    @Test
    void staffSidebarHidesMarketingAndHighlightsProducts() throws Exception {
        mvc.perform(staff(get("/admin/products")))
            .andExpect(status().isOk())
            .andExpect(
                content().string(
                    org.hamcrest.Matchers.not(
                        containsString("href=\"/admin/vouchers\"")
                    )
                )
            )
            .andExpect(
                content().string(containsString("sidebar-menu-link active"))
            );
    }

    @ParameterizedTest
    @ValueSource(
        strings = {
            "/admin",
            "/admin/dashboard",
            "/admin/products",
            "/admin/products/new",
            "/admin/sizes",
            "/admin/sizes/new",
            "/admin/colors",
            "/admin/colors/new",
            "/admin/suppliers",
            "/admin/suppliers/new",
            "/admin/inventory",
            "/admin/inventory?threshold=5",
            "/admin/inventory/transactions",
            "/admin/purchase-receipts",
            "/admin/purchase-receipts/new",
            "/admin/orders",
            "/admin/customers",
            "/admin/vouchers",
            "/admin/vouchers/new",
            "/admin/promotions",
            "/admin/promotions/new",
        }
    )
    void adminPagesRenderWithoutOpenInView(String path) throws Exception {
        var result = mvc
            .perform(admin(get(path)))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("text/html"))
            .andReturn();
        var forms = java.util.regex.Pattern.compile(
            "<form\\b[^>]*method=\"post\"[^>]*>(.*?)</form>",
            java.util.regex.Pattern.DOTALL
        ).matcher(result.getResponse().getContentAsString());
        while (forms.find())
            assertThat(forms.group(1)).contains("name=\"_csrf\"");
    }

    @Test
    void editAndVariantAndAssociationPagesRender() throws Exception {
        for (String path : List.of(
            productPath() + "/edit",
            productPath() + "/variants",
            productPath() + "/variants/new",
            productPath() + "/variants/" + variant.getVariantId() + "/edit",
            "/admin/sizes/" + variant.getSize().getSizeId() + "/edit",
            "/admin/colors/" + variant.getColor().getColorId() + "/edit",
            "/admin/suppliers/" + supplier.getSupplierId() + "/edit",
            voucherPath() + "/edit",
            promotionPath() + "/edit",
            promotionPath() + "/products"
        )) {
            var result = mvc
                .perform(admin(get(path)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn();
            var forms = java.util.regex.Pattern.compile(
                "<form\\b[^>]*method=\"post\"[^>]*>(.*?)</form>",
                java.util.regex.Pattern.DOTALL
            ).matcher(result.getResponse().getContentAsString());
            while (forms.find())
                assertThat(forms.group(1)).contains("name=\"_csrf\"");
        }
    }

    @Test
    void adminListFiltersRenderAndQueryServerSide() throws Exception {
        mvc.perform(
            admin(
                get("/admin/purchase-receipts")
                    .param("keyword", "Web")
                    .param("status", "DRAFT")
            )
        )
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Tìm phiếu nhập")));
        mvc.perform(
            admin(
                get("/admin/suppliers")
                    .param("keyword", "Web")
                    .param("status", "ACTIVE")
            )
        ).andExpect(status().isOk());
        mvc.perform(
            admin(
                get("/admin/customers")
                    .param("keyword", "Web")
                    .param("status", "ACTIVE")
            )
        ).andExpect(status().isOk());
        mvc.perform(
            admin(
                get("/admin/vouchers")
                    .param("keyword", "WEB")
                    .param("discountType", "PERCENT")
            )
        ).andExpect(status().isOk());
        mvc.perform(
            admin(
                get("/admin/promotions")
                    .param("keyword", "Web")
                    .param("status", "ACTIVE")
            )
        ).andExpect(status().isOk());
        mvc.perform(
            admin(get("/admin/orders").param("keyword", "Web"))
        ).andExpect(status().isOk());
        mvc.perform(
            admin(
                get("/admin/inventory/transactions").param("keyword", "WEB-SKU")
            )
        )
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("data-product-select")))
            .andExpect(content().string(containsString("Web shirt")))
            .andExpect(content().string(containsString("data-size-select")))
            .andExpect(content().string(containsString("data-color-select")))
            .andExpect(
                content().string(
                    containsString(
                        "data-variant-id=\"" + variant.getVariantId() + "\""
                    )
                )
            );
        mvc.perform(
            admin(
                get(productPath() + "/variants")
                    .param("keyword", "WEB-SKU")
                    .param("status", "INACTIVE")
                    .param(
                        "colorId",
                        variant.getColor().getColorId().toString()
                    )
            )
        )
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("WEB-SKU")));
    }

    @Test
    void productSearchStatusAndPaginationKeepFilters() throws Exception {
        mvc.perform(
            staff(
                get("/admin/products")
                    .param("keyword", "Web")
                    .param("status", "INACTIVE")
                    .param("size", "1")
            )
        )
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Web shirt")))
            .andExpect(content().string(containsString("INACTIVE")))
            .andExpect(content().string(containsString("Tồn hàng")))
            .andExpect(
                model().attribute(
                    "stockTotals",
                    org.hamcrest.Matchers.hasEntry(product.getProductId(), 5L)
                )
            );
        tx.executeWithoutResult(s -> {
            var p = new Product();
            p.setCategory(category);
            p.setProductName("Web second");
            p.setPrice(BigDecimal.TEN);
            p.setStatus(ActiveStatus.INACTIVE);
            products.saveAndFlush(p);
        });
        var result = mvc
            .perform(
                admin(
                    get("/admin/products")
                        .param("keyword", "Web")
                        .param("status", "INACTIVE")
                        .param("size", "1")
                )
            )
            .andExpect(status().isOk())
            .andReturn();
        assertThat(result.getResponse().getContentAsString()).contains(
            "keyword=Web",
            "status=INACTIVE",
            "size=1",
            "page=1"
        );
        mvc.perform(admin(get("/admin/products").param("keyword", "missing")))
            .andExpect(status().isOk())
            .andExpect(
                content().string(containsString("Chưa có dữ liệu phù hợp"))
            );
    }

    @Test
    void missingCsrfCannotCreateProduct() throws Exception {
        long count = products.count();
        mvc.perform(
            admin(post("/admin/products").param("productName", "Blocked"))
        ).andExpect(status().isForbidden());
        assertThat(products.count()).isEqualTo(count);
    }

    @Test
    void validProductCreateEditAndStatusReuseServices() throws Exception {
        mvc.perform(productForm("/admin/products", "New shirt"))
            .andExpect(redirectedUrl("/admin/products"))
            .andExpect(flash().attributeExists("successMessage"));
        assertThat(products.count()).isEqualTo(2);
        mvc.perform(productForm(productPath(), "Updated shirt")).andExpect(
            redirectedUrl("/admin/products")
        );
        mvc.perform(
            staff(
                post(productPath() + "/status")
                    .with(csrf())
                    .param("status", "INACTIVE")
            )
        ).andExpect(redirectedUrl("/admin/products"));
        assertThat(
            products
                .findById(product.getProductId())
                .orElseThrow()
                .getProductName()
        ).isEqualTo("Updated shirt");
        assertThat(
            products.findById(product.getProductId()).orElseThrow().getStatus()
        ).isEqualTo(ActiveStatus.INACTIVE);
    }

    @Test
    void invalidProductRendersFieldErrorsAndRetainsInput() throws Exception {
        long count = products.count();
        mvc.perform(productForm("/admin/products", "", "-10"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/products/form"))
            .andExpect(
                model().attributeHasFieldErrors("form", "productName", "price")
            )
            .andExpect(content().string(containsString("Web description")));
        assertThat(products.count()).isEqualTo(count);
    }

    @Test
    void invalidNumberBindingRendersForm() throws Exception {
        mvc.perform(productForm(productPath(), "Kept", "not-a-number"))
            .andExpect(status().isOk())
            .andExpect(model().attributeHasFieldErrors("form", "price"));
    }

    @Test
    void variantMetadataCannotChangeQuantity() throws Exception {
        var result = mvc
            .perform(
                staff(
                    get(
                        productPath() +
                            "/variants/" +
                            variant.getVariantId() +
                            "/edit"
                    )
                )
            )
            .andExpect(status().isOk())
            .andReturn();
        assertThat(result.getResponse().getContentAsString())
            .contains("Số lượng: 5")
            .doesNotContain("name=\"quantity\"", "name=\"sku\"");
        mvc.perform(
            staff(
                post(productPath() + "/variants/" + variant.getVariantId())
                    .with(csrf())
                    .param("sku", "NEW-WEB-SKU")
                    .param("sizeId", variant.getSize().getSizeId().toString())
                    .param(
                        "colorId",
                        variant.getColor().getColorId().toString()
                    )
                    .param("price", "350000")
                    .param("status", "ACTIVE")
                    .param("quantity", "999")
            )
        ).andExpect(redirectedUrl(productPath() + "/variants"));
        var updated = variants.findById(variant.getVariantId()).orElseThrow();
        assertThat(updated.getQuantity()).isEqualTo(5);
        assertThat(updated.getSku()).isEqualTo("WEB-SKU");
    }

    @Test
    void bulkVariantCreateStartsWithZeroStockAndDuplicateRendersForm()
        throws Exception {
        var large = tx.execute(s -> {
            var value = new Size();
            value.setSizeName("L");
            return sizes.saveAndFlush(value);
        });
        var request = staff(
            post(productPath() + "/variants")
                .with(csrf())
                .param("sizeIds", large.getSizeId().toString())
                .param("colorIds", variant.getColor().getColorId().toString())
                .param("price", "100")
                .param("status", "ACTIVE")
                .param("quantity", "900")
        );
        var newForm = mvc
            .perform(staff(get(productPath() + "/variants/new")))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/products/variant-bulk-form"))
            .andExpect(
                content().string(containsString("data-select-all=\"sizeIds\""))
            )
            .andExpect(
                content().string(containsString("data-clear-all=\"colorIds\""))
            )
            .andReturn();
        var defaults = (BulkProductVariantRequest) newForm
            .getModelAndView()
            .getModel()
            .get("form");
        assertThat(defaults.price()).isEqualByComparingTo(product.getPrice());

        mvc.perform(request).andExpect(
            redirectedUrl(productPath() + "/variants")
        );
        var created = variants
            .findAll()
            .stream()
            .filter(value ->
                value.getSize().getSizeId().equals(large.getSizeId())
            )
            .findFirst()
            .orElseThrow();
        assertThat(created.getQuantity()).isZero();
        assertThat(created.getSku()).isEqualTo(
            "SP-" +
                product.getProductId() +
                "-S" +
                large.getSizeId() +
                "-C" +
                variant.getColor().getColorId()
        );

        mvc.perform(request)
            .andExpect(status().isOk())
            .andExpect(view().name("admin/products/variant-bulk-form"))
            .andExpect(model().attributeHasErrors("form"));
    }

    @Test
    void bulkVariantCreateBuildsCartesianProductAndSkipsExistingCombination()
        throws Exception {
        var large = tx.execute(s -> {
            var value = new Size();
            value.setSizeName("L");
            return sizes.saveAndFlush(value);
        });
        var blue = tx.execute(s -> {
            var value = new Color();
            value.setColorName("Blue");
            return colors.saveAndFlush(value);
        });

        mvc.perform(
            staff(
                post(productPath() + "/variants")
                    .with(csrf())
                    .param(
                        "sizeIds",
                        variant.getSize().getSizeId().toString(),
                        large.getSizeId().toString()
                    )
                    .param(
                        "colorIds",
                        variant.getColor().getColorId().toString(),
                        blue.getColorId().toString()
                    )
                    .param("price", "275000")
                    .param("status", "ACTIVE")
            )
        )
            .andExpect(redirectedUrl(productPath() + "/variants"))
            .andExpect(
                flash().attribute(
                    "successMessage",
                    containsString("Đã tạo 3 thuộc tính")
                )
            )
            .andExpect(
                flash().attribute("successMessage", containsString("Bỏ qua 1"))
            );

        var all = variants.findAll();
        assertThat(all).hasSize(4);
        assertThat(all)
            .filteredOn(value ->
                value.getProduct().getProductId().equals(product.getProductId())
            )
            .allMatch(value -> value.getQuantity() >= 0);
        assertThat(all)
            .filteredOn(
                value -> !value.getVariantId().equals(variant.getVariantId())
            )
            .allMatch(
                value ->
                    value.getPrice().compareTo(new BigDecimal("275000")) == 0
            )
            .allMatch(value -> value.getStatus() == ActiveStatus.ACTIVE)
            .allMatch(value -> value.getQuantity() == 0);
    }

    @Test
    void variantCannotBeEditedUnderDifferentProduct() throws Exception {
        mvc.perform(
            admin(
                get(
                    "/admin/products/999999/variants/" +
                        variant.getVariantId() +
                        "/edit"
                )
            )
        )
            .andExpect(status().isNotFound())
            .andExpect(view().name("error/404"));
    }

    @Test
    void inventoryShowsReadOnlyQuantityAndInactiveVariant() throws Exception {
        mvc.perform(staff(get("/admin/inventory")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("WEB-SKU")))
            .andExpect(
                content().string(
                    org.hamcrest.Matchers.not(
                        containsString("name=\"quantity\"")
                    )
                )
            );
    }

    @Test
    void receiptCreateAddUpdateRemoveCompleteAndRepeatAreSafe()
        throws Exception {
        var result = mvc
            .perform(
                staff(
                    post("/admin/purchase-receipts")
                        .with(csrf())
                        .param(
                            "supplierId",
                            supplier.getSupplierId().toString()
                        )
                )
            )
            .andExpect(status().is3xxRedirection())
            .andReturn();
        String path = result.getResponse().getRedirectedUrl();
        long id = Long.parseLong(path.substring(path.lastIndexOf('/') + 1));
        assertThat(receipts.findById(id).orElseThrow().getStatus()).isEqualTo(
            PurchaseReceiptStatus.DRAFT
        );
        tx.executeWithoutResult(status -> {
            var selected = variants
                .findById(variant.getVariantId())
                .orElseThrow();
            selected.setPrice(new BigDecimal("275000"));
            variants.saveAndFlush(selected);
        });
        mvc.perform(staff(get(path)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("data-size-select")))
            .andExpect(content().string(containsString("data-color-select")))
            .andExpect(content().string(containsString("data-variant-id")))
            .andExpect(
                content().string(
                    containsString("data-base-price=\"250000.00\"")
                )
            )
            .andExpect(
                content().string(
                    org.hamcrest.Matchers.not(
                        containsString("data-variant-select")
                    )
                )
            )
            .andExpect(
                content().string(
                    org.hamcrest.Matchers.not(
                        containsString("Kích cỡ, màu sắc và SKU")
                    )
                )
            );
        mvc.perform(
            staff(
                post(path + "/items")
                    .with(csrf())
                    .param("variantId", variant.getVariantId().toString())
                    .param("quantity", "3")
                    .param("unitPrice", "100000")
            )
        ).andExpect(redirectedUrl(path));
        mvc.perform(
            staff(
                post(path + "/items/" + variant.getVariantId())
                    .with(csrf())
                    .param("quantity", "4")
                    .param("unitPrice", "100000")
            )
        ).andExpect(redirectedUrl(path));
        assertThat(
            variants
                .findById(variant.getVariantId())
                .orElseThrow()
                .getQuantity()
        ).isEqualTo(5);
        mvc.perform(staff(get("/admin/purchase-receipts")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Web shirt")))
            .andExpect(content().string(containsString("WEB-SKU")))
            .andExpect(content().string(containsString("Kích cỡ: M")))
            .andExpect(content().string(containsString("Màu: Green")));
        mvc.perform(staff(post(path + "/complete").with(csrf())))
            .andExpect(redirectedUrl(path))
            .andExpect(flash().attributeExists("successMessage"));
        assertThat(
            variants
                .findById(variant.getVariantId())
                .orElseThrow()
                .getQuantity()
        ).isEqualTo(9);
        mvc.perform(staff(post(path + "/complete").with(csrf())))
            .andExpect(redirectedUrl(path))
            .andExpect(flash().attributeExists("errorMessage"));
        assertThat(
            variants
                .findById(variant.getVariantId())
                .orElseThrow()
                .getQuantity()
        ).isEqualTo(9);
        assertThat(history.count()).isEqualTo(1);
        var html = mvc
            .perform(staff(get(path)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        assertThat(html).doesNotContain(
            "Complete receipt",
            "name=\"quantity\""
        );
        mvc.perform(staff(get("/admin/inventory/transactions")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("IMPORT")));
    }

    @Test
    void draftItemRemoveAndCancelNeverChangeStock() throws Exception {
        String path = receipt();
        receiptService.addItem(
            Long.parseLong(path.substring(path.lastIndexOf('/') + 1)),
            new PurchaseReceiptDetailRequest(
                variant.getVariantId(),
                3,
                new BigDecimal("100")
            )
        );
        mvc.perform(
            staff(
                post(
                    path + "/items/" + variant.getVariantId() + "/remove"
                ).with(csrf())
            )
        ).andExpect(redirectedUrl(path));
        mvc.perform(staff(post(path + "/cancel").with(csrf()))).andExpect(
            redirectedUrl(path)
        );
        assertThat(
            variants
                .findById(variant.getVariantId())
                .orElseThrow()
                .getQuantity()
        ).isEqualTo(5);
        assertThat(history.count()).isZero();
        mvc.perform(staff(get(path))).andExpect(status().isOk());
    }

    @Test
    void invalidReceiptFormsRetainErrors() throws Exception {
        mvc.perform(staff(post("/admin/purchase-receipts").with(csrf())))
            .andExpect(status().isOk())
            .andExpect(model().attributeHasFieldErrors("form", "supplierId"));
        String path = receipt();
        mvc.perform(
            staff(
                post(path + "/items")
                    .with(csrf())
                    .param("quantity", "-1")
                    .param("unitPrice", "0")
            )
        )
            .andExpect(status().isOk())
            .andExpect(view().name("admin/purchase-receipts/detail"))
            .andExpect(model().attributeHasErrors("form"));
        mvc.perform(staff(post(path + "/complete").with(csrf()))).andExpect(
            flash().attributeExists("errorMessage")
        );
    }

    @Test
    void ordersRenderSnapshotsAndOnlyAllowedTransitions() throws Exception {
        Long id = checkout(true);
        mvc.perform(staff(get("/admin/orders").param("status", "PENDING")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Web customer")));
        String html = mvc
            .perform(staff(get("/admin/orders/" + id)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        assertThat(html)
            .contains(
                "WEB10",
                "12 Web Street",
                "value=\"CONFIRMED\"",
                "value=\"CANCELLED\""
            )
            .doesNotContain("value=\"DELIVERED\"");
    }

    @Test
    void validAndInvalidOrderTransitionsDisplayFlash() throws Exception {
        Long id = checkout(false);
        String path = "/admin/orders/" + id;
        mvc.perform(
            staff(
                post(path + "/status")
                    .with(csrf())
                    .param("status", "CONFIRMED")
            )
        )
            .andExpect(redirectedUrl(path))
            .andExpect(flash().attributeExists("successMessage"));
        mvc.perform(
            staff(
                post(path + "/status")
                    .with(csrf())
                    .param("status", "DELIVERED")
            )
        )
            .andExpect(redirectedUrl(path))
            .andExpect(flash().attributeExists("errorMessage"));
        assertThat(orders.findById(id).orElseThrow().getStatus()).isEqualTo(
            OrderStatus.CONFIRMED
        );
    }

    @Test
    void cancelOrderRestoresStockAndVoucherExactlyOnce() throws Exception {
        Long id = checkout(true);
        String path = "/admin/orders/" + id;
        assertThat(
            variants
                .findById(variant.getVariantId())
                .orElseThrow()
                .getQuantity()
        ).isEqualTo(3);
        assertThat(
            vouchers
                .findById(voucher.getVoucherId())
                .orElseThrow()
                .getQuantity()
        ).isEqualTo(1);
        mvc.perform(
            staff(
                post(path + "/status")
                    .with(csrf())
                    .param("status", "CANCELLED")
            )
        ).andExpect(redirectedUrl(path));
        mvc.perform(
            staff(
                post(path + "/status")
                    .with(csrf())
                    .param("status", "CANCELLED")
            )
        ).andExpect(flash().attributeExists("errorMessage"));
        assertThat(
            variants
                .findById(variant.getVariantId())
                .orElseThrow()
                .getQuantity()
        ).isEqualTo(5);
        assertThat(
            vouchers
                .findById(voucher.getVoucherId())
                .orElseThrow()
                .getQuantity()
        ).isEqualTo(2);
        assertThat(history.count()).isEqualTo(2);
    }

    @Test
    void voucherCreateEditAndStatusWork() throws Exception {
        mvc.perform(voucherForm("/admin/vouchers", "NEWCODE")).andExpect(
            redirectedUrl("/admin/vouchers")
        );
        mvc.perform(voucherForm(voucherPath(), "WEB10")).andExpect(
            redirectedUrl("/admin/vouchers")
        );
        mvc.perform(
            admin(
                post(voucherPath() + "/status")
                    .with(csrf())
                    .param("status", "INACTIVE")
            )
        ).andExpect(redirectedUrl("/admin/vouchers"));
        var v = vouchers.findById(voucher.getVoucherId()).orElseThrow();
        assertThat(v.getStatus()).isEqualTo(ActiveStatus.INACTIVE);
        assertThat(v.getQuantity()).isEqualTo(3);
        assertThat(v.getDiscountValue()).isEqualByComparingTo("15");
    }

    @Test
    void duplicateVoucherAndInvalidRangeRenderForm() throws Exception {
        mvc.perform(voucherForm("/admin/vouchers", "WEB10"))
            .andExpect(status().isOk())
            .andExpect(model().attributeHasErrors("form"));
        mvc.perform(
            voucherForm(voucherPath(), "WEB10", "150", "2025-01-01T00:00")
        )
            .andExpect(status().isOk())
            .andExpect(model().attributeHasErrors("form"));
    }

    @Test
    void promotionCreateEditStatusAndProductAssociationsWork()
        throws Exception {
        mvc.perform(
            promotionForm("/admin/promotions", "New campaign")
        ).andExpect(redirectedUrl("/admin/promotions"));
        mvc.perform(
            promotionForm(promotionPath(), "Updated campaign")
        ).andExpect(redirectedUrl("/admin/promotions"));
        mvc.perform(
            admin(
                post(promotionPath() + "/products")
                    .with(csrf())
                    .param("productId", product.getProductId().toString())
            )
        ).andExpect(redirectedUrl(promotionPath() + "/products"));
        mvc.perform(admin(get(promotionPath() + "/products")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Web shirt")))
            .andExpect(content().string(containsString("187.500")))
            .andExpect(model().attribute("promotionActive", true));
        mvc.perform(
            admin(
                post(promotionPath() + "/products")
                    .with(csrf())
                    .param("productId", product.getProductId().toString())
            )
        ).andExpect(flash().attributeExists("errorMessage"));
        mvc.perform(
            admin(
                post(
                    promotionPath() +
                        "/products/" +
                        product.getProductId() +
                        "/remove"
                ).with(csrf())
            )
        ).andExpect(redirectedUrl(promotionPath() + "/products"));
        assertThat(associations.count()).isZero();
        assertThat(products.existsById(product.getProductId())).isTrue();
        mvc.perform(
            admin(
                post(promotionPath() + "/status")
                    .with(csrf())
                    .param("status", "INACTIVE")
            )
        ).andExpect(redirectedUrl("/admin/promotions"));
        assertThat(
            promotions
                .findById(promotion.getPromotionId())
                .orElseThrow()
                .getStatus()
        ).isEqualTo(ActiveStatus.INACTIVE);
    }

    @Test
    void promotionVariantScopeCanSelectSizeColorAndReturnToAllVariants()
        throws Exception {
        mvc.perform(
            admin(
                post(promotionPath() + "/products")
                    .with(csrf())
                    .param("productId", product.getProductId().toString())
            )
        ).andExpect(redirectedUrl(promotionPath() + "/products"));

        String scopePath =
            promotionPath() +
            "/products/" +
            product.getProductId() +
            "/variants";
        mvc.perform(admin(get(scopePath)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Size M")))
            .andExpect(content().string(containsString("Green")))
            .andExpect(content().string(containsString("data-variant-picker")))
            .andExpect(content().string(containsString("data-size-choice")))
            .andExpect(content().string(containsString("data-color-choice")))
            .andExpect(content().string(containsString("name=\"variantIds\"")));

        mvc.perform(
            admin(
                post(scopePath)
                    .with(csrf())
                    .param("scope", "SELECTED")
                    .param("variantIds", variant.getVariantId().toString())
            )
        ).andExpect(redirectedUrl(scopePath));
        assertThat(
            variantPromotions.findSelectedVariantIds(
                promotion.getPromotionId(),
                product.getProductId()
            )
        ).containsExactly(variant.getVariantId());

        mvc.perform(
            admin(post(scopePath).with(csrf()).param("scope", "ALL"))
        ).andExpect(redirectedUrl(scopePath));
        assertThat(
            variantPromotions.findSelectedVariantIds(
                promotion.getPromotionId(),
                product.getProductId()
            )
        ).isEmpty();
    }

    @Test
    void promotionProductPickerFiltersCategoryAndAddsMultipleProducts()
        throws Exception {
        Long secondId = tx.execute(status -> {
            var second = new Product();
            second.setProductName("Second web shirt");
            second.setCategory(
                categories.findById(category.getCategoryId()).orElseThrow()
            );
            second.setPrice(new BigDecimal("310000"));
            second.setStatus(ActiveStatus.ACTIVE);
            return products.saveAndFlush(second).getProductId();
        });
        mvc.perform(
            admin(
                get(promotionPath() + "/products").param(
                    "categoryId",
                    category.getCategoryId().toString()
                )
            )
        )
            .andExpect(status().isOk())
            .andExpect(
                content().string(containsString("data-product-selection"))
            )
            .andExpect(content().string(containsString("data-select-all")))
            .andExpect(content().string(containsString("Web shirt")))
            .andExpect(content().string(containsString("Second web shirt")))
            .andExpect(
                model().attribute("categoryId", category.getCategoryId())
            );
        mvc.perform(
            admin(
                post(promotionPath() + "/products")
                    .with(csrf())
                    .param(
                        "productIds",
                        product.getProductId().toString(),
                        secondId.toString()
                    )
                    .param("categoryId", category.getCategoryId().toString())
            )
        ).andExpect(
            redirectedUrl(
                promotionPath() +
                    "/products?categoryId=" +
                    category.getCategoryId()
            )
        );
        assertThat(
            associations.findLinkedProductIds(
                promotion.getPromotionId(),
                List.of(product.getProductId(), secondId)
            )
        ).containsExactlyInAnyOrder(product.getProductId(), secondId);
    }

    @Test
    void customerListHandlesUnlinkedAccount() throws Exception {
        mvc.perform(staff(get("/admin/customers")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Walk-in customer")))
            .andExpect(
                content().string(containsString("Chưa liên kết tài khoản"))
            );
    }

    @Test
    void sizeColorSupplierFormsReuseServices() throws Exception {
        mvc.perform(
            staff(post("/admin/sizes").with(csrf()).param("sizeName", "XL"))
        ).andExpect(redirectedUrl("/admin/sizes"));
        mvc.perform(
            staff(post("/admin/colors").with(csrf()).param("colorName", "Blue"))
        ).andExpect(redirectedUrl("/admin/colors"));
        mvc.perform(
            staff(
                post("/admin/sizes/" + variant.getSize().getSizeId())
                    .with(csrf())
                    .param("sizeName", "Medium")
            )
        ).andExpect(redirectedUrl("/admin/sizes"));
        mvc.perform(
            staff(
                post("/admin/colors/" + variant.getColor().getColorId())
                    .with(csrf())
                    .param("colorName", "Forest")
            )
        ).andExpect(redirectedUrl("/admin/colors"));
        mvc.perform(
            staff(
                post("/admin/suppliers")
                    .with(csrf())
                    .param("supplierName", "New supplier")
                    .param("status", "ACTIVE")
            )
        ).andExpect(redirectedUrl("/admin/suppliers"));
        mvc.perform(
            staff(
                post("/admin/suppliers/" + supplier.getSupplierId())
                    .with(csrf())
                    .param("supplierName", "Updated supplier")
                    .param("status", "ACTIVE")
            )
        ).andExpect(redirectedUrl("/admin/suppliers"));
        mvc.perform(
            staff(
                post("/admin/suppliers/" + supplier.getSupplierId() + "/status")
                    .with(csrf())
                    .param("status", "INACTIVE")
            )
        ).andExpect(redirectedUrl("/admin/suppliers"));
        assertThat(
            suppliers
                .findById(supplier.getSupplierId())
                .orElseThrow()
                .getStatus()
        ).isEqualTo(ActiveStatus.INACTIVE);
    }

    @Test
    void errorsAreHtmlAndNotRestJson() throws Exception {
        mvc.perform(admin(get("/admin/products/999999/edit")))
            .andExpect(status().isNotFound())
            .andExpect(view().name("error/404"))
            .andExpect(content().contentTypeCompatibleWith("text/html"));
        mvc.perform(admin(get("/admin/products").param("page", "-1")))
            .andExpect(status().isBadRequest())
            .andExpect(view().name("error/400"));
        mvc.perform(get("/403"))
            .andExpect(status().isForbidden())
            .andExpect(view().name("error/403"));
        mvc.perform(get("/api/products/999999"))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith("application/json"));
    }

    @Test
    void dashboardUsesActualCounts() throws Exception {
        Long first = checkout(false);
        Long second = checkout(false);
        BigDecimal expectedRevenue = tx.execute(s -> {
            var one = orders.findById(first).orElseThrow();
            var two = orders.findById(second).orElseThrow();
            one.setStatus(OrderStatus.DELIVERED);
            two.setStatus(OrderStatus.DELIVERED);
            orders.saveAllAndFlush(List.of(one, two));
            return one.getFinalAmount().add(two.getFinalAmount());
        });
        receipt();
        var result = mvc
            .perform(staff(get("/admin")))
            .andExpect(status().isOk())
            .andReturn();
        var summary = (DashboardService.Summary) result
            .getModelAndView()
            .getModel()
            .get("summary");
        assertThat(summary.activeProducts()).isEqualTo(1);
        assertThat(summary.pendingOrders()).isZero();
        assertThat(summary.todayOrders()).isEqualTo(2);
        assertThat(summary.customers()).isEqualTo(2);
        assertThat(summary.draftReceipts()).isEqualTo(1);
        assertThat(summary.lowStock()).isEqualTo(1);
        assertThat(summary.deliveredOrders()).isEqualTo(2);
        assertThat(summary.deliveredRevenue()).isEqualByComparingTo(
            expectedRevenue
        );
        assertThat(result.getResponse().getContentAsString()).contains(
            "Tổng doanh thu đã giao"
        );
    }

    @Test
    void mappingErrorsKeepHtmlForWebAndJsonForApi() throws Exception {
        mvc.perform(admin(post("/admin").with(csrf())))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(content().contentTypeCompatibleWith("text/html"));
        mvc.perform(get("/api/sizes/unknown")).andExpect(
            status().isUnauthorized()
        );
        mvc.perform(admin(get("/admin/unknown")))
            .andExpect(status().isNotFound())
            .andExpect(view().name("error/404"));
        mvc.perform(get("/assets/unknown.css"))
            .andExpect(status().isNotFound())
            .andExpect(content().contentTypeCompatibleWith("text/html"));
    }

    @Test
    void promotionPriceRendersForInactiveCatalogWithoutMutatingBasePrice()
        throws Exception {
        tx.executeWithoutResult(s -> {
            var p = promotions
                .findById(promotion.getPromotionId())
                .orElseThrow();
            p.setStatus(ActiveStatus.ACTIVE);
            promotions.saveAndFlush(p);
            var link = new PromotionProduct();
            link.setId(
                new PromotionProductId(
                    p.getPromotionId(),
                    product.getProductId()
                )
            );
            link.setPromotion(p);
            link.setProduct(
                products.findById(product.getProductId()).orElseThrow()
            );
            associations.saveAndFlush(link);
        });
        mvc.perform(staff(get(productPath() + "/variants")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("200.000")))
            .andExpect(content().string(containsString("250.000")));
        assertThat(
            variants.findById(variant.getVariantId()).orElseThrow().getPrice()
        ).isEqualByComparingTo("250000");
    }

    @Test
    void voucherStatusDoesNotOverwriteConsumedQuantity() throws Exception {
        checkout(true);
        mvc.perform(
            admin(
                post(voucherPath() + "/status")
                    .with(csrf())
                    .param("status", "INACTIVE")
            )
        ).andExpect(redirectedUrl("/admin/vouchers"));
        assertThat(
            vouchers
                .findById(voucher.getVoucherId())
                .orElseThrow()
                .getQuantity()
        ).isEqualTo(1);
    }

    @Test
    void orderViewReadsHistoricalPricesAfterCatalogChanges() throws Exception {
        Long id = checkout(true);
        tx.executeWithoutResult(s -> {
            var v = variants.findById(variant.getVariantId()).orElseThrow();
            v.setPrice(new BigDecimal("999999"));
            variants.saveAndFlush(v);
        });
        mvc.perform(staff(get("/admin/orders/" + id)))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("250.000")))
            .andExpect(
                content().string(
                    org.hamcrest.Matchers.not(containsString("999.999"))
                )
            );
    }

    @Test
    void dateAndStatusFiltersSurviveOrderPagination() throws Exception {
        checkout(false);
        checkout(false);
        var result = mvc
            .perform(
                staff(
                    get("/admin/orders")
                        .param("status", "PENDING")
                        .param("size", "1")
                        .param(
                            "from",
                            LocalDateTime.now()
                                .minusDays(1)
                                .withNano(0)
                                .toString()
                        )
                        .param(
                            "to",
                            LocalDateTime.now()
                                .plusDays(1)
                                .withNano(0)
                                .toString()
                        )
                )
            )
            .andExpect(status().isOk())
            .andReturn();
        String next = (String) result
            .getModelAndView()
            .getModel()
            .get("nextPageUrl");
        assertThat(next).contains(
            "status=PENDING",
            "from=",
            "to=",
            "size=1",
            "page=1"
        );
    }

    @Test
    void paginationEncodesUnicodeAndSpecialCharactersOnce() throws Exception {
        tx.executeWithoutResult(s -> {
            for (String name : List.of(
                "Áo & đẹp + sale",
                "Áo & đẹp + sale 2"
            )) {
                var p = new Product();
                p.setCategory(category);
                p.setProductName(name);
                p.setPrice(BigDecimal.ONE);
                p.setStatus(ActiveStatus.ACTIVE);
                products.saveAndFlush(p);
            }
        });
        String keyword = "Áo & đẹp + sale";
        var result = mvc
            .perform(
                admin(
                    get("/admin/products")
                        .param("keyword", keyword)
                        .param("size", "1")
                )
            )
            .andExpect(status().isOk())
            .andReturn();
        String next = (String) result
            .getModelAndView()
            .getModel()
            .get("nextPageUrl");
        var query =
            org.springframework.web.util.UriComponentsBuilder.fromUriString(
                next
            )
                .build()
                .getQueryParams();
        assertThat(
            java.net.URLDecoder.decode(
                query.getFirst("keyword"),
                java.nio.charset.StandardCharsets.UTF_8
            )
        ).isEqualTo(keyword);
        assertThat(next).contains("%26").doesNotContain("%2526");
    }

    @Test
    void categoryPagesCreateEditStatusAndProductFilterWork() throws Exception {
        mvc.perform(staff(get("/admin/categories")))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/categories/list"))
            .andExpect(content().string(containsString("Clothing")));
        mvc.perform(staff(get("/admin/categories/new")))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/categories/form"));
        mvc.perform(
            staff(
                post("/admin/categories")
                    .with(csrf())
                    .param("categoryName", "Accessories")
                    .param("status", "ACTIVE")
            )
        ).andExpect(redirectedUrl("/admin/categories"));
        var created = categories
            .findByCategoryNameIgnoreCase("Accessories")
            .orElseThrow();
        mvc.perform(
            staff(get("/admin/categories/" + created.getCategoryId() + "/edit"))
        )
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Accessories")));
        mvc.perform(
            staff(
                post("/admin/categories/" + created.getCategoryId())
                    .with(csrf())
                    .param("categoryName", "Phụ kiện")
                    .param("status", "ACTIVE")
            )
        ).andExpect(redirectedUrl("/admin/categories"));
        mvc.perform(
            staff(
                post("/admin/categories/" + created.getCategoryId() + "/status")
                    .with(csrf())
                    .param("status", "INACTIVE")
            )
        ).andExpect(redirectedUrl("/admin/categories"));
        assertThat(
            categories
                .findById(created.getCategoryId())
                .orElseThrow()
                .getStatus()
        ).isEqualTo(ActiveStatus.INACTIVE);
        mvc.perform(staff(get("/admin/products/new")))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Clothing")));
        mvc.perform(
            staff(
                get("/admin/products").param(
                    "categoryId",
                    category.getCategoryId().toString()
                )
            )
        )
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Web shirt")));
    }

    @Test
    void adminCanUploadMultipleImagesSetPrimaryAndDeleteWithoutDeletingProduct()
        throws Exception {
        byte[] png = new byte[] {
            (byte) 0x89,
            'P',
            'N',
            'G',
            13,
            10,
            26,
            10,
            1,
            2,
            3,
        };
        MockMultipartFile one = new MockMultipartFile(
            "imageFiles",
            "one.png",
            "image/png",
            png
        );
        MockMultipartFile two = new MockMultipartFile(
            "imageFiles",
            "two.png",
            "image/png",
            png
        );
        MockMultipartFile three = new MockMultipartFile(
            "imageFiles",
            "three.png",
            "image/png",
            png
        );
        mvc.perform(
            admin(
                multipart(productPath() + "/images")
                    .file(one)
                    .file(two)
                    .file(three)
                    .with(csrf())
            )
        ).andExpect(redirectedUrl(productPath() + "/edit"));
        var gallery =
            productImages.findByProduct_ProductIdOrderByDisplayOrderAscProductImageIdAsc(
                product.getProductId()
            );
        assertThat(gallery).hasSize(3);
        assertThat(gallery).filteredOn(ProductImage::isPrimary).hasSize(1);
        assertThat(
            products.findById(product.getProductId()).orElseThrow().getImgUrl()
        ).isEqualTo(gallery.getFirst().getImageUrl());

        Long secondId = gallery.get(1).getProductImageId();
        mvc.perform(
            admin(
                post(productPath() + "/images/" + secondId + "/primary").with(
                    csrf()
                )
            )
        ).andExpect(redirectedUrl(productPath() + "/edit"));
        gallery =
            productImages.findByProduct_ProductIdOrderByDisplayOrderAscProductImageIdAsc(
                product.getProductId()
            );
        assertThat(gallery)
            .filteredOn(ProductImage::isPrimary)
            .singleElement()
            .extracting(ProductImage::getProductImageId)
            .isEqualTo(secondId);

        mvc.perform(
            admin(
                post(productPath() + "/images/" + secondId + "/delete").with(
                    csrf()
                )
            )
        ).andExpect(redirectedUrl(productPath() + "/edit"));
        gallery =
            productImages.findByProduct_ProductIdOrderByDisplayOrderAscProductImageIdAsc(
                product.getProductId()
            );
        assertThat(gallery)
            .hasSize(2)
            .filteredOn(ProductImage::isPrimary)
            .hasSize(1);
        assertThat(products.existsById(product.getProductId())).isTrue();
    }

    @Test
    void adminCanOptionallyAssignAnUploadedImageToAColor() throws Exception {
        byte[] png = new byte[] {
            (byte) 0x89,
            'P',
            'N',
            'G',
            13,
            10,
            26,
            10,
            1,
            2,
            3,
        };
        Color green = variant.getColor();
        MockMultipartFile shared = new MockMultipartFile(
            "imageFiles",
            "shared.png",
            "image/png",
            png
        );
        MockMultipartFile image = new MockMultipartFile(
            "imageFiles",
            "green.png",
            "image/png",
            png
        );

        mvc.perform(
            admin(
                multipart(productPath() + "/images")
                    .file(shared)
                    .file(image)
                    .param("imageColorIds", "", green.getColorId().toString())
                    .with(csrf())
            )
        ).andExpect(redirectedUrl(productPath() + "/edit"));

        var gallery =
            productImages.findByProduct_ProductIdOrderByDisplayOrderAscProductImageIdAsc(
                product.getProductId()
            );
        assertThat(gallery.getFirst().getColor()).isNull();
        var saved = gallery.get(1);
        assertThat(saved.getColor()).isNotNull();
        assertThat(saved.getColor().getColorId()).isEqualTo(green.getColorId());
        assertThat(saved.getColor().getColorName()).isEqualTo("Green");
    }

    @Test
    void galleryRejectsMoreThanEightImages() throws Exception {
        byte[] png = new byte[] {
            (byte) 0x89,
            'P',
            'N',
            'G',
            13,
            10,
            26,
            10,
            1,
        };
        var request = multipart(productPath() + "/images").with(csrf());
        for (int i = 0; i < 9; i++) request.file(
            new MockMultipartFile("imageFiles", i + ".png", "image/png", png)
        );
        mvc.perform(admin(request))
            .andExpect(redirectedUrl(productPath() + "/edit"))
            .andExpect(flash().attributeExists("errorMessage"));
        assertThat(
            productImages.countByProduct_ProductId(product.getProductId())
        ).isZero();
    }
}
