package com.lauriel.mobile.data.repository;

import com.lauriel.mobile.data.api.StoreApi;
import com.lauriel.mobile.data.model.Models;
import java.util.List;
import retrofit2.*;

public final class ProductRepository {
    private final StoreApi api;
    public ProductRepository(StoreApi api){this.api=api;}
    public Call<Models.Page<Models.Product>> list(int page,String keyword,Long categoryId,Long sizeId,Long colorId,Callback<Models.Page<Models.Product>> cb){
        Call<Models.Page<Models.Product>> call=api.products(page,12,keyword,categoryId,sizeId,colorId,"createdAt","desc");call.enqueue(cb);return call;
    }
    public void detail(long id,Callback<Models.Product> cb){api.product(id).enqueue(cb);}
    public void related(long id,Callback<List<Models.Product>> cb){api.related(id,8).enqueue(cb);}
    public void promotions(Callback<Models.Page<Models.Promotion>> cb){api.activePromotions(0,12).enqueue(cb);}
    public void categories(Callback<List<Models.Category>> cb){api.categories().enqueue(cb);}
    public void sizes(Callback<List<Models.Size>> cb){api.sizes().enqueue(cb);}
    public void colors(Callback<List<Models.Color>> cb){api.colors().enqueue(cb);}
}
