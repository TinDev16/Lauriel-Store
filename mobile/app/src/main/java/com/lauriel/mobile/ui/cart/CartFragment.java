package com.lauriel.mobile.ui.cart;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.Toast;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.lauriel.mobile.adapter.CartAdapter;
import com.lauriel.mobile.data.api.ApiClient;
import com.lauriel.mobile.data.local.CartStore;
import com.lauriel.mobile.data.model.Models;
import com.lauriel.mobile.data.repository.ProductRepository;
import java.util.HashSet;
import java.util.Set;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.lauriel.mobile.databinding.FragmentCartBinding;
import com.lauriel.mobile.ui.checkout.CheckoutActivity;
import com.lauriel.mobile.util.Formatters;
public class CartFragment extends Fragment {
    private FragmentCartBinding b;private CartStore cart;private CartAdapter adapter;
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater i,@Nullable ViewGroup c,@Nullable Bundle s){
        b=FragmentCartBinding.inflate(i,c,false);return b.getRoot();
    }
    @Override public void onViewCreated(@NonNull View view,@Nullable Bundle saved){
        cart=new CartStore(requireContext(),ApiClient.get(requireContext()).session().userId());
        adapter=new CartAdapter(new CartAdapter.Change(){
            @Override public void set(long id,int quantity){
                try{cart.setQuantity(id,quantity);render();}
                catch(IllegalArgumentException e){Toast.makeText(requireContext(),"Số lượng vượt tồn kho hiển thị.",Toast.LENGTH_SHORT).show();}
            }
            @Override public void remove(long id){cart.remove(id);render();}
        });
        b.list.setLayoutManager(new LinearLayoutManager(requireContext()));b.list.setAdapter(adapter);
        b.checkout.setOnClickListener(v->startActivity(new Intent(requireContext(),CheckoutActivity.class)));
        render();
    }
    @Override public void onResume(){super.onResume();if(b!=null&&cart!=null){render();syncImages();}}
    private void syncImages(){
        Set<Long> productIds=new HashSet<>();
        for(CartStore.CartItem item:cart.items())if(item.productId>0)productIds.add(item.productId);
        ProductRepository products=new ProductRepository(ApiClient.get(requireContext()).api());
        for(long productId:productIds){
            products.detail(productId,new Callback<Models.Product>(){
                @Override public void onResponse(Call<Models.Product> call,Response<Models.Product> response){
                    if(response.isSuccessful()&&response.body()!=null
                            &&cart.updateImage(productId,response.body().imgUrl)&&b!=null)render();
                }
                @Override public void onFailure(Call<Models.Product> call,Throwable error){}
            });
        }
    }
    private void render(){
        adapter.submit(cart.items());b.empty.setVisibility(cart.isEmpty()?View.VISIBLE:View.GONE);
        b.checkout.setEnabled(!cart.isEmpty());b.subtotal.setText("Tạm tính: "+Formatters.money(cart.displaySubtotal()));
    }
    @Override public void onDestroyView(){super.onDestroyView();b=null;}
}
