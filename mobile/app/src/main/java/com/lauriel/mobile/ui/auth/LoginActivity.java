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
import retrofit2.*;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding b;
    private AuthVideoIntro intro;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        b = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        SystemBars.apply(b.getRoot());
        intro = new AuthVideoIntro(this, b.video, b.formContainer, state);
        b.register.setOnClickListener(v ->
            startActivity(new Intent(this, RegisterActivity.class))
        );
        b.login.setOnClickListener(v -> login());
        b.password.setOnEditorActionListener((v, a, e) -> {
            if (a == EditorInfo.IME_ACTION_DONE) {
                login();
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        intro.save(out);
        super.onSaveInstanceState(out);
    }

    @Override
    protected void onDestroy() {
        if (intro != null) intro.destroy();
        super.onDestroy();
    }

    private void login() {
        String username = text(b.username),
            password =
                b.password.getText() == null
                    ? ""
                    : b.password.getText().toString();
        b.usernameLayout.setError(null);
        b.passwordLayout.setError(null);
        b.error.setText("");
        boolean valid = true;
        if (username.isEmpty()) {
            b.usernameLayout.setError("Vui lòng nhập tên đăng nhập");
            valid = false;
        }
        if (password.isEmpty()) {
            b.passwordLayout.setError("Vui lòng nhập mật khẩu");
            valid = false;
        }
        if (!valid) return;
        setLoading(true);
        var client = ApiClient.get(this);
        new AuthRepository(client.api(), client.session()).login(
            username,
            password,
            new Callback<Models.LoginResponse>() {
                @Override
                public void onResponse(
                    Call<Models.LoginResponse> c,
                    Response<Models.LoginResponse> r
                ) {
                    setLoading(false);
                    var result = r.body();
                    if (r.isSuccessful() && result != null) {
                        if (!"CUSTOMER".equals(result.role)) {
                            b.error.setText(
                                "Tài khoản này không dành cho ứng dụng khách hàng."
                            );
                            return;
                        }
                        try {
                            client.session().save(result);
                        } catch (IllegalArgumentException e) {
                            b.error.setText("Dữ liệu đăng nhập không hợp lệ.");
                            return;
                        }
                        client.events().reset();
                        startActivity(
                            new Intent(LoginActivity.this, MainActivity.class)
                        );
                        finish();
                    } else b.error.setText(client.error(r));
                }

                @Override
                public void onFailure(
                    Call<Models.LoginResponse> c,
                    Throwable e
                ) {
                    setLoading(false);
                    b.error.setText(R.string.network_error);
                }
            }
        );
    }

    private void setLoading(boolean v) {
        b.login.setEnabled(!v);
        b.register.setEnabled(!v);
        b.loading.setVisibility(v ? View.VISIBLE : View.GONE);
        b.login.setText(v ? "Đang đăng nhập…" : getString(R.string.login));
    }

    private String text(android.widget.EditText f) {
        return f.getText() == null ? "" : f.getText().toString().trim();
    }
}
