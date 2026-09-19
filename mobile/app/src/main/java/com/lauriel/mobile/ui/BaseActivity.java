package com.lauriel.mobile.ui;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.lauriel.mobile.data.api.ApiClient;
import com.lauriel.mobile.ui.auth.LoginActivity;
public abstract class BaseActivity extends AppCompatActivity {
    @Override protected void onCreate(@Nullable Bundle state){
        super.onCreate(state);
        ApiClient.get(this).events().expired().observe(this,expired -> {
            if(Boolean.TRUE.equals(expired)){
                ApiClient.get(this).events().reset();
                Intent intent=new Intent(this,LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);finish();
            }
        });
    }
}
