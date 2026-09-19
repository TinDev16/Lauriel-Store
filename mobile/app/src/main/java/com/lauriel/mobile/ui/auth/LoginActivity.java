package com.lauriel.mobile.ui.auth;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.lauriel.mobile.data.api.ApiClient;
import com.lauriel.mobile.data.model.Models;
import com.lauriel.mobile.databinding.ActivityLoginBinding;
import com.lauriel.mobile.ui.MainActivity;
import retrofit2.*;
public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding b;
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);b=ActivityLoginBinding.inflate(getLayoutInflater());setContentView(b.getRoot());com.lauriel.mobile.ui.SystemBars.apply(b.getRoot());
        b.register.setOnClickListener(v->startActivity(new Intent(this,RegisterActivity.class)));
        b.login.setOnClickListener(v->login());
    }
    private void login(){
        String username=b.username.getText().toString().trim(),password=b.password.getText().toString();
        if(username.isEmpty()||password.isEmpty()){b.error.setText("Nhập tên đăng nhập và mật khẩu.");return;}
        b.login.setEnabled(false);b.loading.setVisibility(View.VISIBLE);b.error.setText("");
        var client=ApiClient.get(this);
        new com.lauriel.mobile.data.repository.AuthRepository(client.api(),client.session()).login(username,password,new Callback<Models.LoginResponse>(){
            @Override public void onResponse(Call<Models.LoginResponse> call,Response<Models.LoginResponse> response){
                b.login.setEnabled(true);b.loading.setVisibility(View.GONE);
                Models.LoginResponse result=response.body();
                if(response.isSuccessful() && result!=null){
                    if(!"CUSTOMER".equals(result.role)){
                        b.error.setText("Tài khoản này không dành cho ứng dụng khách hàng.");return;
                    }
                    try{client.session().save(result);}catch(IllegalArgumentException ex){b.error.setText("Dữ liệu đăng nhập không hợp lệ.");return;}
                    client.events().reset();startActivity(new Intent(LoginActivity.this,MainActivity.class));finish();
                }else b.error.setText(client.error(response));
            }
            @Override public void onFailure(Call<Models.LoginResponse> call,Throwable error){
                b.login.setEnabled(true);b.loading.setVisibility(View.GONE);b.error.setText(com.lauriel.mobile.R.string.network_error);
            }
        });
    }
}
