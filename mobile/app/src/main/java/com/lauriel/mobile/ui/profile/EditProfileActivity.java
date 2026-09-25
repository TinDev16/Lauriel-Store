package com.lauriel.mobile.ui.profile;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import com.google.android.material.snackbar.Snackbar;
import com.lauriel.mobile.R;
import com.lauriel.mobile.data.api.ApiClient;
import com.lauriel.mobile.data.model.Models;
import com.lauriel.mobile.databinding.ActivityEditProfileBinding;
import com.lauriel.mobile.ui.BaseActivity;
import retrofit2.*;

public class EditProfileActivity extends BaseActivity {
    private ActivityEditProfileBinding b; private boolean saving;
    @Override protected void onCreate(Bundle s){super.onCreate(s);b=ActivityEditProfileBinding.inflate(getLayoutInflater());setContentView(b.getRoot());
        com.lauriel.mobile.ui.SystemBars.apply(b.getRoot());b.back.setOnClickListener(v->finish());b.save.setOnClickListener(v->save());load();}
    private void load(){setLoading(true);ApiClient.get(this).api().profile().enqueue(new Callback<Models.Profile>(){
        @Override public void onResponse(Call<Models.Profile> c,Response<Models.Profile> r){setLoading(false);if(r.isSuccessful()&&r.body()!=null){var p=r.body();b.fullName.setText(p.fullName);b.email.setText(p.email);b.phone.setText(p.phone);b.address.setText(p.address);}else b.error.setText(ApiClient.get(EditProfileActivity.this).error(r));}
        @Override public void onFailure(Call<Models.Profile> c,Throwable e){setLoading(false);b.error.setText(R.string.network_error);}
    });}
    private void save(){if(saving)return;String name=text(b.fullName),email=text(b.email),phone=text(b.phone),address=text(b.address);
        b.fullNameLayout.setError(null);b.emailLayout.setError(null);b.error.setText("");
        if(name.isEmpty()){b.fullNameLayout.setError("Vui lòng nhập họ tên");return;}
        if(!email.isEmpty()&&!Patterns.EMAIL_ADDRESS.matcher(email).matches()){b.emailLayout.setError("Email không hợp lệ");return;}
        saving=true;setLoading(true);ApiClient.get(this).api().updateProfile(new Models.ProfileUpdate(name,email,phone,address)).enqueue(new Callback<Models.Profile>(){
            @Override public void onResponse(Call<Models.Profile> c,Response<Models.Profile> r){saving=false;setLoading(false);
                if(r.isSuccessful()&&r.body()!=null){Snackbar.make(b.getRoot(),"Cập nhật thông tin thành công",Snackbar.LENGTH_LONG).show();b.getRoot().postDelayed(()->finish(),900);}
                else b.error.setText(ApiClient.get(EditProfileActivity.this).error(r));}
            @Override public void onFailure(Call<Models.Profile> c,Throwable e){saving=false;setLoading(false);b.error.setText(R.string.network_error);}
        });}
    private void setLoading(boolean value){b.loading.setVisibility(value?View.VISIBLE:View.GONE);b.save.setEnabled(!value);}
    private String text(android.widget.EditText v){return v.getText()==null?"":v.getText().toString().trim();}
}
