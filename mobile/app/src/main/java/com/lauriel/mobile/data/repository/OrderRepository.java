package com.lauriel.mobile.data.repository;

import com.lauriel.mobile.data.api.StoreApi;
import com.lauriel.mobile.data.model.Models;
import java.util.List;
import retrofit2.*;

public final class OrderRepository {

    private final StoreApi api;

    public OrderRepository(StoreApi api) {
        this.api = api;
    }

    public void list(
        int page,
        String status,
        Callback<Models.Page<Models.OrderSummary>> cb
    ) {
        api.myOrders(page, 12, status).enqueue(cb);
    }

    public void list(int page, Callback<Models.Page<Models.OrderSummary>> cb) {
        list(page, null, cb);
    }

    public void detail(long id, Callback<Models.Order> cb) {
        api.order(id).enqueue(cb);
    }

    public void cancel(long id, Callback<Models.Order> cb) {
        api.cancel(id).enqueue(cb);
    }

    public void checkout(
        List<Models.Item> items,
        String address,
        String voucher,
        Callback<Models.Order> cb
    ) {
        api.checkout(
            new Models.CheckoutRequest(items, address, voucher)
        ).enqueue(cb);
    }
}
