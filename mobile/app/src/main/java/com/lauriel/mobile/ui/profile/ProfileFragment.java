package com.lauriel.mobile.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.lauriel.mobile.R;
import com.lauriel.mobile.data.api.ApiClient;
import com.lauriel.mobile.data.model.Models;
import com.lauriel.mobile.databinding.FragmentProfileBinding;
import com.lauriel.mobile.ui.MainActivity;
import com.lauriel.mobile.ui.auth.LoginActivity;
import com.lauriel.mobile.ui.voucher.VoucherActivity;
import retrofit2.*;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;

    @Nullable
    @Override
    public View onCreateView(
        @NonNull LayoutInflater inflater,
        @Nullable ViewGroup container,
        @Nullable Bundle state
    ) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        binding.retry.setOnClickListener(item -> load());
        binding.logout.setOnClickListener(item -> confirmLogout());
        binding.orders.setOnClickListener(item ->
            ((MainActivity) requireActivity()).showTab(R.id.nav_orders)
        );
        binding.vouchers.setOnClickListener(item ->
            startActivity(
                new Intent(requireContext(), VoucherActivity.class).putExtra(
                    VoucherActivity.EXTRA_MY_VOUCHERS,
                    true
                )
            )
        );
        binding.edit.setOnClickListener(item ->
            startActivity(
                new Intent(requireContext(), EditProfileActivity.class)
            )
        );
        load();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) load();
    }

    private void load() {
        binding.loading.setVisibility(View.VISIBLE);
        binding.retry.setVisibility(View.GONE);
        binding.error.setText("");
        ApiClient.get(requireContext()).api().profile().enqueue(
            new Callback<Models.Profile>() {
                @Override
                public void onResponse(
                    Call<Models.Profile> call,
                    Response<Models.Profile> response
                ) {
                    if (binding == null) return;
                    binding.loading.setVisibility(View.GONE);
                    if (
                        response.isSuccessful() && response.body() != null
                    ) render(response.body());
                    else showError(
                        ApiClient.get(requireContext()).error(response)
                    );
                }

                @Override
                public void onFailure(
                    Call<Models.Profile> call,
                    Throwable error
                ) {
                    if (binding != null) {
                        binding.loading.setVisibility(View.GONE);
                        showError(getString(R.string.network_error));
                    }
                }
            }
        );
    }

    private void render(Models.Profile profile) {
        String name =
            profile.fullName == null || profile.fullName.isBlank()
                ? "Khách hàng"
                : profile.fullName;
        binding.avatar.setText(
            name.substring(0, 1).toUpperCase(java.util.Locale.ROOT)
        );
        binding.fullName.setText(name);
        binding.username.setText("@" + profile.username);
        binding.email.setText("Email: " + value(profile.email));
        binding.phone.setText("Số điện thoại: " + value(profile.phone));
        binding.address.setText("Địa chỉ: " + value(profile.address));
        binding.edit.setEnabled(true);
    }

    private String value(String value) {
        return value == null || value.isBlank() ? "Chưa cập nhật" : value;
    }

    private void showError(String message) {
        binding.error.setText(message);
        binding.retry.setVisibility(View.VISIBLE);
        binding.edit.setEnabled(false);
    }

    private void confirmLogout() {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Đăng xuất?")
            .setMessage("Bạn cần đăng nhập lại để tiếp tục mua sắm.")
            .setNegativeButton("Ở lại", null)
            .setPositiveButton(R.string.logout, (dialog, which) -> logout())
            .show();
    }

    private void logout() {
        var client = ApiClient.get(requireContext());
        client.session().clear();
        client.events().reset();
        startActivity(
            new Intent(requireContext(), LoginActivity.class).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK
            )
        );
        requireActivity().finish();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
