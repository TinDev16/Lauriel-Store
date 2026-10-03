package com.lauriel.mobile.ui.cart;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.snackbar.Snackbar;
import com.lauriel.mobile.R;
import com.lauriel.mobile.adapter.CartAdapter;
import com.lauriel.mobile.data.api.ApiClient;
import com.lauriel.mobile.data.local.CartStore;
import com.lauriel.mobile.data.model.Models;
import com.lauriel.mobile.data.repository.ProductRepository;
import com.lauriel.mobile.databinding.FragmentCartBinding;
import com.lauriel.mobile.ui.MainActivity;
import com.lauriel.mobile.ui.checkout.CheckoutActivity;
import com.lauriel.mobile.util.Formatters;
import java.util.HashSet;
import java.util.Set;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CartFragment extends Fragment {
    private FragmentCartBinding b;private CartStore cart;private CartAdapter adapter;
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater i,@Nullable ViewGroup c,@Nullable Bundle s){b=FragmentCartBinding.inflate(i,c,false);return b.getRoot();}
    @Override public void onViewCreated(@NonNull View view,@Nullable Bundle saved){
        cart=new CartStore(requireContext(),ApiClient.get(requireContext()).session().userId());adapter=new CartAdapter(new CartAdapter.Change(){
            @Override public void set(long id,int quantity){try{cart.setQuantity(id,quantity);render();}catch(IllegalArgumentException e){Snackbar.make(b.getRoot(),"Số lượng vượt tồn kho hiển thị",Snackbar.LENGTH_SHORT).show();}}
            @Override public void remove(long id){removeWithUndo(id);}
        });
        b.list.setLayoutManager(new LinearLayoutManager(requireContext()));b.list.setAdapter(adapter);b.checkout.setOnClickListener(v->startActivity(new Intent(requireContext(),CheckoutActivity.class)));
        b.continueShopping.setOnClickListener(v->((MainActivity)requireActivity()).showTab(R.id.nav_shop));render();
    }
    @Override public void onResume(){super.onResume();if(b!=null&&cart!=null){render();syncImages();}}
    private void removeWithUndo(long id){
        CartStore.CartItem removed=null;for(var item:cart.items())if(item.variantId==id){removed=item;break;}if(removed==null)return;cart.remove(id);render();CartStore.CartItem restore=removed;
        Snackbar.make(b.getRoot(),"Đã xóa sản phẩm",Snackbar.LENGTH_LONG).setAction("Hoàn tác",v->{try{cart.add(restore);render();}catch(IllegalArgumentException ignored){}}).show();
    }
    private void syncImages(){Set<Long> ids=new HashSet<>();for(CartStore.CartItem item:cart.items())if(item.productId>0)ids.add(item.productId);ProductRepository products=new ProductRepository(ApiClient.get(requireContext()).api());for(long productId:ids)products.detail(productId,new Callback<Models.Product>(){
        @Override public void onResponse(Call<Models.Product> call,Response<Models.Product> response){if(response.isSuccessful()&&response.body()!=null&&cart.updateImage(productId,response.body().imgUrl)&&b!=null)render();}
        @Override public void onFailure(Call<Models.Product> call,Throwable error){}
    });}
    private void render(){
        var items=cart.items();adapter.submit(items);boolean empty=items.isEmpty();b.emptyContainer.setVisibility(empty?View.VISIBLE:View.GONE);b.list.setVisibility(empty?View.GONE:View.VISIBLE);b.checkout.setEnabled(!empty);b.checkout.setVisibility(empty?View.GONE:View.VISIBLE);b.subtotal.setVisibility(empty?View.GONE:View.VISIBLE);b.note.setVisibility(empty?View.GONE:View.VISIBLE);b.subtotal.setText("Tạm tính: "+Formatters.money(cart.displaySubtotal()));if(requireActivity() instanceof MainActivity){((MainActivity)requireActivity()).updateCartBadge();((MainActivity)requireActivity()).syncEngagement();}
    }
    @Override public void onDestroyView(){super.onDestroyView();b=null;}
}