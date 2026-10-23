package com.lauriel.mobile.ui.voucher;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.lauriel.mobile.adapter.VoucherAdapter;
import com.lauriel.mobile.data.api.ApiClient;
import com.lauriel.mobile.data.local.VoucherStore;
import com.lauriel.mobile.data.model.Models;
import com.lauriel.mobile.databinding.ActivityVouchersBinding;
import com.lauriel.mobile.ui.BaseActivity;
import com.lauriel.mobile.ui.SystemBars;
import java.util.*;
import retrofit2.*;

public final class VoucherActivity extends BaseActivity {

    public static final String EXTRA_MY_VOUCHERS = "myVouchers";
    private ActivityVouchersBinding binding;
    private VoucherStore store;
    private VoucherAdapter adapter;
    private List<Models.Voucher> vouchers = List.of();
    private boolean showMine;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        binding = ActivityVouchersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        SystemBars.apply(binding.getRoot());
        store = new VoucherStore(this, ApiClient.get(this).session().userId());
        showMine = getIntent().getBooleanExtra(EXTRA_MY_VOUCHERS, false);
        adapter = new VoucherAdapter(
            new VoucherAdapter.Listener() {
                @Override
                public void onSave(Models.Voucher voucher) {
                    store.toggle(voucher.voucherId);
                    render();
                }

                @Override
                public void onCopy(Models.Voucher voucher) {
                    copyVoucher(voucher);
                }
            }
        );
        binding.list.setLayoutManager(new LinearLayoutManager(this));
        binding.list.setAdapter(adapter);
        binding.back.setOnClickListener(view -> finish());
        binding.all.setOnClickListener(view -> {
            showMine = false;
            render();
        });
        binding.mine.setOnClickListener(view -> {
            showMine = true;
            render();
        });
        binding.retry.setOnClickListener(view -> load());
        load();
    }

    private void load() {
        binding.loading.setVisibility(View.VISIBLE);
        binding.retry.setVisibility(View.GONE);
        binding.error.setText("");
        ApiClient.get(this).api().activeVouchers(0, 100).enqueue(
            new Callback<Models.Page<Models.Voucher>>() {
                @Override
                public void onResponse(
                    Call<Models.Page<Models.Voucher>> call,
                    Response<Models.Page<Models.Voucher>> response
                ) {
                    binding.loading.setVisibility(View.GONE);
                    if (response.isSuccessful() && response.body() != null) {
                        vouchers =
                            response.body().content == null
                                ? List.of()
                                : response.body().content;
                        render();
                    } else {
                        showError(
                            ApiClient.get(VoucherActivity.this).error(response)
                        );
                    }
                }

                @Override
                public void onFailure(
                    Call<Models.Page<Models.Voucher>> call,
                    Throwable error
                ) {
                    binding.loading.setVisibility(View.GONE);
                    showError("Không thể kết nối máy chủ.");
                }
            }
        );
    }

    private void render() {
        Set<Long> saved = store.savedIds();
        List<Models.Voucher> visible = new ArrayList<>();
        for (Models.Voucher voucher : vouchers) {
            if (!showMine || saved.contains(voucher.voucherId)) visible.add(
                voucher
            );
        }
        visible.sort(
            Comparator.comparing(
                (Models.Voucher item) -> !saved.contains(item.voucherId)
            )
        );
        adapter.submit(visible, saved);
        binding.title.setText(
            showMine ? "Mã giảm giá của tôi" : "Mã giảm giá toàn sàn"
        );
        binding.all.setChecked(!showMine);
        binding.mine.setChecked(showMine);
        binding.empty.setVisibility(
            visible.isEmpty() ? View.VISIBLE : View.GONE
        );
        binding.empty.setText(
            showMine
                ? "Bạn chưa lưu mã giảm giá nào."
                : "Hiện chưa có mã giảm giá khả dụng."
        );
    }

    private void copyVoucher(Models.Voucher voucher) {
        if (voucher.code == null || voucher.code.isBlank()) return;
        ClipboardManager clipboard = (ClipboardManager) getSystemService(
            Context.CLIPBOARD_SERVICE
        );
        clipboard.setPrimaryClip(
            ClipData.newPlainText(
                getString(com.lauriel.mobile.R.string.voucher_code),
                voucher.code
            )
        );
        Toast.makeText(
            this,
            getString(com.lauriel.mobile.R.string.voucher_copied, voucher.code),
            Toast.LENGTH_SHORT
        ).show();
    }

    private void showError(String message) {
        binding.error.setText(message);
        binding.retry.setVisibility(View.VISIBLE);
    }
}
