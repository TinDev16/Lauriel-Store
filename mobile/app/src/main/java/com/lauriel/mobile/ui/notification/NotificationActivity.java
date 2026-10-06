package com.lauriel.mobile.ui.notification;

import android.content.Intent;
import android.os.Bundle;
import com.lauriel.mobile.databinding.ActivityNotificationsBinding;
import com.lauriel.mobile.ui.BaseActivity;
import com.lauriel.mobile.ui.SystemBars;
import com.lauriel.mobile.ui.voucher.VoucherActivity;

public final class NotificationActivity extends BaseActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        ActivityNotificationsBinding binding = ActivityNotificationsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        SystemBars.apply(binding.getRoot());
        binding.back.setOnClickListener(view -> finish());
        binding.vouchers.setOnClickListener(view ->
                startActivity(new Intent(this, VoucherActivity.class)));
    }
}
