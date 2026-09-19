package com.lauriel.store.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.lauriel.store.dto.response.ProductDetailResponse;
import com.lauriel.store.entity.*;
import com.lauriel.store.entity.enums.ActiveStatus;
import com.lauriel.store.entity.enums.UserStatus;
import com.lauriel.store.repository.*;
import com.lauriel.store.security.JwtService;
import com.lauriel.store.security.StoreUserDetails;
import com.lauriel.store.service.ProductService;
import com.lauriel.store.support.TestSchema;
import com.lauriel.store.support.TestSecrets;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional // Each test rolls back fixtures in its own H2 memory database.
class CatalogApiTest {
    private static final String TEST_HASH = new BCryptPasswordEncoder().encode("TestPassword123");
    @Autowired private WebApplicationContext context;
    @Autowired private ProductRepository products;
    @MockitoSpyBean private ProductVariantRepository variants;
    @Autowired private SizeRepository sizes;
    @Autowired private ColorRepository colors;
    @Autowired private UserRepository users;
    @Autowired private RoleRepository roles;
    @Autowired private JwtService jwt;
    @Autowired private ProductService productService;
    @Autowired private ObjectMapper mapper;
    @Autowired private EntityManager entityManager;
    @Autowired private EntityManagerFactory entityManagerFactory;
    private MockMvc mvc;
    private final Map<String, User> accounts = new HashMap<>();
    private Size small, medium, large;
    private Color black, white;
    private Product shirt, pants, jacket, hidden;
    private ProductVariant editableVariant, inactiveVariant;

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
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        small = size("S");
        medium = size("M");
        large = size("L");
        black = color("Black");
        white = color("White");
        shirt = product("Basic Shirt", "Soft cotton", "100", ActiveStatus.ACTIVE);
        pants = product("Summer Pants", "Comfortable linen", "200", ActiveStatus.ACTIVE);
        jacket = product("Other Jacket", "Literal 100%_safe!", "300", ActiveStatus.ACTIVE);
        hidden = product("Hidden Shirt", "Not public", "50", ActiveStatus.INACTIVE);
        editableVariant = variant(shirt, small, black, "SHIRT-S-BLACK", "120", 5, ActiveStatus.ACTIVE);
        variant(shirt, medium, white, "SHIRT-M-WHITE", "130", 7, ActiveStatus.ACTIVE);
        variant(shirt, medium, black, "SHIRT-M-BLACK", "125", 6, ActiveStatus.ACTIVE);
        inactiveVariant = variant(shirt, large, black, "SHIRT-L-BLACK", "140", 9, ActiveStatus.INACTIVE);
        variant(pants, medium, black, "PANTS-M-BLACK", "220", 2, ActiveStatus.ACTIVE);
        variant(hidden, small, black, "HIDDEN-S-BLACK", "55", 4, ActiveStatus.ACTIVE);
        for (String role : new String[] {"ADMIN", "STAFF", "CUSTOMER"}) {
            User user = new User();
            user.setUsername("catalog-" + role);
            user.setFullName(role);
            user.setPasswordHash(TEST_HASH);
            user.setRole(roles.findByRoleName(role).orElseThrow());
            user.setStatus(UserStatus.ACTIVE);
            accounts.put(role, users.saveAndFlush(user));
        }
    }

    private Size size(String name) {
        Size size = new Size();
        size.setSizeName(name);
        return sizes.saveAndFlush(size);
    }

    private Color color(String name) {
        Color color = new Color();
        color.setColorName(name);
        return colors.saveAndFlush(color);
    }

    private Product product(String name, String description, String price, ActiveStatus status) {
        Product product = new Product();
        product.setProductName(name);
        product.setDescription(description);
        product.setPrice(new BigDecimal(price));
        product.setStatus(status);
        return products.saveAndFlush(product);
    }

    private ProductVariant variant(Product product, Size size, Color color, String sku,
            String price, int quantity, ActiveStatus status) {
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setSize(size);
        variant.setColor(color);
        variant.setSku(sku);
        variant.setPrice(new BigDecimal(price));
        variant.setQuantity(quantity);
        variant.setStatus(status);
        return variants.saveAndFlush(variant);
    }

    private String token(String role) {
        return "Bearer " + jwt.issue(new StoreUserDetails(accounts.get(role))).token();
    }

    private String productJson(String name, String price, String status) {
        return mapper.writeValueAsString(Map.of("productName", name, "description", "Sample",
                "price", new BigDecimal(price), "imgUrl", "/static/product.png", "status", status));
    }

    private String variantJson(Long sizeId, Long colorId, String sku, String price) {
        return mapper.writeValueAsString(Map.of("sizeId", sizeId, "colorId", colorId,
                "sku", sku, "price", new BigDecimal(price), "status", "ACTIVE"));
    }

    @Test
    void publicListNeedsNoJwtAndReturnsOnlyActiveSummaries() throws Exception {
        var result = mvc.perform(get("/api/products")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.page").value(0)).andExpect(jsonPath("$.size").value(12))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].variants").doesNotExist())
                .andExpect(jsonPath("$.content[0].description").doesNotExist()).andReturn();
        assertThat(result.getRequest().getSession(false)).isNull();
        assertThat(result.getResponse().getContentAsString()).doesNotContain("Hidden Shirt");
    }

    @Test
    void detailHasBasePriceAndOnlyActiveVariantsWithTheirOwnPrices() throws Exception {
        var result = mvc.perform(get("/api/products/" + shirt.getProductId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.variants.length()").value(3))
                .andExpect(jsonPath("$.variants[0].size.sizeName").value("S"))
                .andExpect(jsonPath("$.variants[0].color.colorName").value("Black")).andReturn();
        var detail = mapper.readValue(result.getResponse().getContentAsString(), ProductDetailResponse.class);
        assertThat(detail.basePrice()).isEqualByComparingTo("100");
        assertThat(detail.variants().getFirst().price()).isEqualByComparingTo("120");
        assertThat(detail.variants().getFirst().quantity()).isEqualTo(5);
        assertThat(detail.variants()).allSatisfy(variant -> assertThat(variant.status()).isEqualTo(ActiveStatus.ACTIVE));
    }

    @Test
    void missingAndInactiveProductsAre404ForDetailAndVariants() throws Exception {
        for (long id : new long[] {999999L, hidden.getProductId()}) {
            mvc.perform(get("/api/products/" + id)).andExpect(status().isNotFound());
            mvc.perform(get("/api/products/" + id + "/variants")).andExpect(status().isNotFound());
        }
    }

    @Test
    void activeProductCanHaveNoVariantsYet() throws Exception {
        mvc.perform(get("/api/products/" + jacket.getProductId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.variants.length()").value(0));
    }

    @Test
    void publicVariantListFiltersInactiveVariants() throws Exception {
        mvc.perform(get("/api/products/" + shirt.getProductId() + "/variants"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void paginationHasStablePagesAndCorrectTotals() throws Exception {
        mvc.perform(get("/api/products").param("page", "0").param("size", "1").param("sortBy", "price")
                        .param("direction", "asc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].productName").value("Basic Shirt"))
                .andExpect(jsonPath("$.totalElements").value(3)).andExpect(jsonPath("$.totalPages").value(3));
        mvc.perform(get("/api/products").param("page", "1").param("size", "1").param("sortBy", "price")
                        .param("direction", "asc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.content[0].productName").value("Summer Pants"));
        mvc.perform(get("/api/products").param("page", "8").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void searchMatchesNameAndDescriptionCaseInsensitively() throws Exception {
        mvc.perform(get("/api/products").param("keyword", "SHIRT"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].productName").value("Basic Shirt"));
        mvc.perform(get("/api/products").param("keyword", "LINEN"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].productName").value("Summer Pants"));
    }

    @Test
    void searchTreatsPercentAndUnderscoreAsLiteralCharacters() throws Exception {
        mvc.perform(get("/api/products").param("keyword", "%_"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].productName").value("Other Jacket"));
    }

    @Test
    void sizeFilterHasNoDuplicateProductsDespiteSeveralMatchingVariants() throws Exception {
        mvc.perform(get("/api/products").param("sizeId", medium.getSizeId().toString()).param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(2)).andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void colorFilterHasNoDuplicateProducts() throws Exception {
        mvc.perform(get("/api/products").param("colorId", black.getColorId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void combinedFiltersMustMatchTheSameActiveVariant() throws Exception {
        mvc.perform(get("/api/products").param("sizeId", small.getSizeId().toString())
                        .param("colorId", white.getColorId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/products").param("sizeId", small.getSizeId().toString())
                        .param("colorId", black.getColorId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/products").param("sizeId", large.getSizeId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void keywordAndAttributeFiltersCanBeCombined() throws Exception {
        mvc.perform(get("/api/products").param("keyword", "shirt").param("sizeId", medium.getSizeId().toString())
                        .param("colorId", black.getColorId().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void unknownFilterIdReturnsEmptyCatalog() throws Exception {
        mvc.perform(get("/api/products").param("sizeId", "999999"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"page=-1", "size=0", "size=101", "page=abc", "sizeId=0", "colorId=-1",
            "sortBy=variants.quantity", "direction=invalid"})
    void invalidCatalogParametersReturn400(String parameter) throws Exception {
        String[] pair = parameter.split("=");
        mvc.perform(get("/api/products").param(pair[0], pair[1]))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void overflowingPageOffsetIs400() throws Exception {
        mvc.perform(get("/api/products").param("page", "2147483647").param("size", "100"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void descendingPriceSortUsesWhitelist() throws Exception {
        mvc.perform(get("/api/products").param("sortBy", "price").param("direction", "desc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].productName").value("Other Jacket"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "STAFF"})
    void adminAndStaffCanCreateProduct(String role) throws Exception {
        mvc.perform(post("/api/admin/products").header("Authorization", token(role))
                        .contentType(MediaType.APPLICATION_JSON).content(productJson("New Product", "10", "ACTIVE")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.productName").value("New Product"));
        assertThat(products.count()).isEqualTo(5);
    }

    @Test
    void anonymousWriteIs401AndCustomerWriteIs403() throws Exception {
        String body = productJson("Blocked", "10", "ACTIVE");
        mvc.perform(post("/api/admin/products").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized()).andExpect(header().doesNotExist("Location"));
        mvc.perform(post("/api/admin/products").header("Authorization", token("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
        assertThat(products.count()).isEqualTo(4);
    }

    @Test
    void publicPermitIsLimitedToCatalogGetRoutes() throws Exception {
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("Blocked", "10", "ACTIVE")))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/products/1/private")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/unrelated").header("Authorization", token("STAFF")))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "-1", "0.001", "10000000000"})
    void invalidProductNameOrPriceIs400(String value) throws Exception {
        String payload = value.isEmpty() ? productJson(" ", "1", "ACTIVE") : productJson("Invalid", value, "ACTIVE");
        mvc.perform(post("/api/admin/products").header("Authorization", token("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidStatusAndOversizedTextAre400() throws Exception {
        mvc.perform(post("/api/admin/products").header("Authorization", token("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(productJson("Invalid", "10", "DELETED")))
                .andExpect(status().isBadRequest());
        String payload = mapper.writeValueAsString(Map.of("productName", "Long text", "price", 1,
                "status", "ACTIVE", "description", "é".repeat(32768)));
        mvc.perform(post("/api/admin/products").header("Authorization", token("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void zeroPriceIsValidAccordingToSchema() throws Exception {
        mvc.perform(post("/api/admin/products").header("Authorization", token("STAFF"))
                        .contentType(MediaType.APPLICATION_JSON).content(productJson("Free", "0", "ACTIVE")))
                .andExpect(status().isCreated());
    }

    @Test
    void updatingProductDoesNotChangeItsVariants() throws Exception {
        mvc.perform(put("/api/admin/products/" + shirt.getProductId()).header("Authorization", token("STAFF"))
                        .contentType(MediaType.APPLICATION_JSON).content(productJson("Updated Shirt", "250", "ACTIVE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.productName").value("Updated Shirt"));
        assertThat(variants.findById(editableVariant.getVariantId()).orElseThrow().getPrice())
                .isEqualByComparingTo("120");
        assertThat(variants.findById(editableVariant.getVariantId()).orElseThrow().getQuantity()).isEqualTo(5);
    }

    @ParameterizedTest
    @ValueSource(strings = {"id", "variants", "quantity"})
    void productUpdateRejectsUnsupportedFields(String field) throws Exception {
        String payload = productJson("Invalid", "10", "ACTIVE").replace("}", ",\"" + field + "\":1}");
        mvc.perform(put("/api/admin/products/" + shirt.getProductId()).header("Authorization", token("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isBadRequest());
        assertThat(products.findById(shirt.getProductId()).orElseThrow().getProductName()).isEqualTo("Basic Shirt");
    }

    @Test
    void deactivateHidesProductButPreservesRowsAndStock() throws Exception {
        long variantCount = variants.count();
        mvc.perform(patch("/api/admin/products/" + shirt.getProductId() + "/status")
                        .header("Authorization", token("STAFF")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("INACTIVE"));
        assertThat(products.existsById(shirt.getProductId())).isTrue();
        assertThat(variants.count()).isEqualTo(variantCount);
        assertThat(variants.findById(editableVariant.getVariantId()).orElseThrow().getQuantity()).isEqualTo(5);
        mvc.perform(get("/api/products/" + shirt.getProductId())).andExpect(status().isNotFound());
        mvc.perform(get("/api/products/" + shirt.getProductId() + "/variants")).andExpect(status().isNotFound());
        mvc.perform(get("/api/products")).andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void missingProductUpdateIs404() throws Exception {
        mvc.perform(put("/api/admin/products/999999").header("Authorization", token("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(productJson("Missing", "10", "ACTIVE")))
                .andExpect(status().isNotFound());
    }

    @Test
    void creatingVariantStartsAtZeroStock() throws Exception {
        mvc.perform(post("/api/admin/products/" + shirt.getProductId() + "/variants")
                        .header("Authorization", token("STAFF")).contentType(MediaType.APPLICATION_JSON)
                        .content(variantJson(large.getSizeId(), white.getColorId(), "NEW-L-WHITE", "155")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.quantity").value(0))
                .andExpect(jsonPath("$.sku").value("NEW-L-WHITE"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"product", "size", "color"})
    void missingVariantReferenceIs404(String reference) throws Exception {
        Long productId = reference.equals("product") ? 999999L : shirt.getProductId();
        Long sizeId = reference.equals("size") ? 999999L : large.getSizeId();
        Long colorId = reference.equals("color") ? 999999L : white.getColorId();
        mvc.perform(post("/api/admin/products/" + productId + "/variants")
                        .header("Authorization", token("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                        .content(variantJson(sizeId, colorId, "MISSING-REFERENCE", "150")))
                .andExpect(status().isNotFound());
    }

    @Test
    void duplicateVariantAttributesAndSkuAre409() throws Exception {
        mvc.perform(post("/api/admin/products/" + shirt.getProductId() + "/variants")
                        .header("Authorization", token("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                        .content(variantJson(small.getSizeId(), black.getColorId(), "OTHER-SKU", "150")))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/admin/products/" + shirt.getProductId() + "/variants")
                        .header("Authorization", token("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                        .content(variantJson(large.getSizeId(), white.getColorId(), "shirt-s-black", "150")))
                .andExpect(status().isConflict());
    }

    @Test
    void databaseUniqueConstraintStillProtectsAgainstRacesAndReturnsSanitized409() throws Exception {
        // Simulate a race after the application precheck; the original DB constraint is the final guard.
        doReturn(false).when(variants).existsBySkuIgnoreCase(anyString());
        var result = mvc.perform(post("/api/admin/products/" + shirt.getProductId() + "/variants")
                        .header("Authorization", token("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                        .content(variantJson(large.getSizeId(), white.getColorId(), "SHIRT-S-BLACK", "150")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409)).andReturn();
        assertThat(result.getResponse().getContentAsString())
                .doesNotContain("INSERT", "insert into", "constraint", "trace", "SHIRT-S-BLACK");
    }

    @Test
    void updatingVariantPreservesQuantityAndProductBasePrice() throws Exception {
        mvc.perform(put("/api/admin/product-variants/" + editableVariant.getVariantId())
                        .header("Authorization", token("STAFF")).contentType(MediaType.APPLICATION_JSON)
                        .content(variantJson(small.getSizeId(), black.getColorId(), "UPDATED-SKU", "170")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.quantity").value(5))
                .andExpect(jsonPath("$.sku").value("UPDATED-SKU"));
        assertThat(products.findById(shirt.getProductId()).orElseThrow().getPrice()).isEqualByComparingTo("100");
    }

    @Test
    void variantUpdateRejectsStockAndParentIdChanges() throws Exception {
        String base = variantJson(small.getSizeId(), black.getColorId(), "UPDATED-SKU", "170");
        for (String field : new String[] {"quantity", "productId", "id"}) {
            mvc.perform(put("/api/admin/product-variants/" + editableVariant.getVariantId())
                            .header("Authorization", token("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                            .content(base.replace("}", ",\"" + field + "\":99}")))
                    .andExpect(status().isBadRequest());
        }
        assertThat(variants.findById(editableVariant.getVariantId()).orElseThrow().getQuantity()).isEqualTo(5);
    }

    @Test
    void variantCreateAlsoRejectsClientQuantity() throws Exception {
        String payload = variantJson(large.getSizeId(), white.getColorId(), "NEW", "10")
                .replace("}", ",\"quantity\":20}");
        mvc.perform(post("/api/admin/products/" + shirt.getProductId() + "/variants")
                        .header("Authorization", token("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void variantUpdateDetectsDuplicatesAndMissingId() throws Exception {
        mvc.perform(put("/api/admin/product-variants/" + editableVariant.getVariantId())
                        .header("Authorization", token("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                        .content(variantJson(medium.getSizeId(), white.getColorId(), "NEW-SKU", "170")))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/admin/product-variants/" + editableVariant.getVariantId())
                        .header("Authorization", token("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                        .content(variantJson(small.getSizeId(), black.getColorId(), "PANTS-M-BLACK", "170")))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/admin/product-variants/999999")
                        .header("Authorization", token("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                        .content(variantJson(small.getSizeId(), black.getColorId(), "NEW-SKU", "170")))
                .andExpect(status().isNotFound());
    }

    @Test
    void publicSizeAndColorListsAre200() throws Exception {
        mvc.perform(get("/api/sizes")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3));
        mvc.perform(get("/api/colors")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "STAFF"})
    void adminAndStaffCanCreateSizeAndColor(String role) throws Exception {
        mvc.perform(post("/api/admin/sizes").header("Authorization", token(role))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"sizeName\":\"XL\"}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/admin/colors").header("Authorization", token(role))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"colorName\":\"Red\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void customerCannotWriteVariantsSizesColorsOrStatus() throws Exception {
        mvc.perform(post("/api/admin/products/" + shirt.getProductId() + "/variants")
                        .header("Authorization", token("CUSTOMER")).contentType(MediaType.APPLICATION_JSON)
                        .content(variantJson(large.getSizeId(), white.getColorId(), "BLOCKED", "10")))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/admin/product-variants/" + editableVariant.getVariantId())
                        .header("Authorization", token("CUSTOMER")).contentType(MediaType.APPLICATION_JSON)
                        .content(variantJson(small.getSizeId(), black.getColorId(), "BLOCKED", "10")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/sizes").header("Authorization", token("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"sizeName\":\"XL\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/colors").header("Authorization", token("CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"colorName\":\"Red\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/admin/products/" + shirt.getProductId() + "/status")
                        .header("Authorization", token("CUSTOMER")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updatingSizeAndColorKeepsTheirIdentifiersAndVariantReferences() throws Exception {
        mvc.perform(put("/api/admin/sizes/" + small.getSizeId()).header("Authorization", token("STAFF"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"sizeName\":\"XS\"}"))
                .andExpect(status().isOk());
        mvc.perform(put("/api/admin/colors/" + black.getColorId()).header("Authorization", token("STAFF"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"colorName\":\"Jet Black\"}"))
                .andExpect(status().isOk());
        var variant = variants.findById(editableVariant.getVariantId()).orElseThrow();
        assertThat(variant.getSize().getSizeId()).isEqualTo(small.getSizeId());
        assertThat(variant.getSize().getSizeName()).isEqualTo("XS");
        assertThat(variant.getColor().getColorId()).isEqualTo(black.getColorId());
    }

    @Test
    void duplicateSizeAndColorNamesReturn409() throws Exception {
        mvc.perform(post("/api/admin/sizes").header("Authorization", token("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"sizeName\":\"s\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/admin/colors").header("Authorization", token("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"colorName\":\"black\"}"))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/admin/sizes/" + small.getSizeId()).header("Authorization", token("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"sizeName\":\"M\"}"))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/admin/colors/" + black.getColorId()).header("Authorization", token("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"colorName\":\"White\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void missingLookupUpdateIs404AndInvalidLookupBodyIs400() throws Exception {
        mvc.perform(put("/api/admin/sizes/999999").header("Authorization", token("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"sizeName\":\"XL\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/admin/colors/999999").header("Authorization", token("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"colorName\":\"Red\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/admin/sizes").header("Authorization", token("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("sizeName", "X".repeat(21)))))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/admin/colors").header("Authorization", token("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"colorName\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void noHardDeleteRouteExists() throws Exception {
        mvc.perform(delete("/api/admin/products/" + shirt.getProductId()).header("Authorization", token("ADMIN")))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(delete("/api/admin/sizes/" + small.getSizeId()).header("Authorization", token("ADMIN")))
                .andExpect(status().isMethodNotAllowed());
        assertThat(products.existsById(shirt.getProductId())).isTrue();
    }

    @Test
    void metadataUpdateDoesNotOverwriteStockChangedAfterEntityWasLoaded() throws Exception {
        Long variantId = editableVariant.getVariantId();
        entityManager.createNativeQuery("update product_variants set quantity = 3 where variant_id = :id")
                .setParameter("id", variantId).executeUpdate();
        mvc.perform(put("/api/admin/product-variants/" + variantId)
                        .header("Authorization", token("STAFF")).contentType(MediaType.APPLICATION_JSON)
                        .content(variantJson(small.getSizeId(), black.getColorId(), "UPDATED-STOCK-SAFE", "170")))
                .andExpect(status().isOk());
        entityManager.clear();
        assertThat(variants.findById(variantId).orElseThrow().getQuantity()).isEqualTo(3);
    }
    @Test
    void queryCountsRemainConstantWithMultipleVariants() {
        entityManager.flush();
        entityManager.clear(); // No fixture entities may hide lazy-load queries in this check.
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        var page = productService.listPublic(0, 1, null, null, null, "price", "asc");
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2); // Summary query + count.
        entityManager.clear();
        statistics.clear();
        var detail = productService.getPublicDetail(shirt.getProductId());
        assertThat(detail.variants()).hasSize(3);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2); // Product + graph-fetched variants.
    }
}
