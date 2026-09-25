package com.lauriel.mobile.ui.product;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.lauriel.mobile.data.model.Models;
import com.lauriel.mobile.data.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public final class ShopViewModel extends ViewModel {
    public static final class State {
        public final List<Models.Product> products; public final boolean loading, hasMore; public final String error;
        State(List<Models.Product> products, boolean loading, boolean hasMore, String error) { this.products=products; this.loading=loading; this.hasMore=hasMore; this.error=error; }
    }
    private final MutableLiveData<State> state = new MutableLiveData<>(new State(Collections.emptyList(), false, true, null));
    private final List<Models.Product> products = new ArrayList<>();
    private ProductRepository repository; private Call<Models.Page<Models.Product>> active;
    private int page, generation; private boolean loading, hasMore = true;
    private String keyword, sort = "DEFAULT"; private Long categoryId, sizeId, colorId;
    public LiveData<State> state() { return state; }
    public void init(ProductRepository repository) { this.repository = repository; if (products.isEmpty()) loadMore(); }
    public void query(String keyword, Long categoryId, Long sizeId, Long colorId, String sort) {
        String nextKeyword = keyword == null ? null : keyword.trim();
        String nextSort = sort == null ? "DEFAULT" : sort;
        boolean serverFilterChanged = !java.util.Objects.equals(this.keyword,nextKeyword)
                || !java.util.Objects.equals(this.categoryId,categoryId)
                || !java.util.Objects.equals(this.sizeId,sizeId) || !java.util.Objects.equals(this.colorId,colorId);
        this.keyword=nextKeyword; this.categoryId=categoryId; this.sizeId=sizeId; this.colorId=colorId; this.sort=nextSort;
        if (!serverFilterChanged) { publish(null); return; }
        generation++; if (active != null) active.cancel(); products.clear(); page=0; hasMore=true; loading=false; publish(null); loadMore();
    }
    public String keyword() { return keyword; } public Long categoryId() { return categoryId; } public Long sizeId() { return sizeId; } public Long colorId() { return colorId; } public String sort() { return sort; }
    public void retry() { if (!loading) loadMore(); }
    public void loadMore() {
        if (repository == null || loading || !hasMore) return;
        loading=true; publish(null); int requestGeneration=generation;
        active=repository.list(page,keyword,categoryId,sizeId,colorId,new Callback<Models.Page<Models.Product>>() {
            @Override public void onResponse(Call<Models.Page<Models.Product>> call, Response<Models.Page<Models.Product>> response) {
                if (requestGeneration != generation) return; loading=false; var result=response.body();
                if (!response.isSuccessful() || result == null) { publish("Không thể tải sản phẩm. Vui lòng thử lại."); return; }
                Set<Long> ids=new HashSet<>(); for (var old:products) ids.add(old.productId);
                if (result.content != null) for (var product:result.content) if (ids.add(product.productId)) products.add(product);
                page=result.page+1; hasMore=page<result.totalPages; publish(null);
            }
            @Override public void onFailure(Call<Models.Page<Models.Product>> call, Throwable error) {
                if (requestGeneration != generation || call.isCanceled()) return; loading=false; publish("Không thể kết nối máy chủ.");
            }
        });
    }
    private void publish(String error) {
        List<Models.Product> visible=new ArrayList<>(products);
        Comparator<Models.Product> price=Comparator.comparing(p -> p.price == null ? BigDecimal.ZERO : p.price);
        if ("PRICE_ASC".equals(sort)) visible.sort(price); else if ("PRICE_DESC".equals(sort)) visible.sort(price.reversed());
        state.setValue(new State(Collections.unmodifiableList(visible),loading,hasMore,error));
    }
    @Override protected void onCleared() { if (active != null) active.cancel(); }
}