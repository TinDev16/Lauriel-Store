package com.lauriel.mobile.data.api;

import static org.junit.Assert.*;

import com.google.gson.Gson;
import com.lauriel.mobile.data.model.Models;
import java.math.BigDecimal;
import java.util.concurrent.atomic.*;
import okhttp3.mockwebserver.*;
import org.junit.*;
import retrofit2.Response;

public class ApiClientTest {

    private MockWebServer server;

    @Before
    public void setup() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    @After
    public void close() throws Exception {
        server.shutdown();
    }

    private StoreApi api(String token, AtomicInteger expired) {
        return ApiClient.createApi(
            server.url("/").toString(),
            () -> token,
            expired::incrementAndGet
        );
    }

    @Test
    public void loginUsesCorrectEndpointAndBody() throws Exception {
        server.enqueue(
            new MockResponse().setBody(
                "{\"token\":\"jwt\",\"userId\":1,\"username\":\"u\",\"role\":\"CUSTOMER\"}"
            )
        );
        var r = api(null, new AtomicInteger())
            .login(new Models.LoginRequest("u", "secret"))
            .execute();
        assertEquals("jwt", r.body().token);
        var request = server.takeRequest();
        assertEquals("/api/auth/login", request.getPath());
        assertEquals("POST", request.getMethod());
        assertTrue(
            request.getBody().readUtf8().contains("\"password\":\"secret\"")
        );
    }

    @Test
    public void bearerIsAttachedToProtectedRequest() throws Exception {
        server.enqueue(
            new MockResponse().setBody(
                "{\"userId\":1,\"username\":\"u\",\"role\":\"CUSTOMER\"}"
            )
        );
        api("jwt-test", new AtomicInteger()).me().execute();
        assertEquals(
            "Bearer jwt-test",
            server.takeRequest().getHeader("Authorization")
        );
    }

    @Test
    public void publicCatalogDoesNotNeedToken() throws Exception {
        server.enqueue(
            new MockResponse().setBody(
                "{\"content\":[],\"page\":0,\"size\":12,\"totalPages\":0}"
            )
        );
        assertTrue(
            api(null, new AtomicInteger())
                .products(0, 12, null, null, null, null, "createdAt", "desc")
                .execute()
                .isSuccessful()
        );
        assertNull(server.takeRequest().getHeader("Authorization"));
    }

    @Test
    public void categoriesAndProductCategoryAreParsed() throws Exception {
        server.enqueue(
            new MockResponse().setBody(
                "[{\"categoryId\":7,\"categoryName\":\"Ão\"}]"
            )
        );
        var categories = api(null, new AtomicInteger())
            .categories()
            .execute()
            .body();
        assertEquals(7, categories.get(0).categoryId);
        assertEquals("Ão", categories.get(0).categoryName);
        assertEquals("/api/categories", server.takeRequest().getPath());
        server.enqueue(
            new MockResponse().setBody(
                "{\"content\":[{\"productId\":1,\"category\":{\"categoryId\":7,\"categoryName\":\"Ão\"}}],\"page\":0,\"totalPages\":1}"
            )
        );
        var products = api(null, new AtomicInteger())
            .products(0, 12, null, 7L, null, null, "createdAt", "desc")
            .execute()
            .body();
        assertEquals("Ão", products.content.get(0).category.categoryName);
        assertTrue(server.takeRequest().getPath().contains("categoryId=7"));
    }

    @Test
    public void unauthorizedCallsCentralHandler() throws Exception {
        AtomicInteger expired = new AtomicInteger();
        server.enqueue(new MockResponse().setResponseCode(401));
        api("old-token", expired).me().execute();
        assertEquals(1, expired.get());
    }

    @Test
    public void unauthorizedClearsTokenSupplier() throws Exception {
        AtomicReference<String> token = new AtomicReference<>("jwt");
        server.enqueue(new MockResponse().setResponseCode(401));
        ApiClient.createApi(server.url("/").toString(), token::get, () ->
            token.set(null)
        )
            .me()
            .execute();
        assertNull(token.get());
    }

    @Test
    public void public401DoesNotExpireSession() throws Exception {
        AtomicInteger expired = new AtomicInteger();
        server.enqueue(new MockResponse().setResponseCode(401));
        api(null, expired).me().execute();
        assertEquals(0, expired.get());
    }

    @Test
    public void voucherPreviewIsServerCallWithItemsOnly() throws Exception {
        server.enqueue(
            new MockResponse().setBody(
                "{\"valid\":true,\"code\":\"WEB10\",\"orderAmount\":\"250000\",\"discountAmount\":\"25000\",\"shippingFee\":\"20000\",\"finalAmount\":\"245000\"}"
            )
        );
        var r = api("jwt", new AtomicInteger())
            .preview(
                new Models.VoucherRequest(
                    "WEB10",
                    java.util.Collections.singletonList(new Models.Item(15, 2))
                )
            )
            .execute();
        assertEquals(new BigDecimal("20000"), r.body().shippingFee);
        assertEquals(new BigDecimal("245000"), r.body().finalAmount);
        var request = server.takeRequest();
        assertEquals("/api/vouchers/validate", request.getPath());
        assertEquals(
            "{\"code\":\"WEB10\",\"items\":[{\"variantId\":15,\"quantity\":2}]}",
            request.getBody().readUtf8()
        );
    }

    @Test
    public void pricingPreviewParsesAllBackendDiscountLayers()
        throws Exception {
        String json =
            "{\"valid\":true,\"orderAmount\":\"1200000\",\"subtotal\":\"1200000\"," +
            "\"productPromotionDiscount\":\"100000\",\"orderPromotionName\":\"Đơn lớn\"," +
            "\"orderPromotionDiscount\":\"50000\",\"discountAmount\":\"0\"," +
            "\"shippingFee\":\"20000\",\"finalAmount\":\"1170000\"}";
        Models.VoucherPreview value = new Gson().fromJson(
            json,
            Models.VoucherPreview.class
        );
        assertEquals(new BigDecimal("100000"), value.productPromotionDiscount);
        assertEquals(new BigDecimal("50000"), value.orderPromotionDiscount);
        assertEquals(new BigDecimal("1170000"), value.finalAmount);
    }

    @Test
    public void checkoutNeverSendsLocalPricesOrOwner() throws Exception {
        server.enqueue(
            new MockResponse()
                .setResponseCode(201)
                .setBody("{\"orderId\":42,\"items\":[]}")
        );
        var result = api("jwt", new AtomicInteger())
            .checkout(
                new Models.CheckoutRequest(
                    java.util.Collections.singletonList(new Models.Item(15, 2)),
                    "12 Main St",
                    "WEB10"
                )
            )
            .execute();
        assertEquals(42, result.body().orderId);
        String body = server.takeRequest().getBody().readUtf8();
        assertTrue(body.contains("\"shippingAddress\":\"12 Main St\""));
        assertFalse(body.contains("price"));
        assertFalse(body.contains("userId"));
        assertFalse(body.contains("customerId"));
    }

    @Test
    public void orderListParsesIsoAndAmounts() throws Exception {
        server.enqueue(
            new MockResponse().setBody(
                "{\"content\":[{\"orderId\":5,\"status\":\"PENDING\",\"createdAt\":\"2026-09-18T10:30:00\",\"finalAmount\":\"1000.50\"}],\"page\":0,\"totalPages\":1}"
            )
        );
        var r = api("jwt", new AtomicInteger()).myOrders(0, 12, null).execute();
        assertEquals("2026-09-18T10:30:00", r.body().content.get(0).createdAt);
        assertEquals(
            new BigDecimal("1000.50"),
            r.body().content.get(0).finalAmount
        );
    }

    @Test
    public void orderDetailParsesPriceSnapshot() throws Exception {
        server.enqueue(
            new MockResponse().setBody(
                "{\"orderId\":5,\"status\":\"CONFIRMED\",\"items\":[{\"variantId\":1,\"unitPrice\":\"250000\",\"discountAmount\":\"25000\",\"subtotal\":\"450000\"}]}"
            )
        );
        var order = api("jwt", new AtomicInteger()).order(5).execute().body();
        assertEquals(
            new BigDecimal("25000"),
            order.items.get(0).discountAmount
        );
        assertEquals("/api/orders/5", server.takeRequest().getPath());
    }

    @Test
    public void cancelCallsCorrectEndpoint() throws Exception {
        server.enqueue(
            new MockResponse().setBody(
                "{\"orderId\":5,\"status\":\"CANCELLED\"}"
            )
        );
        var r = api("jwt", new AtomicInteger()).cancel(5).execute();
        assertEquals("CANCELLED", r.body().status);
        var request = server.takeRequest();
        assertEquals("/api/orders/5/cancel", request.getPath());
        assertEquals("POST", request.getMethod());
    }

    @Test
    public void conflictIsNotTreatedAsSuccessfulCancel() throws Exception {
        server.enqueue(
            new MockResponse()
                .setResponseCode(409)
                .setBody("{\"status\":409,\"message\":\"Order changed\"}")
        );
        assertFalse(
            api("jwt", new AtomicInteger()).cancel(5).execute().isSuccessful()
        );
    }

    @Test
    public void imagePathsResolveAgainstBackendAndKeepAbsoluteUrls() {
        String base = com.lauriel.mobile.BuildConfig.API_BASE_URL;
        String expected = okhttp3.HttpUrl.parse(base)
            .resolve("/uploads/products/test.png")
            .toString();
        assertEquals(
            expected,
            ApiClient.imageUrl("/uploads/products/test.png")
        );
        assertEquals(
            "https://cdn.example.com/photo.jpg",
            ApiClient.imageUrl("https://cdn.example.com/photo.jpg")
        );
        assertNull(ApiClient.imageUrl("  "));
    }

    @Test
    public void promotionEffectivePriceKeepsBasePrice() {
        var variant = new Gson().fromJson(
            "{\"price\":\"250000\",\"effectivePrice\":\"200000\",\"promotion\":{\"discountPercent\":20}}",
            Models.Variant.class
        );
        assertEquals(new BigDecimal("250000"), variant.price);
        assertEquals(new BigDecimal("200000"), variant.effectivePrice);
    }

    @Test
    public void productGalleryRelatedProfileAndOrderStatusContracts()
        throws Exception {
        server.enqueue(
            new MockResponse().setBody(
                "{\"productId\":1,\"imgUrl\":\"/p.jpg\",\"images\":[{\"id\":9,\"url\":\"/p.jpg\",\"primary\":true,\"order\":0}]}"
            )
        );
        var product = api(null, new AtomicInteger())
            .product(1)
            .execute()
            .body();
        assertTrue(product.images.get(0).primary);
        assertEquals(9, product.images.get(0).id);
        assertEquals("/api/products/1", server.takeRequest().getPath());

        server.enqueue(new MockResponse().setBody("[]"));
        api(null, new AtomicInteger()).related(1, 8).execute();
        assertEquals(
            "/api/products/1/related?limit=8",
            server.takeRequest().getPath()
        );

        server.enqueue(
            new MockResponse().setBody(
                "{\"customerId\":2,\"userId\":3,\"username\":\"an\",\"fullName\":\"Nguyen An\",\"address\":\"Hue\"}"
            )
        );
        var profile = api("jwt", new AtomicInteger())
            .profile()
            .execute()
            .body();
        assertEquals("Hue", profile.address);
        assertEquals("/api/customers/me", server.takeRequest().getPath());

        server.enqueue(
            new MockResponse().setBody(
                "{\"content\":[],\"page\":0,\"totalPages\":0}"
            )
        );
        api("jwt", new AtomicInteger()).myOrders(0, 12, "SHIPPING").execute();
        assertTrue(server.takeRequest().getPath().contains("status=SHIPPING"));
    }
}
