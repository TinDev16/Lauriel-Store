package com.lauriel.mobile.data.api;

import android.content.Context;
import com.google.gson.Gson;
import com.lauriel.mobile.BuildConfig;
import com.lauriel.mobile.data.local.SessionManager;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.*;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {
    private static volatile ApiClient instance;
    private final StoreApi api;
    private final AuthEvents events=new AuthEvents();
    private final SessionManager session;
    private final Gson gson=new Gson();
    private ApiClient(Context context){
        session=new SessionManager(context);
        api=createApi(BuildConfig.API_BASE_URL,session::token,() -> {
            session.clear();
            events.notifyExpired();
        });
    }
    public static ApiClient get(Context context){
        if(instance==null)synchronized(ApiClient.class){if(instance==null)instance=new ApiClient(context.getApplicationContext());}
        return instance;
    }
    static StoreApi createApi(String baseUrl,java.util.function.Supplier<String> token,
            Runnable unauthorized) {
        OkHttpClient client=new OkHttpClient.Builder()
                .connectTimeout(15,TimeUnit.SECONDS).readTimeout(20,TimeUnit.SECONDS)
                .addInterceptor(chain -> {
                    String value=token.get();
                    Request original=chain.request();
                    Request request=value==null?original:original.newBuilder()
                            .header("Authorization","Bearer "+value).build();
                    Response response=chain.proceed(request);
                    if(response.code()==401 && value!=null && value.equals(token.get())) unauthorized.run();
                    return response;
                }).build();
        return new Retrofit.Builder().baseUrl(baseUrl).client(client)
                .addConverterFactory(GsonConverterFactory.create()).build().create(StoreApi.class);
    }
    public StoreApi api(){return api;}
    public SessionManager session(){return session;}
    public AuthEvents events(){return events;}
    public String error(retrofit2.Response<?> response){
        if(response==null)return "Không nhận được phản hồi từ máy chủ.";
        if(response.code()==401)return "Phiên đăng nhập đã hết hạn.";
        if(response.code()==403)return "Bạn không có quyền thực hiện thao tác này.";
        if(response.code()==404)return "Không tìm thấy dữ liệu.";
        try {
            okhttp3.ResponseBody body=response.errorBody();
            if(body!=null){
                com.lauriel.mobile.data.model.Models.ApiError error=gson.fromJson(body.string(),
                        com.lauriel.mobile.data.model.Models.ApiError.class);
                if(error!=null && error.message!=null && !error.message.trim().isEmpty())return error.message;
            }
        } catch(IOException|RuntimeException ignored){}
        if(response.code()==409)return "Dữ liệu đã thay đổi. Vui lòng tải lại và thử lại.";
        if(response.code()>=500)return "Máy chủ đang gặp lỗi. Vui lòng thử lại.";
        return "Yêu cầu không hợp lệ ("+response.code()+").";
    }
}
