package com.lauriel.store.controller;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
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
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.MOCK)
class WebAdminTest {
    private static final String PASSWORD="WebFixturePassword123";
    private static final String HASH=new BCryptPasswordEncoder().encode(PASSWORD);
    @Autowired WebApplicationContext context;
    @Autowired ProductRepository products;
    @Autowired ProductVariantRepository variants;
    @Autowired SizeRepository sizes;
    @Autowired ColorRepository colors;
    @Autowired SupplierRepository suppliers;
    @Autowired PurchaseReceiptRepository receipts;
    @Autowired PurchaseReceiptDetailRepository receiptItems;
    @Autowired InventoryTransactionRepository history;
    @Autowired OrderRepository orders;
    @Autowired OrderDetailRepository orderItems;
    @Autowired VoucherRepository vouchers;
    @Autowired PromotionRepository promotions;
    @Autowired PromotionProductRepository associations;
    @Autowired CustomerRepository customers;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired OrderService orderService;
    @Autowired PurchaseReceiptService receiptService;
    @Autowired PlatformTransactionManager manager;
    private MockMvc mvc;
    private TransactionTemplate tx;
    private final Map<String,User> accounts=new HashMap<>();
    private Product product;
    private ProductVariant variant;
    private Supplier supplier;
    private Voucher voucher;
    private Promotion promotion;

    @DynamicPropertySource static void properties(DynamicPropertyRegistry p) throws Exception {
        String url=TestSchema.createIsolatedDatabase(),secret=TestSecrets.randomBase64();
        p.add("spring.datasource.url",()->url);p.add("spring.datasource.username",()->"sa");
        p.add("spring.datasource.password",()->"");p.add("spring.datasource.driver-class-name",()->"org.h2.Driver");
        p.add("spring.jpa.hibernate.ddl-auto",()->"validate");p.add("spring.jpa.open-in-view",()->"false");
        p.add("app.jwt.secret",()->secret);
    }
    @BeforeEach void fixtures() {
        mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        tx=new TransactionTemplate(manager);
        tx.executeWithoutResult(s -> {
            history.deleteAllInBatch();orderItems.deleteAllInBatch();orders.deleteAllInBatch();
            receiptItems.deleteAllInBatch();receipts.deleteAllInBatch();suppliers.deleteAllInBatch();
            associations.deleteAllInBatch();promotions.deleteAllInBatch();vouchers.deleteAllInBatch();
            variants.deleteAllInBatch();products.deleteAllInBatch();sizes.deleteAllInBatch();colors.deleteAllInBatch();
            customers.deleteAllInBatch();users.deleteAllInBatch();
            for(String role:List.of("ADMIN","STAFF","CUSTOMER")) {
                var u=new User();u.setUsername("web-"+role);u.setFullName("Fixture "+role);u.setPasswordHash(HASH);
                u.setRole(roles.findByRoleName(role).orElseThrow());u.setStatus(UserStatus.ACTIVE);
                accounts.put(role,users.saveAndFlush(u));
                if(role.equals("CUSTOMER")) {
                    var c=new Customer();c.setUser(u);c.setCustomerName("Web customer");c.setPhone("0900000000");
                    customers.saveAndFlush(c);
                }
            }
            var walkIn=new Customer();walkIn.setCustomerName("Walk-in customer");customers.saveAndFlush(walkIn);
            product=new Product();product.setProductName("Web shirt");product.setPrice(new BigDecimal("250000"));
            product.setStatus(ActiveStatus.INACTIVE);products.saveAndFlush(product);
            var size=new Size();size.setSizeName("M");sizes.saveAndFlush(size);
            var color=new Color();color.setColorName("Green");colors.saveAndFlush(color);
            variant=new ProductVariant();variant.setProduct(product);variant.setSize(size);variant.setColor(color);
            variant.setSku("WEB-SKU");variant.setPrice(new BigDecimal("250000"));variant.setQuantity(5);
            variant.setStatus(ActiveStatus.INACTIVE);variants.saveAndFlush(variant);
            supplier=new Supplier();supplier.setSupplierName("Web supplier");supplier.setStatus(ActiveStatus.ACTIVE);suppliers.saveAndFlush(supplier);
            voucher=new Voucher();voucher.setCode("WEB10");voucher.setVoucherName("Web voucher");voucher.setDiscountType(DiscountType.PERCENT);
            voucher.setDiscountValue(new BigDecimal("10"));voucher.setMinOrderAmount(BigDecimal.ZERO);voucher.setQuantity(2);
            voucher.setStartDate(LocalDateTime.now().minusDays(1));voucher.setEndDate(LocalDateTime.now().plusDays(1));
            voucher.setStatus(ActiveStatus.ACTIVE);vouchers.saveAndFlush(voucher);
            promotion=new Promotion();promotion.setPromotionName("Web campaign");promotion.setDiscountPercent(new BigDecimal("20"));
            promotion.setStartDate(LocalDateTime.now().minusDays(1));promotion.setEndDate(LocalDateTime.now().plusDays(1));
            promotion.setStatus(ActiveStatus.INACTIVE);promotions.saveAndFlush(promotion);
        });
    }
    private MockHttpServletRequestBuilder admin(MockHttpServletRequestBuilder r) {
        return r.with(user(new StoreUserDetails(accounts.get("ADMIN"))));
    }
    private MockHttpServletRequestBuilder staff(MockHttpServletRequestBuilder r) {
        return r.with(user(new StoreUserDetails(accounts.get("STAFF"))));
    }
    private String productPath() {return "/admin/products/"+product.getProductId();}
    private String voucherPath() {return "/admin/vouchers/"+voucher.getVoucherId();}
    private String promotionPath() {return "/admin/promotions/"+promotion.getPromotionId();}
    private String receipt() {
        return "/admin/purchase-receipts/"+receiptService.create(new PurchaseReceiptCreateRequest(supplier.getSupplierId()),
                accounts.get("STAFF").getUserId()).receiptId();
    }
    private Long checkout(boolean useVoucher) {
        tx.executeWithoutResult(s -> {
            var p=products.findById(product.getProductId()).orElseThrow();p.setStatus(ActiveStatus.ACTIVE);products.saveAndFlush(p);
            var v=variants.findById(variant.getVariantId()).orElseThrow();v.setStatus(ActiveStatus.ACTIVE);variants.saveAndFlush(v);
        });
        return orderService.checkout(new CheckoutRequest(List.of(new CheckoutItemRequest(variant.getVariantId(),2)),
                "12 Web Street",useVoucher?"WEB10":null),accounts.get("CUSTOMER").getUserId()).orderId();
    }
    private MockHttpServletRequestBuilder productForm(String path,String name) { return productForm(path,name,"300000"); }
    private MockHttpServletRequestBuilder productForm(String path,String name,String price) {
        return admin(post(path).with(csrf()).param("productName",name).param("description","Web description")
                .param("price",price).param("imgUrl","").param("status","ACTIVE"));
    }
    private MockHttpServletRequestBuilder voucherForm(String path,String code) { return voucherForm(path,code,"15","2027-01-01T00:00"); }
    private MockHttpServletRequestBuilder voucherForm(String path,String code,String discount,String end) {
        return admin(post(path).with(csrf()).param("code",code).param("voucherName","Updated voucher").param("discountType","PERCENT")
                .param("discountValue",discount).param("minOrderAmount","0").param("quantity","3")
                .param("startDate","2026-01-01T00:00").param("endDate",end).param("status","ACTIVE"));
    }
    private MockHttpServletRequestBuilder promotionForm(String path,String name) {
        return admin(post(path).with(csrf()).param("promotionName",name).param("description","Campaign")
                .param("discountPercent","25").param("startDate","2026-01-01T00:00").param("endDate","2027-01-01T00:00").param("status","ACTIVE"));
    }

    @Test void loginPageIsPublicWithCsrfAndRealForm() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk()).andExpect(view().name("auth/login"))
                .andExpect(content().string(containsString("name=\"username\"")))
                .andExpect(content().string(containsString("name=\"password\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
        mvc.perform(get("/login?error")).andExpect(content().string(containsString("Không thể đăng nhập")));
        mvc.perform(get("/login?logout")).andExpect(content().string(containsString("Bạn đã đăng xuất")));
    }
    @ParameterizedTest @ValueSource(strings={"/assets/css/main.css","/assets/css/admin.css","/assets/js/admin.js",
            "/assets/libs/bootstrap/css/bootstrap.min.css","/assets/libs/bootstrap/js/bootstrap.bundle.min.js",
            "/assets/libs/bootstrap-icons/bootstrap-icons.css","/assets/libs/bootstrap-icons/fonts/bootstrap-icons.woff2","/assets/images/favicon.ico"})
    void assetsArePublic(String path) throws Exception {mvc.perform(get(path)).andExpect(status().isOk());}
    @Test void anonymousRedirectsToLogin() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
    }
    @ParameterizedTest @ValueSource(strings={"/admin","/admin/products","/admin/inventory","/admin/purchase-receipts","/admin/orders","/admin/customers"})
    void customerCannotUseWeb(String path) throws Exception {
        mvc.perform(get(path).with(user(new StoreUserDetails(accounts.get("CUSTOMER"))))).andExpect(status().isForbidden());
    }
    @ParameterizedTest @ValueSource(strings={"ADMIN","STAFF"})
    void actualFormLoginCreatesSessionAndRedirectsDashboard(String role) throws Exception {
        var result=mvc.perform(formLogin().user(accounts.get(role).getUsername()).password(PASSWORD))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/admin")).andReturn();
        var session=(MockHttpSession)result.getRequest().getSession(false);
        assertThat(session).isNotNull();
        mvc.perform(get("/admin").session(session)).andExpect(status().isOk()).andExpect(view().name("admin/dashboard"));
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
    }
    @Test void invalidPasswordFails() throws Exception {
        mvc.perform(formLogin().user("web-ADMIN").password("incorrect")).andExpect(redirectedUrl("/login?error"));
    }
    @ParameterizedTest @EnumSource(value=UserStatus.class,names={"INACTIVE","LOCKED"})
    void disabledAccountCannotLogin(UserStatus state) throws Exception {
        tx.executeWithoutResult(s -> {var u=users.findById(accounts.get("ADMIN").getUserId()).orElseThrow();u.setStatus(state);users.saveAndFlush(u);});
        mvc.perform(formLogin().user("web-ADMIN").password(PASSWORD)).andExpect(redirectedUrl("/login?error"));
    }
    @Test void logoutRequiresCsrfAndInvalidatesSession() throws Exception {
        var result=mvc.perform(formLogin().user("web-STAFF").password(PASSWORD)).andReturn();
        var session=(MockHttpSession)result.getRequest().getSession(false);
        mvc.perform(post("/logout").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/logout").session(session).with(csrf())).andExpect(redirectedUrl("/login?logout"));
        assertThat(session.isInvalid()).isTrue();
    }
    @ParameterizedTest @ValueSource(strings={"/admin/vouchers","/admin/promotions","/admin/vouchers/new","/admin/promotions/new"})
    void staffCannotReadMarketing(String path) throws Exception {mvc.perform(staff(get(path))).andExpect(status().isForbidden());}
    @Test void staffCannotWriteMarketingEvenWithCsrf() throws Exception {
        mvc.perform(staff(post("/admin/vouchers").with(csrf()))).andExpect(status().isForbidden());
        mvc.perform(staff(post(promotionPath()+"/products").with(csrf()).param("productId",product.getProductId().toString()))).andExpect(status().isForbidden());
    }
    @Test void staffSidebarHidesMarketingAndHighlightsProducts() throws Exception {
        mvc.perform(staff(get("/admin/products"))).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("href=\"/admin/vouchers\""))))
                .andExpect(content().string(containsString("sidebar-menu-link active")));
    }
    @ParameterizedTest @ValueSource(strings={"/admin","/admin/dashboard","/admin/products","/admin/products/new","/admin/sizes","/admin/sizes/new",
            "/admin/colors","/admin/colors/new","/admin/suppliers","/admin/suppliers/new","/admin/inventory",
            "/admin/inventory?threshold=5","/admin/inventory/transactions","/admin/purchase-receipts","/admin/purchase-receipts/new",
            "/admin/orders","/admin/customers","/admin/vouchers","/admin/vouchers/new","/admin/promotions","/admin/promotions/new"})
    void adminPagesRenderWithoutOpenInView(String path) throws Exception {
        var result=mvc.perform(admin(get(path))).andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("text/html")).andReturn();
        var forms=java.util.regex.Pattern.compile("<form\\b[^>]*method=\"post\"[^>]*>(.*?)</form>",java.util.regex.Pattern.DOTALL)
                .matcher(result.getResponse().getContentAsString());
        while(forms.find()) assertThat(forms.group(1)).contains("name=\"_csrf\"");
    }
    @Test void editAndVariantAndAssociationPagesRender() throws Exception {
        for(String path:List.of(productPath()+"/edit",productPath()+"/variants",productPath()+"/variants/new",
                productPath()+"/variants/"+variant.getVariantId()+"/edit",
                "/admin/sizes/"+variant.getSize().getSizeId()+"/edit","/admin/colors/"+variant.getColor().getColorId()+"/edit",
                "/admin/suppliers/"+supplier.getSupplierId()+"/edit",voucherPath()+"/edit",promotionPath()+"/edit",promotionPath()+"/products")) {
            var result=mvc.perform(admin(get(path))).andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("text/html")).andReturn();
        var forms=java.util.regex.Pattern.compile("<form\\b[^>]*method=\"post\"[^>]*>(.*?)</form>",java.util.regex.Pattern.DOTALL)
                .matcher(result.getResponse().getContentAsString());
        while(forms.find()) assertThat(forms.group(1)).contains("name=\"_csrf\"");
        }
    }
    @Test void productSearchStatusAndPaginationKeepFilters() throws Exception {
        mvc.perform(staff(get("/admin/products").param("keyword","Web").param("status","INACTIVE").param("size","1")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Web shirt")))
                .andExpect(content().string(containsString("INACTIVE")));
        tx.executeWithoutResult(s -> {var p=new Product();p.setProductName("Web second");p.setPrice(BigDecimal.TEN);p.setStatus(ActiveStatus.INACTIVE);products.saveAndFlush(p);});
        var result=mvc.perform(admin(get("/admin/products").param("keyword","Web").param("status","INACTIVE").param("size","1")))
                .andExpect(status().isOk()).andReturn();
        assertThat(result.getResponse().getContentAsString()).contains("keyword=Web","status=INACTIVE","size=1","page=1");
        mvc.perform(admin(get("/admin/products").param("keyword","missing"))).andExpect(status().isOk()).andExpect(content().string(containsString("Chưa có dữ liệu phù hợp")));
    }
    @Test void missingCsrfCannotCreateProduct() throws Exception {
        long count=products.count();mvc.perform(admin(post("/admin/products").param("productName","Blocked"))).andExpect(status().isForbidden());
        assertThat(products.count()).isEqualTo(count);
    }
    @Test void validProductCreateEditAndStatusReuseServices() throws Exception {
        mvc.perform(productForm("/admin/products","New shirt")).andExpect(redirectedUrl("/admin/products")).andExpect(flash().attributeExists("successMessage"));
        assertThat(products.count()).isEqualTo(2);
        mvc.perform(productForm(productPath(),"Updated shirt")).andExpect(redirectedUrl("/admin/products"));
        mvc.perform(staff(post(productPath()+"/status").with(csrf()).param("status","INACTIVE"))).andExpect(redirectedUrl("/admin/products"));
        assertThat(products.findById(product.getProductId()).orElseThrow().getProductName()).isEqualTo("Updated shirt");
        assertThat(products.findById(product.getProductId()).orElseThrow().getStatus()).isEqualTo(ActiveStatus.INACTIVE);
    }
    @Test void invalidProductRendersFieldErrorsAndRetainsInput() throws Exception {
        long count=products.count();
        mvc.perform(productForm("/admin/products","","-10")).andExpect(status().isOk())
                .andExpect(view().name("admin/products/form")).andExpect(model().attributeHasFieldErrors("form","productName","price"))
                .andExpect(content().string(containsString("Web description")));
        assertThat(products.count()).isEqualTo(count);
    }
    @Test void invalidNumberBindingRendersForm() throws Exception {
        mvc.perform(productForm(productPath(),"Kept","not-a-number")).andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form","price"));
    }
    @Test void variantMetadataCannotChangeQuantity() throws Exception {
        var result=mvc.perform(staff(get(productPath()+"/variants/"+variant.getVariantId()+"/edit"))).andExpect(status().isOk()).andReturn();
        assertThat(result.getResponse().getContentAsString()).contains("Quantity: 5").doesNotContain("name=\"quantity\"");
        mvc.perform(staff(post(productPath()+"/variants/"+variant.getVariantId()).with(csrf()).param("sku","NEW-WEB-SKU")
                .param("sizeId",variant.getSize().getSizeId().toString()).param("colorId",variant.getColor().getColorId().toString())
                .param("price","350000").param("status","ACTIVE").param("quantity","999")))
                .andExpect(redirectedUrl(productPath()+"/variants"));
        assertThat(variants.findById(variant.getVariantId()).orElseThrow().getQuantity()).isEqualTo(5);
    }
    @Test void variantCreateStartsWithZeroStockAndDuplicateRendersForm() throws Exception {
        var size=tx.execute(s->{var z=new Size();z.setSizeName("L");return sizes.saveAndFlush(z);});
        var request=staff(post(productPath()+"/variants").with(csrf()).param("sku","NEW-VARIANT").param("sizeId",size.getSizeId().toString())
                .param("colorId",variant.getColor().getColorId().toString()).param("price","100").param("status","ACTIVE").param("quantity","900"));
        mvc.perform(request).andExpect(redirectedUrl(productPath()+"/variants"));
        assertThat(variants.findAll().stream().filter(v->v.getSku().equals("NEW-VARIANT")).findFirst().orElseThrow().getQuantity()).isZero();
        mvc.perform(request).andExpect(status().isOk()).andExpect(view().name("admin/products/variant-form")).andExpect(model().attributeHasErrors("form"));
    }
    @Test void variantCannotBeEditedUnderDifferentProduct() throws Exception {
        mvc.perform(admin(get("/admin/products/999999/variants/"+variant.getVariantId()+"/edit"))).andExpect(status().isNotFound()).andExpect(view().name("error/404"));
    }
    @Test void inventoryShowsReadOnlyQuantityAndInactiveVariant() throws Exception {
        mvc.perform(staff(get("/admin/inventory"))).andExpect(status().isOk()).andExpect(content().string(containsString("WEB-SKU")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("name=\"quantity\""))));
    }
    @Test void receiptCreateAddUpdateRemoveCompleteAndRepeatAreSafe() throws Exception {
        var result=mvc.perform(staff(post("/admin/purchase-receipts").with(csrf()).param("supplierId",supplier.getSupplierId().toString())))
                .andExpect(status().is3xxRedirection()).andReturn();
        String path=result.getResponse().getRedirectedUrl();
        long id=Long.parseLong(path.substring(path.lastIndexOf('/')+1));
        assertThat(receipts.findById(id).orElseThrow().getStatus()).isEqualTo(PurchaseReceiptStatus.DRAFT);
        mvc.perform(staff(get(path))).andExpect(status().isOk());
        mvc.perform(staff(post(path+"/items").with(csrf()).param("variantId",variant.getVariantId().toString()).param("quantity","3").param("unitPrice","100000")))
                .andExpect(redirectedUrl(path));
        mvc.perform(staff(post(path+"/items/"+variant.getVariantId()).with(csrf()).param("quantity","4").param("unitPrice","100000")))
                .andExpect(redirectedUrl(path));
        assertThat(variants.findById(variant.getVariantId()).orElseThrow().getQuantity()).isEqualTo(5);
        mvc.perform(staff(post(path+"/complete").with(csrf()))).andExpect(redirectedUrl(path)).andExpect(flash().attributeExists("successMessage"));
        assertThat(variants.findById(variant.getVariantId()).orElseThrow().getQuantity()).isEqualTo(9);
        mvc.perform(staff(post(path+"/complete").with(csrf()))).andExpect(redirectedUrl(path)).andExpect(flash().attributeExists("errorMessage"));
        assertThat(variants.findById(variant.getVariantId()).orElseThrow().getQuantity()).isEqualTo(9);
        assertThat(history.count()).isEqualTo(1);
        var html=mvc.perform(staff(get(path))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).doesNotContain("Complete receipt","name=\"quantity\"");
        mvc.perform(staff(get("/admin/inventory/transactions"))).andExpect(status().isOk()).andExpect(content().string(containsString("IMPORT")));
    }
    @Test void draftItemRemoveAndCancelNeverChangeStock() throws Exception {
        String path=receipt();
        receiptService.addItem(Long.parseLong(path.substring(path.lastIndexOf('/')+1)),new PurchaseReceiptDetailRequest(variant.getVariantId(),3,new BigDecimal("100")));
        mvc.perform(staff(post(path+"/items/"+variant.getVariantId()+"/remove").with(csrf()))).andExpect(redirectedUrl(path));
        mvc.perform(staff(post(path+"/cancel").with(csrf()))).andExpect(redirectedUrl(path));
        assertThat(variants.findById(variant.getVariantId()).orElseThrow().getQuantity()).isEqualTo(5);
        assertThat(history.count()).isZero();
        mvc.perform(staff(get(path))).andExpect(status().isOk());
    }
    @Test void invalidReceiptFormsRetainErrors() throws Exception {
        mvc.perform(staff(post("/admin/purchase-receipts").with(csrf()))).andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("form","supplierId"));
        String path=receipt();
        mvc.perform(staff(post(path+"/items").with(csrf()).param("quantity","-1").param("unitPrice","0")))
                .andExpect(status().isOk()).andExpect(view().name("admin/purchase-receipts/detail")).andExpect(model().attributeHasErrors("form"));
        mvc.perform(staff(post(path+"/complete").with(csrf()))).andExpect(flash().attributeExists("errorMessage"));
    }
    @Test void ordersRenderSnapshotsAndOnlyAllowedTransitions() throws Exception {
        Long id=checkout(true);
        mvc.perform(staff(get("/admin/orders").param("status","PENDING"))).andExpect(status().isOk()).andExpect(content().string(containsString("Web customer")));
        String html=mvc.perform(staff(get("/admin/orders/"+id))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("WEB10","12 Web Street","value=\"CONFIRMED\"","value=\"CANCELLED\"").doesNotContain("value=\"DELIVERED\"");
    }
    @Test void validAndInvalidOrderTransitionsDisplayFlash() throws Exception {
        Long id=checkout(false);String path="/admin/orders/"+id;
        mvc.perform(staff(post(path+"/status").with(csrf()).param("status","CONFIRMED"))).andExpect(redirectedUrl(path)).andExpect(flash().attributeExists("successMessage"));
        mvc.perform(staff(post(path+"/status").with(csrf()).param("status","DELIVERED"))).andExpect(redirectedUrl(path)).andExpect(flash().attributeExists("errorMessage"));
        assertThat(orders.findById(id).orElseThrow().getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }
    @Test void cancelOrderRestoresStockAndVoucherExactlyOnce() throws Exception {
        Long id=checkout(true);String path="/admin/orders/"+id;
        assertThat(variants.findById(variant.getVariantId()).orElseThrow().getQuantity()).isEqualTo(3);
        assertThat(vouchers.findById(voucher.getVoucherId()).orElseThrow().getQuantity()).isEqualTo(1);
        mvc.perform(staff(post(path+"/status").with(csrf()).param("status","CANCELLED"))).andExpect(redirectedUrl(path));
        mvc.perform(staff(post(path+"/status").with(csrf()).param("status","CANCELLED"))).andExpect(flash().attributeExists("errorMessage"));
        assertThat(variants.findById(variant.getVariantId()).orElseThrow().getQuantity()).isEqualTo(5);
        assertThat(vouchers.findById(voucher.getVoucherId()).orElseThrow().getQuantity()).isEqualTo(2);
        assertThat(history.count()).isEqualTo(2);
    }
    @Test void voucherCreateEditAndStatusWork() throws Exception {
        mvc.perform(voucherForm("/admin/vouchers","NEWCODE")).andExpect(redirectedUrl("/admin/vouchers"));
        mvc.perform(voucherForm(voucherPath(),"WEB10")).andExpect(redirectedUrl("/admin/vouchers"));
        mvc.perform(admin(post(voucherPath()+"/status").with(csrf()).param("status","INACTIVE"))).andExpect(redirectedUrl("/admin/vouchers"));
        var v=vouchers.findById(voucher.getVoucherId()).orElseThrow();assertThat(v.getStatus()).isEqualTo(ActiveStatus.INACTIVE);
        assertThat(v.getQuantity()).isEqualTo(3);assertThat(v.getDiscountValue()).isEqualByComparingTo("15");
    }
    @Test void duplicateVoucherAndInvalidRangeRenderForm() throws Exception {
        mvc.perform(voucherForm("/admin/vouchers","WEB10")).andExpect(status().isOk()).andExpect(model().attributeHasErrors("form"));
        mvc.perform(voucherForm(voucherPath(),"WEB10","150","2025-01-01T00:00"))
                .andExpect(status().isOk()).andExpect(model().attributeHasErrors("form"));
    }
    @Test void promotionCreateEditStatusAndProductAssociationsWork() throws Exception {
        mvc.perform(promotionForm("/admin/promotions","New campaign")).andExpect(redirectedUrl("/admin/promotions"));
        mvc.perform(promotionForm(promotionPath(),"Updated campaign")).andExpect(redirectedUrl("/admin/promotions"));
        mvc.perform(admin(post(promotionPath()+"/products").with(csrf()).param("productId",product.getProductId().toString())))
                .andExpect(redirectedUrl(promotionPath()+"/products"));
        mvc.perform(admin(get(promotionPath()+"/products"))).andExpect(status().isOk()).andExpect(content().string(containsString("Web shirt")));
        mvc.perform(admin(post(promotionPath()+"/products").with(csrf()).param("productId",product.getProductId().toString())))
                .andExpect(flash().attributeExists("errorMessage"));
        mvc.perform(admin(post(promotionPath()+"/products/"+product.getProductId()+"/remove").with(csrf()))).andExpect(redirectedUrl(promotionPath()+"/products"));
        assertThat(associations.count()).isZero();assertThat(products.existsById(product.getProductId())).isTrue();
        mvc.perform(admin(post(promotionPath()+"/status").with(csrf()).param("status","INACTIVE"))).andExpect(redirectedUrl("/admin/promotions"));
        assertThat(promotions.findById(promotion.getPromotionId()).orElseThrow().getStatus()).isEqualTo(ActiveStatus.INACTIVE);
    }
    @Test void customerListHandlesUnlinkedAccount() throws Exception {
        mvc.perform(staff(get("/admin/customers"))).andExpect(status().isOk()).andExpect(content().string(containsString("Walk-in customer")))
                .andExpect(content().string(containsString("No linked account")));
    }
    @Test void sizeColorSupplierFormsReuseServices() throws Exception {
        mvc.perform(staff(post("/admin/sizes").with(csrf()).param("sizeName","XL"))).andExpect(redirectedUrl("/admin/sizes"));
        mvc.perform(staff(post("/admin/colors").with(csrf()).param("colorName","Blue"))).andExpect(redirectedUrl("/admin/colors"));
        mvc.perform(staff(post("/admin/sizes/"+variant.getSize().getSizeId()).with(csrf()).param("sizeName","Medium"))).andExpect(redirectedUrl("/admin/sizes"));
        mvc.perform(staff(post("/admin/colors/"+variant.getColor().getColorId()).with(csrf()).param("colorName","Forest"))).andExpect(redirectedUrl("/admin/colors"));
        mvc.perform(staff(post("/admin/suppliers").with(csrf()).param("supplierName","New supplier").param("status","ACTIVE"))).andExpect(redirectedUrl("/admin/suppliers"));
        mvc.perform(staff(post("/admin/suppliers/"+supplier.getSupplierId()).with(csrf()).param("supplierName","Updated supplier").param("status","ACTIVE"))).andExpect(redirectedUrl("/admin/suppliers"));
        mvc.perform(staff(post("/admin/suppliers/"+supplier.getSupplierId()+"/status").with(csrf()).param("status","INACTIVE"))).andExpect(redirectedUrl("/admin/suppliers"));
        assertThat(suppliers.findById(supplier.getSupplierId()).orElseThrow().getStatus()).isEqualTo(ActiveStatus.INACTIVE);
    }
    @Test void errorsAreHtmlAndNotRestJson() throws Exception {
        mvc.perform(admin(get("/admin/products/999999/edit"))).andExpect(status().isNotFound()).andExpect(view().name("error/404"))
                .andExpect(content().contentTypeCompatibleWith("text/html"));
        mvc.perform(admin(get("/admin/products").param("page","-1"))).andExpect(status().isBadRequest()).andExpect(view().name("error/400"));
        mvc.perform(get("/403")).andExpect(status().isForbidden()).andExpect(view().name("error/403"));
        mvc.perform(get("/api/products/999999")).andExpect(status().isNotFound()).andExpect(content().contentTypeCompatibleWith("application/json"));
    }

    @Test void dashboardUsesActualCounts() throws Exception {
        checkout(false);
        receipt();
        var result=mvc.perform(staff(get("/admin"))).andExpect(status().isOk()).andReturn();
        var summary=(DashboardService.Summary)result.getModelAndView().getModel().get("summary");
        assertThat(summary.activeProducts()).isEqualTo(1);
        assertThat(summary.variants()).isEqualTo(1);
        assertThat(summary.pendingOrders()).isEqualTo(1);
        assertThat(summary.todayOrders()).isEqualTo(1);
        assertThat(summary.customers()).isEqualTo(2);
        assertThat(summary.draftReceipts()).isEqualTo(1);
        assertThat(summary.lowStock()).isEqualTo(1);
    }

    @Test void mappingErrorsKeepHtmlForWebAndJsonForApi() throws Exception {
        mvc.perform(admin(post("/admin").with(csrf()))).andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
        mvc.perform(get("/api/sizes/unknown")).andExpect(status().isUnauthorized());
        mvc.perform(admin(get("/admin/unknown"))).andExpect(status().isNotFound())
                .andExpect(view().name("error/404"));
        mvc.perform(get("/assets/unknown.css")).andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }

    @Test void promotionPriceRendersForInactiveCatalogWithoutMutatingBasePrice() throws Exception {
        tx.executeWithoutResult(s -> {
            var p=promotions.findById(promotion.getPromotionId()).orElseThrow();
            p.setStatus(ActiveStatus.ACTIVE);promotions.saveAndFlush(p);
            var link=new PromotionProduct();link.setId(new PromotionProductId(p.getPromotionId(),product.getProductId()));
            link.setPromotion(p);link.setProduct(products.findById(product.getProductId()).orElseThrow());associations.saveAndFlush(link);
        });
        mvc.perform(staff(get(productPath()+"/variants"))).andExpect(status().isOk())
                .andExpect(content().string(containsString("200.000")))
                .andExpect(content().string(containsString("250.000")));
        assertThat(variants.findById(variant.getVariantId()).orElseThrow().getPrice()).isEqualByComparingTo("250000");
    }

    @Test void voucherStatusDoesNotOverwriteConsumedQuantity() throws Exception {
        checkout(true);
        mvc.perform(admin(post(voucherPath()+"/status").with(csrf()).param("status","INACTIVE")))
                .andExpect(redirectedUrl("/admin/vouchers"));
        assertThat(vouchers.findById(voucher.getVoucherId()).orElseThrow().getQuantity()).isEqualTo(1);
    }

    @Test void orderViewReadsHistoricalPricesAfterCatalogChanges() throws Exception {
        Long id=checkout(true);
        tx.executeWithoutResult(s -> {
            var v=variants.findById(variant.getVariantId()).orElseThrow();v.setPrice(new BigDecimal("999999"));variants.saveAndFlush(v);
        });
        mvc.perform(staff(get("/admin/orders/"+id))).andExpect(status().isOk())
                .andExpect(content().string(containsString("250.000")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("999.999"))));
    }

    @Test void dateAndStatusFiltersSurviveOrderPagination() throws Exception {
        checkout(false);checkout(false);
        var result=mvc.perform(staff(get("/admin/orders").param("status","PENDING").param("size","1")
                .param("from",LocalDateTime.now().minusDays(1).withNano(0).toString())
                .param("to",LocalDateTime.now().plusDays(1).withNano(0).toString()))).andExpect(status().isOk()).andReturn();
        String next=(String)result.getModelAndView().getModel().get("nextPageUrl");
        assertThat(next).contains("status=PENDING","from=","to=","size=1","page=1");
    }

    @Test void paginationEncodesUnicodeAndSpecialCharactersOnce() throws Exception {
        tx.executeWithoutResult(s -> {
            for(String name:List.of("Áo & đẹp + sale","Áo & đẹp + sale 2")) {
                var p=new Product();p.setProductName(name);p.setPrice(BigDecimal.ONE);p.setStatus(ActiveStatus.ACTIVE);products.saveAndFlush(p);
            }
        });
        String keyword="Áo & đẹp + sale";
        var result=mvc.perform(admin(get("/admin/products").param("keyword",keyword).param("size","1")))
                .andExpect(status().isOk()).andReturn();
        String next=(String)result.getModelAndView().getModel().get("nextPageUrl");
        var query=org.springframework.web.util.UriComponentsBuilder.fromUriString(next).build().getQueryParams();
        assertThat(java.net.URLDecoder.decode(query.getFirst("keyword"),java.nio.charset.StandardCharsets.UTF_8)).isEqualTo(keyword);
        assertThat(next).contains("%26").doesNotContain("%2526");
    }
}
