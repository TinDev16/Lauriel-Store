package com.lauriel.mobile.data.repository;

import com.lauriel.mobile.data.api.StoreApi;
import com.lauriel.mobile.data.local.SessionManager;
import com.lauriel.mobile.data.model.Models;
import retrofit2.*;

public final class AuthRepository {

    private final StoreApi api;
    private final SessionManager session;

    public AuthRepository(StoreApi api, SessionManager session) {
        this.api = api;
        this.session = session;
    }

    public void login(
        String username,
        String password,
        Callback<Models.LoginResponse> callback
    ) {
        api.login(new Models.LoginRequest(username, password)).enqueue(
            callback
        );
    }

    public void register(
        Models.RegisterRequest request,
        Callback<Models.RegisterResponse> callback
    ) {
        api.register(request).enqueue(callback);
    }

    public void me(Callback<Models.Me> callback) {
        api.me().enqueue(callback);
    }

    public void saveCustomer(Models.LoginResponse response) {
        session.save(response);
    }

    public void logout() {
        session.clear();
    }
}
