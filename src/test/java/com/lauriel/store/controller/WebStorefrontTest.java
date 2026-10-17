package com.lauriel.store.controller;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.lauriel.store.support.TestSchema;
import com.lauriel.store.support.TestSecrets;
import com.lauriel.store.repository.UserRepository;
import com.lauriel.store.security.StoreUserDetails;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class WebStorefrontTest {
    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;
    private MockMvc mvc;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry properties) throws Exception {
        String url = TestSchema.createIsolatedDatabase();
        properties.add("spring.datasource.url", () -> url);
        properties.add("spring.datasource.username", () -> "sa");
        properties.add("spring.datasource.password", () -> "");
        properties.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        properties.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        properties.add("app.jwt.secret", TestSecrets::randomBase64);
        properties.add("telegram.enabled", () -> "false");
    }

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        jdbc.execute("DELETE FROM inventory_transactions");
        jdbc.execute("DELETE FROM order_details");
        jdbc.execute("DELETE FROM orders");
        jdbc.execute("DELETE FROM promotion_variants");
        jdbc.execute("DELETE FROM promotion_products");
        jdbc.execute("DELETE FROM promotions");
        jdbc.execute("DELETE FROM order_promotions");
        jdbc.execute("DELETE FROM vouchers");
        jdbc.execute("DELETE FROM product_images");
        jdbc.execute("DELETE FROM product_variants");
        jdbc.execute("DELETE FROM products");
        jdbc.execute("DELETE FROM categories");
        jdbc.execute("DELETE FROM sizes");
        jdbc.execute("DELETE FROM colors");
        jdbc.execute("DELETE FROM customers");
        jdbc.execute("DELETE FROM users");
        jdbc.update("INSERT INTO categories(category_id,category_name,status) VALUES(101,'Áo','ACTIVE')");
        jdbc.update("INSERT INTO products(product_id,category_id,product_name,description,price,img_url,status) VALUES(101,101,'Áo Lauriel','Thiết kế tối giản',250000,'/images/ao.jpg','ACTIVE')");
        jdbc.update("INSERT INTO sizes(size_id,size_name) VALUES(101,'M')");
        jdbc.update("INSERT INTO colors(color_id,color_name) VALUES(101,'Đen')");
        jdbc.update("INSERT INTO product_variants(variant_id,product_id,size_id,color_id,sku,price,quantity,status) VALUES(201,101,101,101,'AO-M-DEN',250000,5,'ACTIVE')");
        jdbc.update("INSERT INTO product_images(product_image_id,product_id,image_url,is_primary,display_order) VALUES(301,101,'/images/ao-detail.jpg',true,0)");
        createCustomer(901L, "webcustomer");
    }

    @Test
    void publicCatalogAndDetailRenderWithoutAuthentication() throws Exception {
        mvc.perform(get("/")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/shop"));
        mvc.perform(get("/shop")).andExpect(status().isOk()).andExpect(content().string(containsString("LAURIEL")));
        mvc.perform(get("/shop/products")).andExpect(status().isOk()).andExpect(content().string(containsString("Áo Lauriel")));
        mvc.perform(get("/shop/products/101")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("AO-M-DEN"))))
                .andExpect(content().string(containsString("Tồn kho")));
        mvc.perform(get("/register")).andExpect(status().isOk());
        mvc.perform(post("/register").with(csrf()).param("username", "x").param("password", "short")
                        .param("fullName", ""))
                .andExpect(status().isOk()).andExpect(view().name("auth/register"));
    }

    @Test
    void featuredProductsAreOrderedByDeliveredQuantity() throws Exception {
        jdbc.update("INSERT INTO products(product_id,category_id,product_name,description,price,img_url,status) "
                + "VALUES(102,101,'Áo bán chạy','Sản phẩm bán chạy',300000,'/images/best-seller.jpg','ACTIVE')");
        jdbc.update("INSERT INTO product_variants(variant_id,product_id,size_id,color_id,sku,price,quantity,status) "
                + "VALUES(202,102,101,101,'BEST-M-DEN',300000,20,'ACTIVE')");
        jdbc.update("INSERT INTO orders(order_id,customer_id,total_amount,discount_amount,order_promotion_discount,"
                + "shipping_fee,final_amount,status,shipping_address) "
                + "VALUES(701,901,1000000,0,0,0,1000000,'DELIVERED','Hà Nội'),"
                + "(702,901,5000000,0,0,0,5000000,'CANCELLED','Hà Nội')");
        jdbc.update("INSERT INTO order_details(order_id,variant_id,quantity,unit_price,discount_amount,subtotal) "
                + "VALUES(701,202,8,300000,0,2400000),(701,201,2,250000,0,500000),"
                + "(702,201,20,250000,0,5000000)");

        mvc.perform(get("/api/products/featured").param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productId").value(102))
                .andExpect(jsonPath("$[1].productId").value(101));

        String home = mvc.perform(get("/shop")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(home.indexOf("Áo bán chạy"))
                .isLessThan(home.indexOf("Áo Lauriel"));
    }

    @Test
    void sessionCartRequiresCsrfAndMergesTheSameVariant() throws Exception {
        jdbc.update("INSERT INTO sizes(size_id,size_name) VALUES(102,'L')");
        jdbc.update("INSERT INTO product_variants(variant_id,product_id,size_id,color_id,sku,price,quantity,status) VALUES(202,101,102,101,'AO-L-DEN',260000,2,'ACTIVE')");
        mvc.perform(post("/shop/cart/add").param("productId", "101").param("variantId", "201").param("quantity", "1"))
                .andExpect(status().isForbidden());
        var result = mvc.perform(post("/shop/cart/add").with(csrf()).param("productId", "101")
                        .param("variantId", "201").param("quantity", "2"))
                .andExpect(status().is3xxRedirection()).andReturn();
        var session = result.getRequest().getSession(false);
        mvc.perform(post("/shop/cart/add").session((org.springframework.mock.web.MockHttpSession) session).with(csrf())
                        .param("productId", "101").param("variantId", "201").param("quantity", "1"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/shop/cart").session((org.springframework.mock.web.MockHttpSession) session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"3\"")))
                .andExpect(content().string(containsString("data-cart-auto-update")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString(">Cập nhật</button>"))));
        mvc.perform(post("/shop/cart/update").session((MockHttpSession) session).with(csrf())
                        .param("variantId", "201").param("quantity", "4"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/shop/cart").session((MockHttpSession) session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"4\"")))
                .andExpect(content().string(containsString("1.000.000")));
        mvc.perform(post("/shop/cart/add").session((MockHttpSession) session).with(csrf())
                        .param("productId", "101").param("variantId", "202").param("quantity", "1"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/shop/cart/update").session((MockHttpSession) session).with(csrf())
                        .param("variantId", "201").param("quantity", "6"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/shop/cart").session((MockHttpSession) session))
                .andExpect(status().isOk()).andExpect(content().string(containsString("L · Đen")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("AO-L-DEN"))))
                .andExpect(content().string(containsString("value=\"4\"")));
        mvc.perform(post("/shop/cart/buy-now").session((MockHttpSession) session).with(csrf())
                        .param("productId", "101").param("variantId", "202").param("quantity", "2"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/shop/checkout?mode=buy-now"));
        mvc.perform(post("/shop/cart/remove").session((MockHttpSession) session).with(csrf()).param("variantId", "202"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/shop/cart").session((MockHttpSession) session))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.not(containsString("L · Đen"))));
        mvc.perform(post("/logout").session((MockHttpSession) session).with(user(customer("webcustomer"))).with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?logout"));
        mvc.perform(get("/shop/cart").session((MockHttpSession) session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Áo Lauriel")))
                .andExpect(content().string(containsString("value=\"4\"")));
    }

    @Test
    void switchingAccountsOnTheSameBrowserKeepsTheirCartsSeparate() throws Exception {
        jdbc.update("INSERT INTO sizes(size_id,size_name) VALUES(102,'L')");
        jdbc.update("INSERT INTO product_variants(variant_id,product_id,size_id,color_id,sku,price,quantity,status) "
                + "VALUES(202,101,102,101,'AO-L-DEN',260000,5,'ACTIVE')");
        createCustomer(902L, "customer-b");
        jdbc.update("UPDATE users SET password_hash=? WHERE username='webcustomer'", passwordEncoder.encode("AccountA@123"));
        jdbc.update("UPDATE users SET password_hash=? WHERE username='customer-b'", passwordEncoder.encode("AccountB@123"));

        var loginA = mvc.perform(post("/login").with(csrf()).param("username", "webcustomer")
                        .param("password", "AccountA@123"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/shop")).andReturn();
        var session = (MockHttpSession) loginA.getRequest().getSession(false);
        mvc.perform(post("/shop/cart/add").session(session).with(csrf()).param("productId", "101")
                        .param("variantId", "201").param("quantity", "1"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/logout").session(session).with(csrf())).andExpect(status().is3xxRedirection());

        mvc.perform(post("/login").session(session).with(csrf()).param("username", "customer-b")
                        .param("password", "AccountB@123"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/shop"));
        mvc.perform(get("/shop/cart").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Áo Lauriel"))));
        mvc.perform(post("/shop/cart/add").session(session).with(csrf()).param("productId", "101")
                        .param("variantId", "202").param("quantity", "1"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/logout").session(session).with(csrf())).andExpect(status().is3xxRedirection());

        mvc.perform(post("/login").session(session).with(csrf()).param("username", "webcustomer")
                        .param("password", "AccountA@123"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/shop/cart").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("M · Đen")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("L · Đen"))));
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT variant_id FROM customer_cart_items WHERE user_id=901", Long.class)).isEqualTo(201L);
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT variant_id FROM customer_cart_items WHERE user_id=902", Long.class)).isEqualTo(202L);
    }

    @Test
    void protectedCustomerPagesAndAdminBoundaryRemainEnforced() throws Exception {
        var checkout = mvc.perform(get("/shop/checkout")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?checkout")).andReturn();
        mvc.perform(get("/login").param("checkout", "").session(
                        (MockHttpSession) checkout.getRequest().getSession(false)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Vui lòng đăng nhập để tiếp tục thanh toán.")));
        mvc.perform(get("/shop/orders")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/shop/profile")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin").with(user("customer").roles("CUSTOMER"))).andExpect(status().isForbidden());
    }

    @Test
    void checkoutUsesSharedPricingAndCreatesOwnedOrderThatCanBeCancelled() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("INSERT INTO promotions(promotion_id,promotion_name,discount_percent,start_date,end_date,status) VALUES(301,'Giảm sản phẩm',10,?,?,'ACTIVE')",
                now.minusDays(1), now.plusDays(1));
        jdbc.update("INSERT INTO promotion_products(promotion_id,product_id) VALUES(301,101)");
        jdbc.update("INSERT INTO order_promotions(order_promotion_id,promotion_name,min_order_amount,discount_percent,max_discount_amount,start_date,end_date,status) VALUES(401,'Giảm đơn',0,10,50000,?,?,'ACTIVE')",
                now.minusDays(1), now.plusDays(1));
        jdbc.update("INSERT INTO vouchers(voucher_id,code,voucher_name,discount_type,discount_value,min_order_amount,quantity,start_date,end_date,status) VALUES(501,'SINHNHAT','Voucher sinh nhật','FIXED',10000,0,3,?,?,'ACTIVE')",
                now.minusDays(1), now.plusDays(1));

        mvc.perform(get("/shop"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/shop/offers/products/301")))
                .andExpect(content().string(containsString("/shop/offers/orders/401")))
                .andExpect(content().string(containsString("/shop/offers/vouchers/501")));
        mvc.perform(get("/shop/offers/products/301"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Giảm sản phẩm")))
                .andExpect(content().string(containsString("Áo Lauriel")));
        mvc.perform(get("/shop/offers/orders/401"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ƯU ĐÃI HÓA ĐƠN")));
        mvc.perform(get("/shop/offers/vouchers/501"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Voucher sinh nhật")))
                .andExpect(content().string(containsString("SINHNHAT")));

        var add = mvc.perform(post("/shop/cart/add").with(csrf()).param("productId", "101")
                        .param("variantId", "201").param("quantity", "1"))
                .andExpect(status().is3xxRedirection()).andReturn();
        var session = (MockHttpSession) add.getRequest().getSession(false);
        var customer = customer("webcustomer");
        mvc.perform(post("/shop/checkout/voucher").session(session).with(user(customer)).with(csrf())
                        .param("code", "SINHNHAT").param("mode", "cart"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/shop/checkout").session(session).with(user(customer)))
                .andExpect(status().isOk()).andExpect(content().string(containsString("212.500")))
                .andExpect(content().string(containsString("250.000")))
                .andExpect(content().string(containsString("225.000")))
                .andExpect(content().string(containsString("Giảm sản phẩm")))
                .andExpect(content().string(containsString("Thông tin người nhận")))
                .andExpect(content().string(containsString("Tổng cộng voucher giảm giá")))
                .andExpect(content().string(containsString("Voucher sinh nhật")))
                .andExpect(content().string(containsString("SINHNHAT")));

        var placed = mvc.perform(post("/shop/checkout").session(session).with(user(customer)).with(csrf())
                        .param("shippingAddress", "12 Nguyễn Huệ, TP.HCM").param("mode", "cart"))
                .andExpect(status().is3xxRedirection()).andReturn();
        String location = placed.getResponse().getRedirectedUrl();
        long orderId = Long.parseLong(location.replaceAll("\\D+", ""));
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT final_amount FROM orders WHERE order_id=?", BigDecimal.class, orderId))
                .isEqualByComparingTo("212500.00");
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT sales_channel FROM orders WHERE order_id=?", String.class, orderId))
                .isEqualTo("WEB");
        mvc.perform(get(location).session(session).with(user(customer)))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Đặt hàng thành công")));

        createCustomer(902L, "othercustomer");
        mvc.perform(get("/shop/orders/" + orderId).with(user(customer("othercustomer"))))
                .andExpect(status().isNotFound());
        mvc.perform(post("/shop/orders/" + orderId + "/cancel").with(user(customer)).with(csrf()))
                .andExpect(status().is3xxRedirection());
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT status FROM orders WHERE order_id=?", String.class, orderId)).isEqualTo("CANCELLED");
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT quantity FROM product_variants WHERE variant_id=201", Integer.class)).isEqualTo(5);
    }

    @Test
    void checkoutLetsCustomerSaveAndReuseAnAdditionalAddress() throws Exception {
        var added = mvc.perform(post("/shop/cart/add").with(csrf()).param("productId", "101")
                        .param("variantId", "201").param("quantity", "1"))
                .andExpect(status().is3xxRedirection()).andReturn();
        var session = (MockHttpSession) added.getRequest().getSession(false);
        var customer = customer("webcustomer");

        mvc.perform(get("/shop/checkout").session(session).with(user(customer)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Địa chỉ mặc định")))
                .andExpect(content().string(containsString("Bạn muốn lưu thêm địa chỉ mới này?")));
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM customer_addresses WHERE customer_id=901", Long.class)).isEqualTo(1L);

        mvc.perform(post("/shop/checkout").session(session).with(user(customer)).with(csrf())
                        .param("shippingAddress", "12 Nguyễn Huệ, TP.HCM")
                        .param("saveAddress", "true").param("mode", "cart"))
                .andExpect(status().is3xxRedirection());
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM customer_addresses WHERE customer_id=901", Long.class)).isEqualTo(2L);
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM customer_addresses WHERE customer_id=901 AND is_default=true", Long.class))
                .isEqualTo(1L);
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT address FROM customers WHERE customer_id=901", String.class)).isEqualTo("Địa chỉ mặc định");

        mvc.perform(post("/shop/cart/add").session(session).with(csrf()).param("productId", "101")
                        .param("variantId", "201").param("quantity", "1"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/shop/checkout").session(session).with(user(customer)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Địa chỉ mặc định")))
                .andExpect(content().string(containsString("12 Nguyễn Huệ, TP.HCM")));
    }

    @Test
    void checkoutConfirmsAndSavesAddressImmediatelyBeforeOrderPlacement() throws Exception {
        var added = mvc.perform(post("/shop/cart/add").with(csrf()).param("productId", "101")
                        .param("variantId", "201").param("quantity", "1"))
                .andExpect(status().is3xxRedirection()).andReturn();
        var session = (MockHttpSession) added.getRequest().getSession(false);
        var customer = customer("webcustomer");
        String temporaryAddress = "88 Võ Văn Tần, Quận 3, TP.HCM";

        mvc.perform(post("/shop/checkout/address/confirm").session(session).with(user(customer)).with(csrf())
                        .param("shippingAddress", temporaryAddress))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").value(temporaryAddress))
                .andExpect(jsonPath("$.saved").value(false));

        mvc.perform(get("/shop/checkout").session(session).with(user(customer)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(temporaryAddress)))
                .andExpect(content().string(containsString("data-initial-confirmed=\"true\"")));
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM customer_addresses WHERE customer_id=901 AND address_text=?",
                Long.class, temporaryAddress)).isZero();

        mvc.perform(post("/shop/checkout/address/confirm").session(session).with(user(customer)).with(csrf())
                        .param("shippingAddress", temporaryAddress).param("saveAddress", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(true))
                .andExpect(jsonPath("$.addresses[*].address").value(org.hamcrest.Matchers.hasItem(temporaryAddress)));

        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM customer_addresses WHERE customer_id=901 AND address_text=?",
                Long.class, temporaryAddress)).isEqualTo(1L);
    }

    @Test
    void firstCheckoutOffersToSaveTheFirstAddressAsDefault() throws Exception {
        jdbc.update("UPDATE customers SET address=NULL WHERE customer_id=901");
        var added = mvc.perform(post("/shop/cart/add").with(csrf()).param("productId", "101")
                        .param("variantId", "201").param("quantity", "1"))
                .andExpect(status().is3xxRedirection()).andReturn();
        var session = (MockHttpSession) added.getRequest().getSession(false);
        var customer = customer("webcustomer");

        mvc.perform(get("/shop/checkout").session(session).with(user(customer)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bạn muốn lưu địa chỉ đầu tiên này?")));
        mvc.perform(post("/shop/checkout").session(session).with(user(customer)).with(csrf())
                        .param("shippingAddress", "25 Lê Lợi, Quận 1, TP.HCM")
                        .param("saveAddress", "true").param("mode", "cart"))
                .andExpect(status().is3xxRedirection());

        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM customer_addresses WHERE customer_id=901 AND is_default=true", Long.class))
                .isEqualTo(1L);
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT address FROM customers WHERE customer_id=901", String.class))
                .isEqualTo("25 Lê Lợi, Quận 1, TP.HCM");
    }

    @Test
    void customerCanViewAndUpdateOnlySafeProfileFields() throws Exception {
        var customer = customer("webcustomer");
        mvc.perform(get("/shop/profile").with(user(customer)))
                .andExpect(status().isOk()).andExpect(content().string(containsString("webcustomer")));
        mvc.perform(post("/shop/profile/edit").with(user(customer)).with(csrf())
                        .param("fullName", "Khách hàng Web").param("email", "web@example.com")
                        .param("phone", "0900000000").param("address", "Hà Nội"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/shop/profile"));
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT full_name FROM users WHERE user_id=901", String.class)).isEqualTo("Khách hàng Web");
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT address FROM customers WHERE user_id=901", String.class)).isEqualTo("Hà Nội");
    }

    private void createCustomer(long id, String username) {
        Long roleId = jdbc.queryForObject("SELECT role_id FROM roles WHERE role_name='CUSTOMER'", Long.class);
        jdbc.update("INSERT INTO users(user_id,role_id,username,password_hash,full_name,status) VALUES(?,?,?,'unused','Khách hàng','ACTIVE')",
                id, roleId, username);
        jdbc.update("INSERT INTO customers(customer_id,user_id,customer_name,address) VALUES(?,?, 'Khách hàng','Địa chỉ mặc định')",
                id, id);
    }

    private StoreUserDetails customer(String username) {
        return new StoreUserDetails(users.findByUsername(username).orElseThrow());
    }
}
