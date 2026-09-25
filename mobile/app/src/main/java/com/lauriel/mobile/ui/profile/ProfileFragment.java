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
import retrofit2.*;

public class ProfileFragment extends Fragment {
    private FragmentProfileBinding b;
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater i,@Nullable ViewGroup c,@Nullable Bundle s){b=FragmentProfileBinding.inflate(i,c,false);return b.getRoot();}
    @Override public void onViewCreated(@NonNull View v,@Nullable Bundle s){
        b.retry.setOnClickListener(w->load());b.logout.setOnClickListener(w->confirmLogout());
        b.orders.setOnClickListener(w->((MainActivity)requireActivity()).showTab(R.id.nav_orders));
        b.edit.setOnClickListener(w->startActivity(new Intent(requireContext(),EditProfileActivity.class)));load();
    }
    @Override public void onResume(){super.onResume();if(b!=null)load();}
    private void load(){
        b.loading.setVisibility(View.VISIBLE);b.retry.setVisibility(View.GONE);b.error.setText("");
        ApiClient.get(requireContext()).api().profile().enqueue(new Callback<Models.Profile>(){
            @Override public void onResponse(Call<Models.Profile> c,Response<Models.Profile> r){
                if(b==null)return;b.loading.setVisibility(View.GONE);
                if(r.isSuccessful()&&r.body()!=null)render(r.body());else showError(ApiClient.get(requireContext()).error(r));
            }
            @Override public void onFailure(Call<Models.Profile> c,Throwable e){if(b!=null){b.loading.setVisibility(View.GONE);showError(getString(R.string.network_error));}}
        });
    }
    private void render(Models.Profile p){
        String name=p.fullName==null||p.fullName.isBlank()?"Khách hàng":p.fullName;
        b.avatar.setText(name.substring(0,1).toUpperCase(java.util.Locale.ROOT));
        b.fullName.setText(name);b.username.setText("@"+p.username);
        b.email.setText("Email: "+value(p.email));b.phone.setText("Số điện thoại: "+value(p.phone));
        b.address.setText("Địa chỉ: "+value(p.address));b.edit.setEnabled(true);
    }
    private String value(String v){return v==null||v.isBlank()?"Chưa cập nhật":v;}
    private void showError(String m){b.error.setText(m);b.retry.setVisibility(View.VISIBLE);b.edit.setEnabled(false);}
    private void confirmLogout(){new MaterialAlertDialogBuilder(requireContext()).setTitle("Đăng xuất?")
            .setMessage("Bạn cần đăng nhập lại để tiếp tục mua sắm.").setNegativeButton("Ở lại",null)
            .setPositiveButton(R.string.logout,(d,w)->logout()).show();}
    private void logout(){var c=ApiClient.get(requireContext());c.session().clear();c.events().reset();
        startActivity(new Intent(requireContext(),LoginActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));requireActivity().finish();}
    @Override public void onDestroyView(){super.onDestroyView();b=null;}
}
