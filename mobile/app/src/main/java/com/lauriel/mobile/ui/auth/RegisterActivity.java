package com.lauriel.mobile.ui.auth;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.lauriel.mobile.R;
import com.lauriel.mobile.data.api.ApiClient;
import com.lauriel.mobile.data.model.Models;
import com.lauriel.mobile.databinding.ActivityRegisterBinding;
import retrofit2.*;
public class RegisterActivity extends AppCompatActivity {
    private ActivityRegisterBinding b;
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);b=ActivityRegisterBinding.inflate(getLayoutInflater());setContentView(b.getRoot());com.lauriel.mobile.ui.SystemBars.apply(b.getRoot());
        b.back.setOnClickListener(v->finish());b.register.setOnClickListener(v->register());
    }
    private void register(){
        String username=b.username.getText().toString().trim(), password=b.password.getText().toString(),
                fullName=b.fullName.getText().toString().trim(),email=b.email.getText().toString().trim(),
                phone=b.phone.getText().toString().trim();
        if(username.isEmpty()||password.length()<8||fullName.isEmpty()){
            b.error.setText("Điền tên đăng nhập, họ tên và mật khẩu tối thiểu 8 ký tự.");return;
        }
        b.register.setEnabled(false);b.loading.setVisibility(View.VISIBLE);b.error.setText("");
        var api=ApiClient.get(this);
        new com.lauriel.mobile.data.repository.AuthRepository(api.api(),api.session()).register(new Models.RegisterRequest(username,email.isEmpty()?null:email,password,fullName,phone.isEmpty()?null:phone),
                new Callback<Models.RegisterResponse>(){
                    @Override public void onResponse(Call<Models.RegisterResponse> call,Response<Models.RegisterResponse> response){
                        b.register.setEnabled(true);b.loading.setVisibility(View.GONE);
                        if(response.isSuccessful()){android.widget.Toast.makeText(RegisterActivity.this,
                                "Đăng ký thành công. Vui lòng đăng nhập.",android.widget.Toast.LENGTH_LONG).show();finish();}
                        else b.error.setText(api.error(response));
                    }
                    @Override public void onFailure(Call<Models.RegisterResponse> call,Throwable error){
                        b.register.setEnabled(true);b.loading.setVisibility(View.GONE);b.error.setText(R.string.network_error);
                    }
                });
    }
}
