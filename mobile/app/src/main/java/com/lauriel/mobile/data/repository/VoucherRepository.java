package com.lauriel.mobile.data.repository;
import com.lauriel.mobile.data.api.StoreApi;
import com.lauriel.mobile.data.model.Models;
import java.util.List;
import retrofit2.*;
public final class VoucherRepository {
    private final StoreApi api;
    public VoucherRepository(StoreApi api){this.api=api;}
    public void preview(String code,List<Models.Item> items,Callback<Models.VoucherPreview> cb){
        api.preview(new Models.VoucherRequest(code,items)).enqueue(cb);
    }
}
