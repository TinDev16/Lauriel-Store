package com.lauriel.mobile.ui.order;

import android.os.Bundle;
import android.view.View;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.lauriel.mobile.R;
import com.lauriel.mobile.adapter.OrderLineAdapter;
import com.lauriel.mobile.data.api.ApiClient;
import com.lauriel.mobile.data.model.Models;
import com.lauriel.mobile.data.repository.OrderRepository;
import com.lauriel.mobile.databinding.ActivityOrderDetailBinding;
import com.lauriel.mobile.ui.BaseActivity;
import com.lauriel.mobile.ui.common.StatusChip;
import com.lauriel.mobile.util.Formatters;
import java.math.BigDecimal;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderDetailActivity extends BaseActivity {
    private ActivityOrderDetailBinding b;
    private OrderLineAdapter itemAdapter;
    private long id;
    private Models.Order order;
    private String pendingError;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        b = ActivityOrderDetailBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        com.lauriel.mobile.ui.SystemBars.apply(b.getRoot());
        id = getIntent().getLongExtra("orderId", -1);
        if (id <= 0) { finish(); return; }

        itemAdapter = new OrderLineAdapter();
        b.items.setLayoutManager(new LinearLayoutManager(this));
        b.items.setAdapter(itemAdapter);
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
            @Override public void onResponse(Call<Models.Order> call, Response<Models.Order> response) {
                if (isFinishing()) return;
                b.loading.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    order = response.body();
                    render();
                    if (pendingError != null) { b.error.setText(pendingError); pendingError = null; }
                } else {
                    b.error.setText(ApiClient.get(OrderDetailActivity.this).error(response));
                }
            }

            @Override public void onFailure(Call<Models.Order> call, Throwable error) {
                if (!isFinishing()) {
                    b.loading.setVisibility(View.GONE);
                    b.error.setText(R.string.network_error);
                }
            }
        });
    }

    private void render() {
        b.title.setText(getString(R.string.order_number, order.orderId));
        b.date.setText(getString(R.string.ordered_at, Formatters.date(order.createdAt)));
        StatusChip.apply(b.status, order.status);
        b.address.setText(order.shippingAddress == null || order.shippingAddress.isBlank()
                ? getString(R.string.no_shipping_address) : order.shippingAddress);

        int lines = order.items == null ? 0 : order.items.size();
        b.productCount.setText(getString(R.string.product_count, lines));
        itemAdapter.submit(order.items);

        b.subtotal.setText(Formatters.money(order.totalAmount));
        BigDecimal orderPromotionDiscount = order.orderPromotionDiscount == null ? BigDecimal.ZERO : order.orderPromotionDiscount;
        b.orderPromotionLabel.setText(order.orderPromotionName == null ? "Khuyến mãi đơn hàng" : order.orderPromotionName);
        b.orderPromotionDiscount.setText(orderPromotionDiscount.signum() == 0 ? "0 ₫" : "−" + Formatters.money(orderPromotionDiscount));
        b.voucher.setText(order.voucherCode == null || order.voucherCode.isBlank() ? getString(R.string.no_voucher) : order.voucherCode);
        BigDecimal discount = order.discountAmount == null ? BigDecimal.ZERO : order.discountAmount;
        b.discount.setText(discount.signum() == 0 ? "0 ₫" : "−" + Formatters.money(discount));
        b.shipping.setText(Formatters.money(order.shippingFee));
        b.finalAmount.setText(Formatters.money(order.finalAmount));
        b.cancel.setVisibility(Formatters.canCancel(order.status) ? View.VISIBLE : View.GONE);
    }

    private void confirmCancel() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Hủy đơn hàng #" + id + "?")
                .setMessage("Tồn kho và lượt sử dụng voucher sẽ được hoàn lại sau khi đơn được hủy thành công.")
                .setNegativeButton("Giữ đơn", null)
                .setPositiveButton("Xác nhận hủy", (dialog, which) -> cancel())
                .show();
    }

    private void cancel() {
        b.cancel.setEnabled(false);
        b.loading.setVisibility(View.VISIBLE);
        new OrderRepository(ApiClient.get(this).api()).cancel(id, new Callback<Models.Order>() {
            @Override public void onResponse(Call<Models.Order> call, Response<Models.Order> response) {
                if (isFinishing()) return;
                b.loading.setVisibility(View.GONE);
                b.cancel.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    order = response.body();
                    render();
                    Snackbar.make(b.getRoot(), "Đã hủy đơn hàng", Snackbar.LENGTH_SHORT).show();
                    if (pendingError != null) { b.error.setText(pendingError); pendingError = null; }
                } else {
                    pendingError = ApiClient.get(OrderDetailActivity.this).error(response);
                    load();
                }
            }

            @Override public void onFailure(Call<Models.Order> call, Throwable error) {
                if (isFinishing()) return;
                b.loading.setVisibility(View.GONE);
                b.cancel.setEnabled(true);
                b.error.setText(R.string.network_error);
            }
        });
    }
}
