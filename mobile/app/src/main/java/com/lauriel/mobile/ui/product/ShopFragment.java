package com.lauriel.mobile.ui.product;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.lauriel.mobile.data.api.ApiClient;
import com.lauriel.mobile.data.model.Models;
import com.lauriel.mobile.data.repository.ProductRepository;
import com.lauriel.mobile.adapter.ProductAdapter;
import com.lauriel.mobile.databinding.FragmentShopBinding;
import java.util.*;
import retrofit2.*;
public class ShopFragment extends Fragment {
    private FragmentShopBinding b;private ShopViewModel vm;private ProductRepository products;
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup parent,@Nullable Bundle saved){
        b=FragmentShopBinding.inflate(inflater,parent,false);return b.getRoot();
    }
    @Override public void onViewCreated(@NonNull View view,@Nullable Bundle saved){
        products=new ProductRepository(ApiClient.get(requireContext()).api());
        vm=new ViewModelProvider(this).get(ShopViewModel.class);
        var adapter=new ProductAdapter(p->startActivity(new Intent(requireContext(),ProductDetailActivity.class).putExtra("productId",p.productId)));
        b.list.setLayoutManager(new GridLayoutManager(requireContext(),2));b.list.setAdapter(adapter);
        vm.state().observe(getViewLifecycleOwner(),state->{
            adapter.submit(state.products);b.loading.setVisibility(state.loading?View.VISIBLE:View.GONE);
            b.loadMore.setVisibility(state.hasMore && !state.loading && !state.products.isEmpty()?View.VISIBLE:View.GONE);
            b.empty.setVisibility(!state.loading && state.products.isEmpty()?View.VISIBLE:View.GONE);
            b.empty.setText(state.error==null?com.lauriel.mobile.R.string.empty_products:com.lauriel.mobile.R.string.network_error);
            b.error.setText(state.error==null?"":state.error);
        });
        b.search.setText(vm.keyword());b.search.setSingleLine(true);b.search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        b.search.setOnEditorActionListener((text,action,event)->{
            if(action==EditorInfo.IME_ACTION_SEARCH){vm.query(b.search.getText().toString(),vm.sizeId(),vm.colorId());return true;}
            return false;
        });
        b.filter.setOnClickListener(v->showFilters());
        b.loadMore.setOnClickListener(v->vm.loadMore());
        b.error.setOnClickListener(v->vm.retry());b.empty.setOnClickListener(v->vm.retry());
        vm.init(products);
    }
    private void showFilters(){
        products.sizes(new Callback<List<Models.Size>>(){
            @Override public void onResponse(Call<List<Models.Size>> c,Response<List<Models.Size>> r){
                if(!isAdded()||!r.isSuccessful()||r.body()==null){showError();return;}
                List<Models.Size> sizes=r.body();
                products.colors(new Callback<List<Models.Color>>(){
                    @Override public void onResponse(Call<List<Models.Color>> c2,Response<List<Models.Color>> cr){
                        if(!isAdded()||!cr.isSuccessful()||cr.body()==null){showError();return;}
                        showChoices(sizes,cr.body());
                    }
                    @Override public void onFailure(Call<List<Models.Color>> c2,Throwable e){showError();}
                });
            }
            @Override public void onFailure(Call<List<Models.Size>> c,Throwable e){showError();}
        });
    }
    private void showChoices(List<Models.Size> sizes,List<Models.Color> colors){
        List<String> labels=new ArrayList<>();labels.add("Tất cả size / màu");
        for(var size:sizes)labels.add("Kích cỡ: "+size.sizeName);
        for(var color:colors)labels.add("Màu: "+color.colorName);
        new MaterialAlertDialogBuilder(requireContext()).setTitle("Lọc sản phẩm")
                .setItems(labels.toArray(new String[0]),(dialog,which)->{
                    Long size=vm.sizeId(),color=vm.colorId();
                    if(which==0){size=null;color=null;}
                    else if(which<=sizes.size())size=sizes.get(which-1).sizeId;
                    else color=colors.get(which-1-sizes.size()).colorId;
                    vm.query(b.search.getText().toString(),size,color);
                }).setNeutralButton("Xóa lọc",(dialog,which)->vm.query(b.search.getText().toString(),null,null)).show();
    }
    private void showError(){if(b!=null)b.error.setText(com.lauriel.mobile.R.string.network_error);}
    @Override public void onDestroyView(){super.onDestroyView();b=null;}
}
