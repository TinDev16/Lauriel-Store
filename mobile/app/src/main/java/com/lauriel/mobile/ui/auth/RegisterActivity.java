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
import retrofit2.*;

public class RegisterActivity extends AppCompatActivity {
    private ActivityRegisterBinding b; private AuthVideoIntro intro;
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);b=ActivityRegisterBinding.inflate(getLayoutInflater());setContentView(b.getRoot());SystemBars.apply(b.getRoot());
        intro=new AuthVideoIntro(this,b.video,b.formContainer,state);
        b.back.setOnClickListener(v->finish());b.register.setOnClickListener(v->register());
        b.password.setOnEditorActionListener((v,a,e)->{if(a==EditorInfo.IME_ACTION_DONE){register();return true;}return false;});
    }
    @Override protected void onSaveInstanceState(Bundle out){intro.save(out);super.onSaveInstanceState(out);}
    @Override protected void onDestroy(){if(intro!=null)intro.destroy();super.onDestroy();}
    private void register(){
        String username=text(b.username),password=raw(b.password),fullName=text(b.fullName),email=text(b.email),phone=text(b.phone);
        clearErrors();boolean valid=true;
        if(username.isEmpty()){b.usernameLayout.setError("Vui lòng nhập tên đăng nhập");valid=false;}
        if(fullName.isEmpty()){b.fullNameLayout.setError("Vui lòng nhập họ tên");valid=false;}
        if(!email.isEmpty()&&!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()){b.emailLayout.setError("Email chưa đúng định dạng");valid=false;}
        if(password.length()<8){b.passwordLayout.setError("Mật khẩu cần ít nhất 8 ký tự");valid=false;}if(!valid)return;
        setLoading(true);var api=ApiClient.get(this);new AuthRepository(api.api(),api.session()).register(
                new Models.RegisterRequest(username,email.isEmpty()?null:email,password,fullName,phone.isEmpty()?null:phone),
                new Callback<Models.RegisterResponse>(){
            @Override public void onResponse(Call<Models.RegisterResponse> c,Response<Models.RegisterResponse> r){setLoading(false);
                if(r.isSuccessful()){Snackbar bar=Snackbar.make(b.getRoot(),"Đăng ký thành công",Snackbar.LENGTH_LONG).setAction("Đăng nhập",v->finish());
                    bar.addCallback(new BaseTransientBottomBar.BaseCallback<Snackbar>(){@Override public void onDismissed(Snackbar x,int event){if(!isFinishing())finish();}});bar.show();
                }else b.error.setText(api.error(r));}
            @Override public void onFailure(Call<Models.RegisterResponse> c,Throwable e){setLoading(false);b.error.setText(R.string.network_error);}
        });
    }
    private void clearErrors(){b.usernameLayout.setError(null);b.fullNameLayout.setError(null);b.emailLayout.setError(null);b.phoneLayout.setError(null);b.passwordLayout.setError(null);b.error.setText("");}
    private void setLoading(boolean v){b.register.setEnabled(!v);b.back.setEnabled(!v);b.loading.setVisibility(v?View.VISIBLE:View.GONE);b.register.setText(v?"Đang tạo tài khoản…":getString(R.string.register));}
    private String text(android.widget.EditText f){return raw(f).trim();}private String raw(android.widget.EditText f){return f.getText()==null?"":f.getText().toString();}
}
