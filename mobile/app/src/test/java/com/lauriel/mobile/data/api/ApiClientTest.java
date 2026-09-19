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
    @Before public void setup() throws Exception {server=new MockWebServer();server.start();}
    @After public void close() throws Exception {server.shutdown();}
    private StoreApi api(String token,AtomicInteger expired){
        return ApiClient.createApi(server.url("/").toString(),()->token,expired::incrementAndGet);
    }
    @Test public void loginUsesCorrectEndpointAndBody() throws Exception {
        server.enqueue(new MockResponse().setBody("{\"token\":\"jwt\",\"userId\":1,\"username\":\"u\",\"role\":\"CUSTOMER\"}"));
        var r=api(null,new AtomicInteger()).login(new Models.LoginRequest("u","secret")).execute();
        assertEquals("jwt",r.body().token);var request=server.takeRequest();
        assertEquals("/api/auth/login",request.getPath());assertEquals("POST",request.getMethod());
        assertTrue(request.getBody().readUtf8().contains("\"password\":\"secret\""));
    }
    @Test public void bearerIsAttachedToProtectedRequest() throws Exception {
        server.enqueue(new MockResponse().setBody("{\"userId\":1,\"username\":\"u\",\"role\":\"CUSTOMER\"}"));
        api("jwt-test",new AtomicInteger()).me().execute();
        assertEquals("Bearer jwt-test",server.takeRequest().getHeader("Authorization"));
    }
    @Test public void publicCatalogDoesNotNeedToken() throws Exception {
        server.enqueue(new MockResponse().setBody("{\"content\":[],\"page\":0,\"size\":12,\"totalPages\":0}"));
        assertTrue(api(null,new AtomicInteger()).products(0,12,null,null,null,"createdAt","desc").execute().isSuccessful());
        assertNull(server.takeRequest().getHeader("Authorization"));
    }
    @Test public void unauthorizedCallsCentralHandler() throws Exception {
        AtomicInteger expired=new AtomicInteger();server.enqueue(new MockResponse().setResponseCode(401));
        api("old-token",expired).me().execute();assertEquals(1,expired.get());
    }
    @Test public void unauthorizedClearsTokenSupplier() throws Exception {
        AtomicReference<String> token=new AtomicReference<>("jwt");
        server.enqueue(new MockResponse().setResponseCode(401));
        ApiClient.createApi(server.url("/").toString(),token::get,()->token.set(null)).me().execute();
        assertNull(token.get());
    }
    @Test public void public401DoesNotExpireSession() throws Exception {
        AtomicInteger expired=new AtomicInteger();server.enqueue(new MockResponse().setResponseCode(401));
        api(null,expired).me().execute();assertEquals(0,expired.get());
    }
    @Test public void voucherPreviewIsServerCallWithItemsOnly() throws Exception {
        server.enqueue(new MockResponse().setBody("{\"valid\":true,\"code\":\"WEB10\",\"orderAmount\":\"250000\",\"discountAmount\":\"25000\",\"finalAmount\":\"225000\"}"));
        var r=api("jwt",new AtomicInteger()).preview(new Models.VoucherRequest("WEB10",
                java.util.Collections.singletonList(new Models.Item(15,2)))).execute();
        assertEquals(new BigDecimal("225000"),r.body().finalAmount);
        var request=server.takeRequest();assertEquals("/api/vouchers/validate",request.getPath());
        assertEquals("{\"code\":\"WEB10\",\"items\":[{\"variantId\":15,\"quantity\":2}]}",request.getBody().readUtf8());
    }
    @Test public void checkoutNeverSendsLocalPricesOrOwner() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(201).setBody("{\"orderId\":42,\"items\":[]}"));
        var result=api("jwt",new AtomicInteger()).checkout(new Models.CheckoutRequest(
                java.util.Collections.singletonList(new Models.Item(15,2)),"12 Main St","WEB10")).execute();
        assertEquals(42,result.body().orderId);
        String body=server.takeRequest().getBody().readUtf8();
        assertTrue(body.contains("\"shippingAddress\":\"12 Main St\""));
        assertFalse(body.contains("price"));assertFalse(body.contains("userId"));assertFalse(body.contains("customerId"));
    }
    @Test public void orderListParsesIsoAndAmounts() throws Exception {
        server.enqueue(new MockResponse().setBody("{\"content\":[{\"orderId\":5,\"status\":\"PENDING\",\"createdAt\":\"2026-09-18T10:30:00\",\"finalAmount\":\"1000.50\"}],\"page\":0,\"totalPages\":1}"));
        var r=api("jwt",new AtomicInteger()).myOrders(0,12).execute();
        assertEquals("2026-09-18T10:30:00",r.body().content.get(0).createdAt);
        assertEquals(new BigDecimal("1000.50"),r.body().content.get(0).finalAmount);
    }
    @Test public void orderDetailParsesPriceSnapshot() throws Exception {
        server.enqueue(new MockResponse().setBody("{\"orderId\":5,\"status\":\"CONFIRMED\",\"items\":[{\"variantId\":1,\"unitPrice\":\"250000\",\"discountAmount\":\"25000\",\"subtotal\":\"450000\"}]}"));
        var order=api("jwt",new AtomicInteger()).order(5).execute().body();
        assertEquals(new BigDecimal("25000"),order.items.get(0).discountAmount);
        assertEquals("/api/orders/5",server.takeRequest().getPath());
    }
    @Test public void cancelCallsCorrectEndpoint() throws Exception {
        server.enqueue(new MockResponse().setBody("{\"orderId\":5,\"status\":\"CANCELLED\"}"));
        var r=api("jwt",new AtomicInteger()).cancel(5).execute();
        assertEquals("CANCELLED",r.body().status);
        var request=server.takeRequest();assertEquals("/api/orders/5/cancel",request.getPath());assertEquals("POST",request.getMethod());
    }
    @Test public void conflictIsNotTreatedAsSuccessfulCancel() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(409).setBody("{\"status\":409,\"message\":\"Order changed\"}"));
        assertFalse(api("jwt",new AtomicInteger()).cancel(5).execute().isSuccessful());
    }
    @Test public void promotionEffectivePriceKeepsBasePrice() {
        var variant=new Gson().fromJson("{\"price\":\"250000\",\"effectivePrice\":\"200000\",\"promotion\":{\"discountPercent\":20}}",Models.Variant.class);
        assertEquals(new BigDecimal("250000"),variant.price);
        assertEquals(new BigDecimal("200000"),variant.effectivePrice);
    }
}
