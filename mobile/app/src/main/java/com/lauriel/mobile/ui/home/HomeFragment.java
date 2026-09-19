package com.lauriel.mobile.ui.home;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import com.lauriel.mobile.R;
import com.lauriel.mobile.adapter.ProductAdapter;
import com.lauriel.mobile.data.api.ApiClient;
import com.lauriel.mobile.data.model.Models;
import com.lauriel.mobile.data.repository.ProductRepository;
import com.lauriel.mobile.databinding.FragmentHomeBinding;
import com.lauriel.mobile.ui.MainActivity;
import com.lauriel.mobile.ui.product.ProductDetailActivity;
import retrofit2.*;
public class HomeFragment extends Fragment {
    private FragmentHomeBinding b;
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater i,@Nullable ViewGroup c,@Nullable Bundle s){
        b=FragmentHomeBinding.inflate(i,c,false);return b.getRoot();
    }
    @Override public void onViewCreated(@NonNull View v,@Nullable Bundle s){
        var repository=new ProductRepository(ApiClient.get(requireContext()).api());
        var adapter=new ProductAdapter(p->startActivity(new Intent(requireContext(),ProductDetailActivity.class).putExtra("productId",p.productId)));
        b.list.setLayoutManager(new GridLayoutManager(requireContext(),2));b.list.setAdapter(adapter);
        b.shop.setOnClickListener(w->((MainActivity)requireActivity()).showTab(R.id.nav_shop));
        b.error.setOnClickListener(w->requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.container,new HomeFragment()).commit());
        b.loadMore.setOnClickListener(w->((MainActivity)requireActivity()).showTab(R.id.nav_shop));
        b.loading.setVisibility(View.VISIBLE);
        repository.list(0,null,null,null,new Callback<Models.Page<Models.Product>>(){
            @Override public void onResponse(Call<Models.Page<Models.Product>> c,Response<Models.Page<Models.Product>> r){
                if(b==null)return;b.loading.setVisibility(View.GONE);
                if(r.isSuccessful()&&r.body()!=null){adapter.submit(r.body().content);
                    b.empty.setVisibility(adapter.getItemCount()==0?View.VISIBLE:View.GONE);
                    b.loadMore.setVisibility(r.body().totalPages>1?View.VISIBLE:View.GONE);}
                else{b.error.setText("Không thể tải sản phẩm.");b.empty.setVisibility(View.VISIBLE);}
            }
            @Override public void onFailure(Call<Models.Page<Models.Product>> c,Throwable e){
                if(b!=null){b.loading.setVisibility(View.GONE);b.error.setText(R.string.network_error);}
            }
        });
        repository.promotions(new Callback<Models.Page<Models.Promotion>>(){
            @Override public void onResponse(Call<Models.Page<Models.Promotion>> c,Response<Models.Page<Models.Promotion>> r){
                if(b==null)return;
                if(r.isSuccessful()&&r.body()!=null&&r.body().content!=null&&!r.body().content.isEmpty()){
                    StringBuilder names=new StringBuilder();
                    for(var promo:r.body().content)names.append("• ").append(promo.promotionName)
                            .append(" · giảm ").append(promo.discountPercent).append("%\n");
                    b.promotions.setText(names.toString().trim());
                }else{b.promotionTitle.setVisibility(View.GONE);b.promotions.setVisibility(View.GONE);}
            }
            @Override public void onFailure(Call<Models.Page<Models.Promotion>> c,Throwable e){
                if(b!=null){b.promotionTitle.setVisibility(View.GONE);b.promotions.setVisibility(View.GONE);}
            }
        });
    }
    @Override public void onDestroyView(){super.onDestroyView();b=null;}
}
