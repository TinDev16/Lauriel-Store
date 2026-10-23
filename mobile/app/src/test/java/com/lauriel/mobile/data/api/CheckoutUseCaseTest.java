package com.lauriel.mobile.data.api;

import static org.junit.Assert.*;

import android.content.Context;
import android.os.Looper;
import com.lauriel.mobile.data.local.CartStore;
import com.lauriel.mobile.data.model.Models;
import com.lauriel.mobile.data.repository.CheckoutUseCase;
import com.lauriel.mobile.data.repository.OrderRepository;
import java.math.BigDecimal;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.mockwebserver.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import retrofit2.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
public class CheckoutUseCaseTest {

    private MockWebServer server;
    private CartStore cart;
    private CheckoutUseCase checkout;

    @Before
    public void setup() throws Exception {
        server = new MockWebServer();
        server.start();
        Context context = RuntimeEnvironment.getApplication();
        context.getSharedPreferences("cart_91", 0).edit().clear().commit();
        cart = new CartStore(context, 91);
        cart.add(
            new CartStore.CartItem(
                15,
                3,
                "Shirt",
                "SKU-15",
                "M",
                "Black",
                null,
                new BigDecimal("250000"),
                2,
                5
            )
        );
        checkout = new CheckoutUseCase(
            new OrderRepository(
                ApiClient.createApi(
                    server.url("/").toString(),
                    () -> "jwt",
                    () -> {}
                )
            ),
            cart
        );
    }

    @After
    public void close() throws Exception {
        server.shutdown();
    }

    private Response<Models.Order> place() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Response<Models.Order>> response =
            new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        checkout.place(
            "12 Main St",
            "WEB10",
            new Callback<Models.Order>() {
                @Override
                public void onResponse(
                    Call<Models.Order> c,
                    Response<Models.Order> r
                ) {
                    response.set(r);
                    latch.countDown();
                }

                @Override
                public void onFailure(Call<Models.Order> c, Throwable e) {
                    failure.set(e);
                    latch.countDown();
                }
            }
        );
        long until = System.currentTimeMillis() + 5000;
        while (latch.getCount() != 0 && System.currentTimeMillis() < until) {
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            Thread.sleep(10);
        }
        assertTrue("Checkout callback timed out", latch.getCount() == 0);
        if (failure.get() != null) throw new AssertionError(failure.get());
        return response.get();
    }

    @Test
    public void successfulCheckoutClearsCartOnlyAfter201() throws Exception {
        server.enqueue(
            new MockResponse()
                .setResponseCode(201)
                .setBody("{\"orderId\":8,\"items\":[]}")
        );
        assertTrue(place().isSuccessful());
        assertTrue(cart.isEmpty());
        String body = server.takeRequest().getBody().readUtf8();
        assertEquals(
            "{\"items\":[{\"variantId\":15,\"quantity\":2}],\"shippingAddress\":\"12 Main St\",\"voucherCode\":\"WEB10\"}",
            body
        );
    }

    @Test
    public void stockConflictKeepsCart() throws Exception {
        server.enqueue(
            new MockResponse()
                .setResponseCode(409)
                .setBody("{\"status\":409,\"message\":\"Stock changed\"}")
        );
        assertFalse(place().isSuccessful());
        assertEquals(2, cart.payload().get(0).quantity);
    }

    @Test
    public void invalidVoucherKeepsCart() throws Exception {
        server.enqueue(
            new MockResponse()
                .setResponseCode(400)
                .setBody("{\"status\":400,\"message\":\"Voucher invalid\"}")
        );
        assertFalse(place().isSuccessful());
        assertFalse(cart.isEmpty());
    }

    @Test
    public void buyNowUsesTemporaryItemAndNeverMutatesPersistentCart()
        throws Exception {
        checkout = new CheckoutUseCase(
            new OrderRepository(
                ApiClient.createApi(
                    server.url("/").toString(),
                    () -> "jwt",
                    () -> {}
                )
            ),
            java.util.Collections.singletonList(new Models.Item(99, 3))
        );
        server.enqueue(
            new MockResponse()
                .setResponseCode(201)
                .setBody("{\"orderId\":9,\"items\":[]}")
        );
        assertTrue(place().isSuccessful());
        assertFalse(cart.isEmpty());
        assertEquals(2, cart.payload().get(0).quantity);
        String body = server.takeRequest().getBody().readUtf8();
        assertTrue(body.contains("\"variantId\":99"));
        assertTrue(body.contains("\"quantity\":3"));
    }
}
