package com.lauriel.mobile.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import androidx.appcompat.app.AppCompatActivity;
import com.lauriel.mobile.R;
import com.lauriel.mobile.data.api.ApiClient;
import com.lauriel.mobile.data.model.Models;
import com.lauriel.mobile.data.repository.AuthRepository;
import com.lauriel.mobile.databinding.ActivityLoginBinding;
import com.lauriel.mobile.ui.MainActivity;
import com.lauriel.mobile.ui.SystemBars;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding b;
    @Override protected void onCreate(Bundle state){super.onCreate(state);b=ActivityLoginBinding.inflate(getLayoutInflater());setContentView(b.getRoot());SystemBars.apply(b.getRoot());b.register.setOnClickListener(v->startActivity(new Intent(this,RegisterActivity.class)));b.login.setOnClickListener(v->login());b.password.setOnEditorActionListener((v,action,event)->{if(action==EditorInfo.IME_ACTION_DONE){login();return true;}return false;});}
    private void login(){
        String username=text(b.username),password=b.password.getText()==null?"":b.password.getText().toString();b.usernameLayout.setError(null);b.passwordLayout.setError(null);b.error.setText("");boolean valid=true;
        if(username.isEmpty()){b.usernameLayout.setError("Vui lòng nhập tên đăng nhập");valid=false;}if(password.isEmpty()){b.passwordLayout.setError("Vui lòng nhập mật khẩu");valid=false;}if(!valid)return;
        setLoading(true);var client=ApiClient.get(this);new AuthRepository(client.api(),client.session()).login(username,password,new Callback<Models.LoginResponse>(){
            @Override public void onResponse(Call<Models.LoginResponse> call,Response<Models.LoginResponse> response){setLoading(false);Models.LoginResponse result=response.body();if(response.isSuccessful()&&result!=null){if(!"CUSTOMER".equals(result.role)){b.error.setText("Tài khoản này không dành cho ứng dụng khách hàng.");return;}try{client.session().save(result);}catch(IllegalArgumentException ex){b.error.setText("Dữ liệu đăng nhập không hợp lệ.");return;}client.events().reset();startActivity(new Intent(LoginActivity.this,MainActivity.class));finish();}else b.error.setText(client.error(response));}
            @Override public void onFailure(Call<Models.LoginResponse> call,Throwable error){setLoading(false);b.error.setText(R.string.network_error);}
        });
    }
    private void setLoading(boolean loading){b.login.setEnabled(!loading);b.register.setEnabled(!loading);b.loading.setVisibility(loading?View.VISIBLE:View.GONE);b.login.setText(loading?"Đang đăng nhập…":getString(R.string.login));}
    private String text(android.widget.EditText field){return field.getText()==null?"":field.getText().toString().trim();}
}