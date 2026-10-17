package com.lauriel.mobile.data.api;

import com.lauriel.mobile.data.model.Models;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.*;

public interface StoreApi {
    @POST("api/auth/login") Call<Models.LoginResponse> login(@Body Models.LoginRequest body);
    @POST("api/auth/register") Call<Models.RegisterResponse> register(@Body Models.RegisterRequest body);
    @GET("api/auth/me") Call<Models.Me> me();
    @POST("api/customer/activity/heartbeat") Call<Void> heartbeat();
    @PUT("api/customer/activity/cart") Call<Void> syncCart(@Body Models.CartSnapshot body);
    @GET("api/customers/me") Call<Models.Profile> profile();
    @PUT("api/customers/me") Call<Models.Profile> updateProfile(@Body Models.ProfileUpdate body);
    @GET("api/promotions/active") Call<Models.Page<Models.Promotion>> activePromotions(@Query("page") int page,@Query("size") int size);
    @GET("api/order-promotions/active") Call<Models.Page<Models.OrderPromotion>> activeOrderPromotions(@Query("page") int page,@Query("size") int size);
    @GET("api/products") Call<Models.Page<Models.Product>> products(@Query("page") int page,@Query("size") int size,
            @Query("keyword") String keyword,@Query("categoryId") Long categoryId,@Query("sizeId") Long sizeId,@Query("colorId") Long colorId,
            @Query("sortBy") String sortBy,@Query("direction") String direction);
    @GET("api/products/featured") Call<List<Models.Product>> featuredProducts(@Query("limit") int limit);
    @GET("api/products/{id}") Call<Models.Product> product(@Path("id") long id);
    @GET("api/products/{id}/related") Call<List<Models.Product>> related(@Path("id") long id,@Query("limit") int limit);
    @GET("api/categories") Call<List<Models.Category>> categories();
    @GET("api/sizes") Call<List<Models.Size>> sizes();
    @GET("api/colors") Call<List<Models.Color>> colors();
    @GET("api/vouchers/active") Call<Models.Page<Models.Voucher>> activeVouchers(@Query("page") int page,@Query("size") int size);
    @POST("api/vouchers/validate") Call<Models.VoucherPreview> preview(@Body Models.VoucherRequest body);
    @POST("api/orders/preview") Call<Models.VoucherPreview> previewOrder(@Body Models.OrderPreviewRequest body);
    @POST("api/orders") Call<Models.Order> checkout(@Body Models.CheckoutRequest body);
    @GET("api/orders/my-orders") Call<Models.Page<Models.OrderSummary>> myOrders(
            @Query("page") int page,@Query("size") int size,@Query("status") String status);
    @GET("api/orders/{id}") Call<Models.Order> order(@Path("id") long id);
    @POST("api/orders/{id}/cancel") Call<Models.Order> cancel(@Path("id") long id);
}
