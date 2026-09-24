package com.lauriel.mobile.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.lauriel.mobile.R;
import com.lauriel.mobile.data.api.ApiClient;
import com.lauriel.mobile.data.model.Models;
import com.lauriel.mobile.data.repository.AuthRepository;
import com.lauriel.mobile.databinding.FragmentProfileBinding;
import com.lauriel.mobile.ui.MainActivity;
import com.lauriel.mobile.ui.auth.LoginActivity;
import com.lauriel.mobile.util.Formatters;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {
    private FragmentProfileBinding b;
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater i,@Nullable ViewGroup c,@Nullable Bundle s){b=FragmentProfileBinding.inflate(i,c,false);return b.getRoot();}
    @Override public void onViewCreated(@NonNull View v,@Nullable Bundle s){b.retry.setOnClickListener(w->load());b.logout.setOnClickListener(w->confirmLogout());b.orders.setOnClickListener(w->((MainActivity)requireActivity()).showTab(R.id.nav_orders));load();}
    private void load(){b.loading.setVisibility(View.VISIBLE);b.retry.setVisibility(View.GONE);b.error.setText("");new AuthRepository(ApiClient.get(requireContext()).api(),ApiClient.get(requireContext()).session()).me(new Callback<Models.Me>(){
        @Override public void onResponse(Call<Models.Me> c,Response<Models.Me> r){if(b==null)return;b.loading.setVisibility(View.GONE);if(r.isSuccessful()&&r.body()!=null){String username=r.body().username==null?"Khách hàng":r.body().username;b.avatar.setText(username.substring(0,1).toUpperCase(java.util.Locale.ROOT));b.account.setText(username+"\n"+Formatters.role(r.body().role));}else showError(ApiClient.get(requireContext()).error(r));}
        @Override public void onFailure(Call<Models.Me> c,Throwable error){if(b!=null){b.loading.setVisibility(View.GONE);showError(getString(R.string.network_error));}}
    });}
    private void showError(String message){b.error.setText(message);b.retry.setVisibility(View.VISIBLE);}
    private void confirmLogout(){new MaterialAlertDialogBuilder(requireContext()).setTitle("Đăng xuất?").setMessage("Bạn cần đăng nhập lại để tiếp tục mua sắm.").setNegativeButton("Ở lại",null).setPositiveButton(R.string.logout,(dialog,which)->logout()).show();}
    private void logout(){var client=ApiClient.get(requireContext());client.session().clear();client.events().reset();Intent intent=new Intent(requireContext(),LoginActivity.class);intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);startActivity(intent);requireActivity().finish();}
    @Override public void onDestroyView(){super.onDestroyView();b=null;}
}