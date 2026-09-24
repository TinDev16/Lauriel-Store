package com.lauriel.mobile.ui.auth;

import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.lauriel.mobile.R;
import com.lauriel.mobile.data.api.ApiClient;
import com.lauriel.mobile.data.model.Models;
import com.lauriel.mobile.data.repository.AuthRepository;
import com.lauriel.mobile.databinding.ActivityRegisterBinding;
import com.lauriel.mobile.ui.SystemBars;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {
    private ActivityRegisterBinding b;
    @Override protected void onCreate(Bundle state){super.onCreate(state);b=ActivityRegisterBinding.inflate(getLayoutInflater());setContentView(b.getRoot());SystemBars.apply(b.getRoot());b.back.setOnClickListener(v->finish());b.register.setOnClickListener(v->register());b.password.setOnEditorActionListener((v,action,event)->{if(action==EditorInfo.IME_ACTION_DONE){register();return true;}return false;});}
    private void register(){
        String username=text(b.username),password=raw(b.password),fullName=text(b.fullName),email=text(b.email),phone=text(b.phone);clearErrors();boolean valid=true;
        if(username.isEmpty()){b.usernameLayout.setError("Vui lòng nhập tên đăng nhập");valid=false;}if(fullName.isEmpty()){b.fullNameLayout.setError("Vui lòng nhập họ tên");valid=false;}if(!email.isEmpty()&&!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()){b.emailLayout.setError("Email chưa đúng định dạng");valid=false;}if(password.length()<8){b.passwordLayout.setError("Mật khẩu cần ít nhất 8 ký tự");valid=false;}if(!valid)return;
        setLoading(true);var api=ApiClient.get(this);new AuthRepository(api.api(),api.session()).register(new Models.RegisterRequest(username,email.isEmpty()?null:email,password,fullName,phone.isEmpty()?null:phone),new Callback<Models.RegisterResponse>(){
            @Override public void onResponse(Call<Models.RegisterResponse> call,Response<Models.RegisterResponse> response){setLoading(false);if(response.isSuccessful()){Snackbar bar=Snackbar.make(b.getRoot(),"Đăng ký thành công",Snackbar.LENGTH_LONG).setAction("Đăng nhập",v->finish());bar.addCallback(new BaseTransientBottomBar.BaseCallback<Snackbar>(){@Override public void onDismissed(Snackbar transientBottomBar,int event){if(!isFinishing())finish();}});bar.show();}else b.error.setText(api.error(response));}
            @Override public void onFailure(Call<Models.RegisterResponse> call,Throwable error){setLoading(false);b.error.setText(R.string.network_error);}
        });
    }
    private void clearErrors(){b.usernameLayout.setError(null);b.fullNameLayout.setError(null);b.emailLayout.setError(null);b.phoneLayout.setError(null);b.passwordLayout.setError(null);b.error.setText("");}
    private void setLoading(boolean loading){b.register.setEnabled(!loading);b.back.setEnabled(!loading);b.loading.setVisibility(loading?View.VISIBLE:View.GONE);b.register.setText(loading?"Đang tạo tài khoản…":getString(R.string.register));}
    private String text(android.widget.EditText field){return raw(field).trim();}private String raw(android.widget.EditText field){return field.getText()==null?"":field.getText().toString();}
}