package com.lauriel.mobile.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import com.lauriel.mobile.data.model.Models;

public final class SessionManager {
    private final SharedPreferences prefs;
    public SessionManager(Context context){prefs=context.getApplicationContext().getSharedPreferences("lauriel_session",Context.MODE_PRIVATE);}
    public synchronized void save(Models.LoginResponse login){
        if(login==null || login.token==null || !"CUSTOMER".equals(login.role) || login.userId<=0)
            throw new IllegalArgumentException("Yêu cầu tài khoản khách hàng");
        prefs.edit().putString("token",login.token).putLong("userId",login.userId)
                .putString("username",login.username).putString("role",login.role).apply();
    }
    public synchronized String token(){return prefs.getString("token",null);}
    public synchronized long userId(){return prefs.getLong("userId",-1);}
    public synchronized String username(){return prefs.getString("username","");}
    public synchronized String role(){return prefs.getString("role","");}
    public synchronized boolean hasCustomer(){return token()!=null && userId()>0 && "CUSTOMER".equals(role());}
    public synchronized void clear(){prefs.edit().clear().apply();}
}
