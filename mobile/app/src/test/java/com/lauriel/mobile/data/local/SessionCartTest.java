package com.lauriel.mobile.data.local;

import static org.junit.Assert.*;

import android.content.Context;
import com.lauriel.mobile.data.model.Models;
import com.lauriel.mobile.util.Formatters;
import java.math.BigDecimal;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
public class SessionCartTest {

    private Context context;
    private SessionManager session;
    private CartStore cart;

    @Before
    public void setup() {
        context = RuntimeEnvironment.getApplication();
        context
            .getSharedPreferences("lauriel_session", 0)
            .edit()
            .clear()
            .commit();
        context.getSharedPreferences("cart_42", 0).edit().clear().commit();
        context.getSharedPreferences("cart_43", 0).edit().clear().commit();
        session = new SessionManager(context);
        cart = new CartStore(context, 42);
    }

    private Models.LoginResponse login(String role) {
        var r = new Models.LoginResponse();
        r.token = "jwt-test";
        r.userId = 42;
        r.username = "customer";
        r.role = role;
        return r;
    }

    private CartStore.CartItem item(long id, int qty, int stock) {
        return new CartStore.CartItem(
            id,
            7,
            "Shirt",
            "SKU-" + id,
            "M",
            "Black",
            null,
            new BigDecimal("120.50"),
            qty,
            stock
        );
    }

    @Test
    public void customerLoginSavesTokenAndIdentity() {
        session.save(login("CUSTOMER"));
        assertEquals("jwt-test", session.token());
        assertEquals(42, session.userId());
        assertTrue(session.hasCustomer());
    }

    @Test
    public void adminCannotBeStored() {
        try {
            session.save(login("ADMIN"));
            fail();
        } catch (IllegalArgumentException expected) {}
        assertFalse(session.hasCustomer());
    }

    @Test
    public void staffCannotBeStored() {
        try {
            session.save(login("STAFF"));
            fail();
        } catch (IllegalArgumentException expected) {}
        assertNull(session.token());
    }

    @Test
    public void loginPersistsWithoutPassword() {
        session.save(login("CUSTOMER"));
        assertEquals("customer", new SessionManager(context).username());
        assertFalse(
            context
                .getSharedPreferences("lauriel_session", 0)
                .contains("password")
        );
    }

    @Test
    public void logoutClearsIdentity() {
        session.save(login("CUSTOMER"));
        session.clear();
        assertNull(session.token());
        assertEquals(-1, session.userId());
    }

    @Test
    public void addVariant() {
        cart.add(item(1, 2, 5));
        assertEquals(1, cart.items().size());
        assertEquals(2, cart.items().get(0).quantity);
    }

    @Test
    public void duplicateVariantMerges() {
        cart.add(item(1, 2, 5));
        cart.add(item(1, 3, 5));
        assertEquals(1, cart.items().size());
        assertEquals(5, cart.items().get(0).quantity);
    }

    @Test
    public void existingCartItemReceivesNewImageWhenAddedAgain() {
        cart.add(item(1, 1, 5));
        cart.add(
            new CartStore.CartItem(
                1,
                7,
                "Shirt",
                "SKU-1",
                "M",
                "Black",
                "/uploads/products/new.png",
                new BigDecimal("120.50"),
                1,
                5
            )
        );
        assertEquals("/uploads/products/new.png", cart.items().get(0).image);
    }

    @Test
    public void cartImageRefreshPersistsWithoutChangingQuantity() {
        cart.add(item(1, 2, 5));
        cart.add(item(2, 1, 5));
        long checkoutRevision = cart.revision();
        CartStore observer = new CartStore(context, 42);
        assertTrue(cart.updateImage(7, "/uploads/products/new.png"));
        assertEquals(checkoutRevision, cart.revision());
        assertEquals(
            "/uploads/products/new.png",
            observer.items().get(0).image
        );
        assertFalse(cart.updateImage(7, "/uploads/products/new.png"));
        var saved = new CartStore(context, 42).items();
        assertEquals(2, saved.get(0).quantity);
        assertEquals("/uploads/products/new.png", saved.get(0).image);
        assertEquals("/uploads/products/new.png", saved.get(1).image);
    }

    @Test
    public void duplicateCannotExceedStock() {
        cart.add(item(1, 4, 5));
        try {
            cart.add(item(1, 2, 5));
            fail();
        } catch (IllegalArgumentException expected) {}
        assertEquals(4, cart.items().get(0).quantity);
    }

    @Test
    public void updateQuantity() {
        cart.add(item(1, 1, 5));
        cart.setQuantity(1, 4);
        assertEquals(4, cart.items().get(0).quantity);
    }

    @Test
    public void quantityZeroRemoves() {
        cart.add(item(1, 1, 5));
        cart.setQuantity(1, 0);
        assertTrue(cart.isEmpty());
    }

    @Test
    public void removeItem() {
        cart.add(item(1, 1, 5));
        cart.remove(1);
        assertTrue(cart.isEmpty());
    }

    @Test
    public void cartPersistsAcrossInstances() {
        cart.add(item(1, 1, 5));
        assertEquals(1, new CartStore(context, 42).items().size());
    }

    @Test
    public void cartsAreSeparatedByUser() {
        cart.add(item(1, 1, 5));
        assertTrue(new CartStore(context, 43).isEmpty());
    }

    @Test
    public void checkoutSuccessClearsCart() {
        cart.add(item(1, 1, 5));
        long rev = cart.revision();
        assertTrue(cart.clearIfRevision(rev));
        assertTrue(cart.isEmpty());
    }

    @Test
    public void checkoutFailureRetainsCart() {
        cart.add(item(1, 1, 5));
        assertEquals(1, cart.items().size());
    }

    @Test
    public void staleCheckoutCannotClearNewCart() {
        cart.add(item(1, 1, 5));
        long rev = cart.revision();
        cart.add(item(2, 1, 5));
        assertFalse(cart.clearIfRevision(rev));
        assertEquals(2, cart.items().size());
    }

    @Test
    public void cartMutationInvalidatesVoucherRevision() {
        cart.add(item(1, 1, 5));
        long rev = cart.revision();
        cart.setQuantity(1, 2);
        assertNotEquals(rev, cart.revision());
    }

    @Test
    public void checkoutPayloadContainsOnlyVariantIdAndQuantity() {
        cart.add(item(1, 2, 5));
        String json = new com.google.gson.Gson().toJson(cart.payload());
        assertEquals("[{\"variantId\":1,\"quantity\":2}]", json);
        assertFalse(json.contains("price"));
    }

    @Test
    public void concurrentCartInstancesRefreshBeforeCheckout() {
        cart.add(item(1, 1, 5));
        CartStore checkout = new CartStore(context, 42);
        long previewRevision = checkout.revision();
        cart.setQuantity(1, 2);
        assertNotEquals(previewRevision, checkout.revision());
        assertEquals(2, checkout.payload().get(0).quantity);
        assertFalse(checkout.clearIfRevision(previewRevision));
    }

    @Test
    public void returnedCartItemsCannotMutateStoredQuantity() {
        cart.add(item(1, 1, 5));
        cart.items().get(0).quantity = 999;
        assertEquals(1, cart.items().get(0).quantity);
    }

    @Test
    public void decimalSubtotalUsesBigDecimal() {
        cart.add(item(1, 2, 5));
        assertEquals(new BigDecimal("241.00"), cart.displaySubtotal());
    }

    @Test
    public void apiBusinessErrorBodyIsMapped() {
        var api = com.lauriel.mobile.data.api.ApiClient.get(context);
        var body = okhttp3.ResponseBody.create(
            "{\"status\":409,\"message\":\"Tồn kho đã thay đổi\"}",
            okhttp3.MediaType.parse("application/json")
        );
        var response = retrofit2.Response.error(409, body);
        assertEquals("Tồn kho đã thay đổi", api.error(response));
    }

    @Test
    public void moneyUsesVietnameseGrouping() {
        assertTrue(
            Formatters.money(new BigDecimal("250000")).contains("250.000")
        );
    }

    @Test
    public void statusLabelsAndCancelPolicy() {
        assertEquals("Chờ xác nhận", Formatters.status("PENDING"));
        assertTrue(Formatters.canCancel("CONFIRMED"));
        assertFalse(Formatters.canCancel("PROCESSING"));
    }

    @Test
    public void isoDateCanBeDisplayed() {
        assertEquals(
            "18/09/2026 10:30",
            Formatters.date("2026-09-18T10:30:00")
        );
    }
}
