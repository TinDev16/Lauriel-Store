package com.lauriel.mobile.data.repository;

import com.lauriel.mobile.data.api.StoreApi;
import com.lauriel.mobile.data.local.CartStore;
import com.lauriel.mobile.data.model.Models;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public final class CustomerActivityRepository {

    private final StoreApi api;

    public CustomerActivityRepository(StoreApi api) {
        this.api = api;
    }

    public void heartbeat() {
        api.heartbeat().enqueue(silent());
    }

    public void syncCart(CartStore cart) {
        api.syncCart(new Models.CartSnapshot(cart.payload())).enqueue(silent());
    }

    private static Callback<Void> silent() {
        return new Callback<>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {}

            @Override
            public void onFailure(Call<Void> call, Throwable error) {}
        };
    }
}
