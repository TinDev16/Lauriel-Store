package com.lauriel.mobile.ui.product;
import androidx.lifecycle.*;
import com.lauriel.mobile.data.model.Models;
import com.lauriel.mobile.data.repository.ProductRepository;
import java.util.*;
import retrofit2.*;
public final class ShopViewModel extends ViewModel {
    public static final class State {
        public final List<Models.Product> products; public final boolean loading,hasMore; public final String error;
        State(List<Models.Product> products,boolean loading,boolean hasMore,String error){
            this.products=products;this.loading=loading;this.hasMore=hasMore;this.error=error;
        }
    }
    private final MutableLiveData<State> state=new MutableLiveData<>(new State(Collections.emptyList(),false,true,null));
    private final List<Models.Product> products=new ArrayList<>();
    private ProductRepository repository;private Call<Models.Page<Models.Product>> active;
    private int page,generation;private boolean loading,hasMore=true;
    private String keyword;private Long sizeId,colorId;
    public LiveData<State> state(){return state;}
    public void init(ProductRepository repository){this.repository=repository;if(products.isEmpty())loadMore();}
    public void query(String keyword,Long sizeId,Long colorId){
        this.keyword=keyword==null?null:keyword.trim();this.sizeId=sizeId;this.colorId=colorId;
        generation++;if(active!=null)active.cancel();products.clear();page=0;hasMore=true;loading=false;
        publish(null);loadMore();
    }
    public String keyword(){return keyword;} public Long sizeId(){return sizeId;}public Long colorId(){return colorId;}
    public void retry(){if(!loading)loadMore();}
    public void loadMore(){
        if(repository==null||loading||!hasMore)return;
        loading=true;publish(null);int myGeneration=generation;
        active=repository.list(page,keyword,sizeId,colorId,new Callback<Models.Page<Models.Product>>(){
            @Override public void onResponse(Call<Models.Page<Models.Product>> call,Response<Models.Page<Models.Product>> response){
                if(myGeneration!=generation)return;loading=false;
                var result=response.body();
                if(!response.isSuccessful()||result==null){publish("Không thể tải sản phẩm ("+response.code()+").");return;}
                Set<Long> ids=new HashSet<>();for(var old:products)ids.add(old.productId);
                if(result.content!=null)for(var product:result.content)if(ids.add(product.productId))products.add(product);
                page=result.page+1;hasMore=page<result.totalPages;publish(null);
            }
            @Override public void onFailure(Call<Models.Page<Models.Product>> call,Throwable error){
                if(myGeneration!=generation || call.isCanceled())return;loading=false;
                publish("Không thể kết nối máy chủ. Vui lòng kiểm tra kết nối.");
            }
        });
    }
    private void publish(String error){state.setValue(new State(Collections.unmodifiableList(new ArrayList<>(products)),loading,hasMore,error));}
    @Override protected void onCleared(){if(active!=null)active.cancel();}
}
