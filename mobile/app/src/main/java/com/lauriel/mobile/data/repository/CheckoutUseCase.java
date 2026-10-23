package com.lauriel.mobile.data.repository;

import com.lauriel.mobile.data.local.CartStore;
import com.lauriel.mobile.data.model.Models;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Sends only IDs, quantities, address and optional voucher; the server owns prices and stock. */
public final class CheckoutUseCase {

    private final OrderRepository orders;
    private final CartStore cart;
    private final List<Models.Item> temporaryItems;

    public CheckoutUseCase(OrderRepository orders, CartStore cart) {
        this.orders = orders;
        this.cart = cart;
        this.temporaryItems = null;
    }

    public CheckoutUseCase(
        OrderRepository orders,
        List<Models.Item> temporaryItems
    ) {
        this.orders = orders;
        this.cart = null;
        this.temporaryItems = List.copyOf(temporaryItems);
    }

    public void place(
        String address,
        String voucherCode,
        Callback<Models.Order> callback
    ) {
        long revision = cart == null ? -1 : cart.revision();
        List<Models.Item> payload =
            cart == null ? temporaryItems : cart.payload();
        orders.checkout(
            payload,
            address,
            voucherCode,
            new Callback<Models.Order>() {
                @Override
                public void onResponse(
                    Call<Models.Order> call,
                    Response<Models.Order> response
                ) {
                    if (
                        cart != null &&
                        response.isSuccessful() &&
                        response.body() != null
                    ) {
                        cart.clearIfRevision(revision);
                    }
                    callback.onResponse(call, response);
                }

                @Override
                public void onFailure(
                    Call<Models.Order> call,
                    Throwable error
                ) {
                    callback.onFailure(call, error);
                }
            }
        );
    }
}
