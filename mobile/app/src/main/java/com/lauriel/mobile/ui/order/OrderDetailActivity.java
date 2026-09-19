package com.lauriel.mobile.ui.order;

import android.os.Bundle;
import android.view.View;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.lauriel.mobile.R;
import com.lauriel.mobile.data.api.ApiClient;
import com.lauriel.mobile.data.model.Models;
import com.lauriel.mobile.data.repository.OrderRepository;
import com.lauriel.mobile.databinding.ActivityOrderDetailBinding;
import com.lauriel.mobile.ui.BaseActivity;
import com.lauriel.mobile.util.Formatters;
import retrofit2.*;

public class OrderDetailActivity extends BaseActivity {
    private ActivityOrderDetailBinding b;
    private long id;
    private Models.Order order;
    private String pendingError;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        b = ActivityOrderDetailBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());com.lauriel.mobile.ui.SystemBars.apply(b.getRoot());
        id = getIntent().getLongExtra("orderId", -1);
        if (id <= 0) { finish(); return; }
        b.back.setOnClickListener(v -> finish());
        b.error.setOnClickListener(v -> load());
        b.cancel.setOnClickListener(v -> confirmCancel());
        load();
    }
    private void load() {
        b.loading.setVisibility(View.VISIBLE);
        if (pendingError == null) b.error.setText("");
        b.cancel.setVisibility(View.GONE);
        new OrderRepository(ApiClient.get(this).api()).detail(id, new Callback<Models.Order>() {
            @Override public void onResponse(Call<Models.Order> c, Response<Models.Order> r) {
                if (isFinishing()) return;
                b.loading.setVisibility(View.GONE);
                if (r.isSuccessful() && r.body() != null) {
                    order = r.body(); render();
                    if (pendingError != null) { b.error.setText(pendingError); pendingError = null; }
                }
                else b.error.setText(ApiClient.get(OrderDetailActivity.this).error(r));
            }
            @Override public void onFailure(Call<Models.Order> c, Throwable error) {
                if (!isFinishing()) {
                    b.loading.setVisibility(View.GONE);
                    b.error.setText(R.string.network_error);
                }
            }
        });
    }
    private void render() {
        b.title.setText("Đơn #" + order.orderId + " · " + Formatters.status(order.status));
        StringBuilder detail = new StringBuilder();
        detail.append("Ngày: ").append(Formatters.date(order.createdAt)).append("\n")
                .append("Giao tới: ").append(order.shippingAddress).append("\n\n");
        if (order.items != null) for (var item : order.items) {
            detail.append(item.productName).append(" · ").append(item.sku).append("\n")
                    .append(item.size == null ? "" : item.size.sizeName).append(" / ")
                    .append(item.color == null ? "" : item.color.colorName).append(" × ")
                    .append(item.quantity).append("\n")
                    .append("Giá tại lúc đặt: ").append(Formatters.money(item.unitPrice)).append("\n")
                    .append("Giảm khuyến mãi / đơn vị: ").append(Formatters.money(item.discountAmount)).append("\n")
                    .append("Thành tiền: ").append(Formatters.money(item.subtotal)).append("\n\n");
        }
        detail.append("Tổng sau khuyến mãi: ").append(Formatters.money(order.totalAmount)).append("\n")
                .append("Mã giảm giá: ").append(order.voucherCode == null ? "—" : order.voucherCode).append("\n")
                .append("Giảm voucher: ").append(Formatters.money(order.discountAmount)).append("\n")
                .append("Thanh toán: ").append(Formatters.money(order.finalAmount));
        b.details.setText(detail.toString());
        b.cancel.setVisibility(Formatters.canCancel(order.status) ? View.VISIBLE : View.GONE);
    }
    private void confirmCancel() {
        new MaterialAlertDialogBuilder(this).setTitle("Hủy đơn #" + id + "?")
                .setMessage("Đơn sẽ được hủy nếu trạng thái hiện tại cho phép.")
                .setNegativeButton("Giữ đơn", null)
                .setPositiveButton("Hủy đơn", (dialog, which) -> cancel()).show();
    }
    private void cancel() {
        b.cancel.setEnabled(false);
        b.loading.setVisibility(View.VISIBLE);
        new OrderRepository(ApiClient.get(this).api()).cancel(id, new Callback<Models.Order>() {
            @Override public void onResponse(Call<Models.Order> c, Response<Models.Order> r) {
                if (isFinishing()) return;
                b.loading.setVisibility(View.GONE);
                b.cancel.setEnabled(true);
                if (r.isSuccessful() && r.body() != null) {
                    order = r.body(); render();
                    if (pendingError != null) { b.error.setText(pendingError); pendingError = null; }
                }
                else {
                    pendingError = ApiClient.get(OrderDetailActivity.this).error(r);
                    load();
                }
            }
            @Override public void onFailure(Call<Models.Order> c, Throwable error) {
                if (isFinishing()) return;
                b.loading.setVisibility(View.GONE);
                b.cancel.setEnabled(true);
                b.error.setText(R.string.network_error);
            }
        });
    }
}
