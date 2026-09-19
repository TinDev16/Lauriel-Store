package com.lauriel.mobile.ui.auth;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.lauriel.mobile.data.api.ApiClient;
import com.lauriel.mobile.data.model.Models;
import com.lauriel.mobile.databinding.ActivitySplashBinding;
import com.lauriel.mobile.ui.MainActivity;
import retrofit2.*;
public class SplashActivity extends AppCompatActivity {
    private ActivitySplashBinding binding;
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);binding=ActivitySplashBinding.inflate(getLayoutInflater());setContentView(binding.getRoot());com.lauriel.mobile.ui.SystemBars.apply(binding.getRoot());
        var client=ApiClient.get(this);
        if(!client.session().hasCustomer()){open(LoginActivity.class);return;}
        new com.lauriel.mobile.data.repository.AuthRepository(client.api(),client.session()).me(new Callback<Models.Me>(){
            @Override public void onResponse(Call<Models.Me> call,Response<Models.Me> response){
                if(isFinishing())return;
                Models.Me me=response.body();
                if(response.isSuccessful() && me!=null && "CUSTOMER".equals(me.role)
                        && me.userId==client.session().userId())open(MainActivity.class);
                else {client.session().clear();open(LoginActivity.class);}
            }
            @Override public void onFailure(Call<Models.Me> call,Throwable error){
                if(!isFinishing()){binding.status.setText(com.lauriel.mobile.R.string.network_error);
                    binding.retry.setVisibility(android.view.View.VISIBLE);
                    binding.retry.setOnClickListener(v->recreate());}
            }
        });
    }
    private void open(Class<?> activity){startActivity(new Intent(this,activity));finish();}
}
