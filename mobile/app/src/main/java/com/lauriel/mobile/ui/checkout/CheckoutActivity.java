package com.lauriel.mobile.ui.checkout;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.lauriel.mobile.R;
import com.lauriel.mobile.data.api.ApiClient;
import com.lauriel.mobile.data.local.CartStore;
import com.lauriel.mobile.data.model.Models;
import com.lauriel.mobile.data.repository.OrderRepository;
import com.lauriel.mobile.data.repository.VoucherRepository;
import com.lauriel.mobile.databinding.ActivityCheckoutBinding;
import com.lauriel.mobile.ui.BaseActivity;
import com.lauriel.mobile.ui.MainActivity;
import com.lauriel.mobile.ui.order.OrderDetailActivity;
import com.lauriel.mobile.util.Formatters;
import retrofit2.*;

public class CheckoutActivity extends BaseActivity {
    private ActivityCheckoutBinding b;
    private CartStore cart;
    private Models.VoucherPreview preview;
    private long previewRevision = -1;
    private String previewCode;
    private boolean submitting, validating;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        b = ActivityCheckoutBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());com.lauriel.mobile.ui.SystemBars.apply(b.getRoot());
        cart = new CartStore(this, ApiClient.get(this).session().userId());
        b.back.setOnClickListener(v -> finish());
        b.apply.setOnClickListener(v -> validateVoucher());
        b.checkout.setOnClickListener(v -> checkout());
        b.voucher.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { clearPreview(); }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
        render();
    }

    @Override protected void onResume() {
        super.onResume();
        if (cart != null && previewRevision != cart.revision()) clearPreview();
        if (b != null) render();
    }

    private void render() {
        if (cart.isEmpty()) {
            b.summary.setText(R.string.empty_cart);
            b.checkout.setEnabled(false);
            return;
        }
        StringBuilder summary = new StringBuilder();
        for (var item : cart.items()) {
            summary.append(item.productName).append(" · ").append(item.size).append("/")
                    .append(item.color).append(" × ").append(item.quantity).append("\n");
        }
        summary.append("\nTạm tính hiển thị: ").append(Formatters.money(cart.displaySubtotal()));
        b.summary.setText(summary.toString());
        b.checkout.setEnabled(!submitting);
    }

    private void clearPreview() {
        preview = null;
        previewRevision = -1;
        previewCode = null;
        if (b != null) b.preview.setText("");
    }

    private void validateVoucher() {
        String code = b.voucher.getText().toString().trim();
        if (code.isEmpty()) { clearPreview(); b.error.setText("Nhập mã giảm giá."); return; }
        if (cart.isEmpty() || validating) return;
        long revision = cart.revision();
        validating = true;
        b.apply.setEnabled(false);
        b.loading.setVisibility(View.VISIBLE);
        b.error.setText("");
        new VoucherRepository(ApiClient.get(this).api()).preview(code, cart.payload(),
                new Callback<Models.VoucherPreview>() {
            @Override public void onResponse(Call<Models.VoucherPreview> c, Response<Models.VoucherPreview> r) {
                if (isFinishing()) return;
                validating = false;
                b.apply.setEnabled(true);
                b.loading.setVisibility(View.GONE);
                if (revision != cart.revision() || !code.equals(b.voucher.getText().toString().trim())) {
                    clearPreview(); return;
                }
                if (r.isSuccessful() && r.body() != null && r.body().valid) {
                    preview = r.body();
                    previewRevision = revision;
                    previewCode = code;
                    b.preview.setText("Tổng sau khuyến mãi: " + Formatters.money(preview.orderAmount)
                            + "\nGiảm voucher: " + Formatters.money(preview.discountAmount)
                            + "\nDự kiến thanh toán: " + Formatters.money(preview.finalAmount));
                } else {
                    clearPreview();
                    b.error.setText(ApiClient.get(CheckoutActivity.this).error(r));
                }
            }
            @Override public void onFailure(Call<Models.VoucherPreview> c, Throwable error) {
                if (isFinishing()) return;
                validating = false;
                b.apply.setEnabled(true);
                b.loading.setVisibility(View.GONE);
                clearPreview();
                b.error.setText(R.string.network_error);
            }
        });
    }

    private void checkout() {
        if (submitting || validating || cart.isEmpty()) return;
        String address = b.address.getText().toString().trim();
        String code = b.voucher.getText().toString().trim();
        if (address.isEmpty()) { b.error.setText("Nhập địa chỉ giao hàng."); return; }
        if (!code.isEmpty() && (preview == null || previewRevision != cart.revision()
                || !code.equals(previewCode))) {
            b.error.setText("Giỏ hàng hoặc mã đã thay đổi. Vui lòng áp dụng voucher lại.");
            return;
        }
        submitting = true;
        b.checkout.setEnabled(false);
        b.loading.setVisibility(View.VISIBLE);
        b.error.setText("");
        new com.lauriel.mobile.data.repository.CheckoutUseCase(
                new OrderRepository(ApiClient.get(this).api()), cart).place(address,
                code.isEmpty() ? null : code, new Callback<Models.Order>() {
            @Override public void onResponse(Call<Models.Order> c, Response<Models.Order> r) {
                if (isFinishing()) return;
                submitting = false;
                b.loading.setVisibility(View.GONE);
                if (r.isSuccessful() && r.body() != null) {
                    clearPreview();
                    long id = r.body().orderId;
                    new MaterialAlertDialogBuilder(CheckoutActivity.this)
                            .setTitle("Đặt hàng thành công").setMessage("Đơn #" + id + " đã được tạo.")
                            .setPositiveButton("Xem đơn hàng", (dialog, which) -> {
                                startActivity(new Intent(CheckoutActivity.this, OrderDetailActivity.class)
                                        .putExtra("orderId", id));
                                finish();
                            }).setNegativeButton("Tiếp tục mua", (dialog, which) -> {
                                startActivity(new Intent(CheckoutActivity.this, MainActivity.class)
                                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                                finish();
                            }).setCancelable(false).show();
                } else {
                    b.checkout.setEnabled(true);
                    clearPreview();
                    b.error.setText(ApiClient.get(CheckoutActivity.this).error(r)
                            + (r.code() == 409 ? " Kiểm tra lại tồn kho và giỏ hàng." : ""));
                }
            }
            @Override public void onFailure(Call<Models.Order> c, Throwable error) {
                if (isFinishing()) return;
                submitting = false;
                b.loading.setVisibility(View.GONE);
                b.checkout.setEnabled(true);
                clearPreview();
                b.error.setText(R.string.network_error);
            }
        });
    }
}
