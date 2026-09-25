package com.lauriel.mobile.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import com.lauriel.mobile.R;
import com.lauriel.mobile.adapter.ProductAdapter;
import com.lauriel.mobile.data.api.ApiClient;
import com.lauriel.mobile.data.model.Models;
import com.lauriel.mobile.data.repository.ProductRepository;
import com.lauriel.mobile.databinding.FragmentHomeBinding;
import com.lauriel.mobile.ui.MainActivity;
import com.lauriel.mobile.ui.common.GridSpacingDecoration;
import com.lauriel.mobile.ui.product.ProductDetailActivity;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding b;
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater i,@Nullable ViewGroup c,@Nullable Bundle s){b=FragmentHomeBinding.inflate(i,c,false);return b.getRoot();}
    @Override public void onViewCreated(@NonNull View v,@Nullable Bundle s){
        var repository=new ProductRepository(ApiClient.get(requireContext()).api());
        var adapter=new ProductAdapter(p->startActivity(new Intent(requireContext(),ProductDetailActivity.class).putExtra("productId",p.productId)));
        b.list.setLayoutManager(new GridLayoutManager(requireContext(),2)); b.list.addItemDecoration(new GridSpacingDecoration(dp(8),2)); b.list.setAdapter(adapter); b.list.setHasFixedSize(true);
        b.list.setLayoutAnimation(android.view.animation.AnimationUtils.loadLayoutAnimation(requireContext(),R.anim.layout_fade_in));
        b.shop.setOnClickListener(w->((MainActivity)requireActivity()).showTab(R.id.nav_shop));
        b.error.setOnClickListener(w->loadProducts(repository,adapter)); b.loadMore.setOnClickListener(w->((MainActivity)requireActivity()).showTab(R.id.nav_shop));
        loadProducts(repository,adapter);
        repository.promotions(new Callback<Models.Page<Models.Promotion>>(){
            @Override public void onResponse(Call<Models.Page<Models.Promotion>> c,Response<Models.Page<Models.Promotion>> r){
                if(b==null)return; if(r.isSuccessful()&&r.body()!=null&&r.body().content!=null&&!r.body().content.isEmpty()){
                    StringBuilder names=new StringBuilder(); for(var promo:r.body().content)names.append("• ").append(promo.promotionName).append(" · giảm ").append(promo.discountPercent).append("%\n"); b.promotions.setText(names.toString().trim());
                }else hidePromotions();
            }
            @Override public void onFailure(Call<Models.Page<Models.Promotion>> c,Throwable e){hidePromotions();}
        });
    }
    private void loadProducts(ProductRepository repository,ProductAdapter adapter){
        if(b==null)return; b.loading.setVisibility(View.VISIBLE);b.loadMore.setVisibility(View.GONE);b.error.setText("");b.empty.setVisibility(View.GONE);b.stateIcon.setVisibility(View.GONE);
        repository.list(0,null,null,null,null,new Callback<Models.Page<Models.Product>>(){
            @Override public void onResponse(Call<Models.Page<Models.Product>> c,Response<Models.Page<Models.Product>> r){
                if(b==null)return;b.loading.setVisibility(View.GONE);if(r.isSuccessful()&&r.body()!=null){adapter.submit(r.body().content);boolean empty=adapter.getItemCount()==0;b.empty.setVisibility(empty?View.VISIBLE:View.GONE);b.stateIcon.setVisibility(empty?View.VISIBLE:View.GONE);b.loadMore.setVisibility(r.body().totalPages>1?View.VISIBLE:View.GONE);}
                else{b.error.setText("Không thể tải sản phẩm. Chạm để thử lại.");b.stateIcon.setVisibility(View.VISIBLE);}
            }
            @Override public void onFailure(Call<Models.Page<Models.Product>> c,Throwable e){if(b!=null){b.loading.setVisibility(View.GONE);b.error.setText("Không thể kết nối máy chủ. Chạm để thử lại.");b.stateIcon.setVisibility(View.VISIBLE);}}
        });
    }
    private void hidePromotions(){if(b!=null){b.promotionTitle.setVisibility(View.GONE);b.promotions.setVisibility(View.GONE);}}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    @Override public void onDestroyView(){super.onDestroyView();b=null;}
}