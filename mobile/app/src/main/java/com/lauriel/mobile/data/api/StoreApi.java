package com.lauriel.mobile.data.api;

import com.lauriel.mobile.data.model.Models;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.*;

public interface StoreApi {
    @POST("api/auth/login") Call<Models.LoginResponse> login(@Body Models.LoginRequest body);
    @POST("api/auth/register") Call<Models.RegisterResponse> register(@Body Models.RegisterRequest body);
    @GET("api/auth/me") Call<Models.Me> me();
    @GET("api/promotions/active") Call<Models.Page<Models.Promotion>> activePromotions(@Query("page") int page,@Query("size") int size);
    @GET("api/products") Call<Models.Page<Models.Product>> products(@Query("page") int page,@Query("size") int size,
            @Query("keyword") String keyword,@Query("sizeId") Long sizeId,@Query("colorId") Long colorId,
            @Query("sortBy") String sortBy,@Query("direction") String direction);
    @GET("api/products/{id}") Call<Models.Product> product(@Path("id") long id);
    @GET("api/sizes") Call<List<Models.Size>> sizes();
    @GET("api/colors") Call<List<Models.Color>> colors();
    @POST("api/vouchers/validate") Call<Models.VoucherPreview> preview(@Body Models.VoucherRequest body);
    @POST("api/orders") Call<Models.Order> checkout(@Body Models.CheckoutRequest body);
    @GET("api/orders/my-orders") Call<Models.Page<Models.OrderSummary>> myOrders(@Query("page") int page,@Query("size") int size);
    @GET("api/orders/{id}") Call<Models.Order> order(@Path("id") long id);
    @POST("api/orders/{id}/cancel") Call<Models.Order> cancel(@Path("id") long id);
}
