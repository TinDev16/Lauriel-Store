package com.lauriel.mobile.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import com.lauriel.mobile.R;
import com.lauriel.mobile.data.api.ApiClient;
import com.lauriel.mobile.data.model.Models;
import com.lauriel.mobile.databinding.FragmentProfileBinding;
import com.lauriel.mobile.ui.auth.LoginActivity;
import retrofit2.*;

public class ProfileFragment extends Fragment {
    private FragmentProfileBinding b;
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentProfileBinding.inflate(i, c, false);
        return b.getRoot();
    }
    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        b.retry.setOnClickListener(w -> load());
        b.logout.setOnClickListener(w -> logout());
        load();
    }
    private void load() {
        b.loading.setVisibility(View.VISIBLE);
        b.error.setText("");
        new com.lauriel.mobile.data.repository.AuthRepository(ApiClient.get(requireContext()).api(),ApiClient.get(requireContext()).session()).me(new Callback<Models.Me>() {
            @Override public void onResponse(Call<Models.Me> c, Response<Models.Me> r) {
                if (b == null) return;
                b.loading.setVisibility(View.GONE);
                if (r.isSuccessful() && r.body() != null)
                    b.account.setText("Tên đăng nhập: " + r.body().username + "\nVai trò: " + com.lauriel.mobile.util.Formatters.role(r.body().role));
                else b.error.setText(ApiClient.get(requireContext()).error(r));
            }
            @Override public void onFailure(Call<Models.Me> c, Throwable error) {
                if (b != null) { b.loading.setVisibility(View.GONE); b.error.setText(R.string.network_error); }
            }
        });
    }
    private void logout() {
        var client = ApiClient.get(requireContext());
        client.session().clear();
        client.events().reset();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
    @Override public void onDestroyView() { super.onDestroyView(); b = null; }
}
