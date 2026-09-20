package com.lauriel.mobile.ui.product;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.lauriel.mobile.R;
import com.lauriel.mobile.data.api.ApiClient;
import com.lauriel.mobile.data.local.CartStore;
import com.lauriel.mobile.data.model.Models;
import com.lauriel.mobile.databinding.ActivityProductDetailBinding;
import com.lauriel.mobile.ui.BaseActivity;
import com.lauriel.mobile.util.Formatters;
import java.util.*;
import retrofit2.*;
public class ProductDetailActivity extends BaseActivity {
    private ActivityProductDetailBinding b;
    private Models.Product product;private Models.Variant selected;private Long sizeId;private int quantity=1;
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);b=ActivityProductDetailBinding.inflate(getLayoutInflater());setContentView(b.getRoot());com.lauriel.mobile.ui.SystemBars.apply(b.getRoot());
        b.back.setOnClickListener(v->finish());b.decrease.setOnClickListener(v->{if(quantity>1){quantity--;updateQuantity();}});
        b.increase.setOnClickListener(v->{if(selected!=null&&quantity<selected.quantity){quantity++;updateQuantity();}});
        b.addCart.setOnClickListener(v->add());b.addCart.setEnabled(false);
        long id=getIntent().getLongExtra("productId",-1);
        if(id<=0){finish();return;}b.error.setOnClickListener(v->load(id));load(id);
    }
    private void load(long id){
        b.loading.setVisibility(View.VISIBLE);
        new com.lauriel.mobile.data.repository.ProductRepository(ApiClient.get(this).api()).detail(id,new Callback<Models.Product>(){
            @Override public void onResponse(Call<Models.Product> c,Response<Models.Product> r){
                if(isFinishing())return;b.loading.setVisibility(View.GONE);
                if(!r.isSuccessful()||r.body()==null){b.error.setText(ApiClient.get(ProductDetailActivity.this).error(r));return;}
                product=r.body();render();
            }
            @Override public void onFailure(Call<Models.Product> c,Throwable e){
                if(!isFinishing()){b.loading.setVisibility(View.GONE);b.error.setText(R.string.network_error);}
            }
        });
    }
    private void render(){
        b.name.setText(product.productName);b.description.setText(product.description);
        b.price.setText(Formatters.money(product.basePrice));
        Glide.with(b.image).load(com.lauriel.mobile.data.api.ApiClient.imageUrl(product.imgUrl)).placeholder(R.drawable.image_placeholder)
                .error(R.drawable.image_placeholder).centerCrop().into(b.image);
        b.sizes.removeAllViews();b.colors.removeAllViews();
        if(product.variants==null||product.variants.isEmpty()){b.stock.setText("Không có biến thể khả dụng.");return;}
        Set<Long> sizes=new LinkedHashSet<>();
        for(var v:product.variants)if("ACTIVE".equals(v.status)&&v.size!=null&&sizes.add(v.size.sizeId)){
            Chip chip=chip(v.size.sizeName);long id=v.size.sizeId;
            chip.setOnClickListener(w->{sizeId=id;showColors();});b.sizes.addView(chip);
            if(sizeId==null){sizeId=id;chip.setChecked(true);}
        }
        showColors();
    }
    private Chip chip(String text){
        Chip chip=new Chip(this);chip.setText(text);chip.setCheckable(true);chip.setMinHeight((int)(48*getResources().getDisplayMetrics().density));
        return chip;
    }
    private void showColors(){
        b.colors.removeAllViews();selected=null;quantity=1;
        if(product.variants==null || sizeId==null){updateSelection();return;}
        for(var v:product.variants)if("ACTIVE".equals(v.status)&&v.size!=null&&v.color!=null&&v.size.sizeId==sizeId){
            Chip color=chip(v.color.colorName);
            color.setOnClickListener(w->{selected=v;quantity=1;updateSelection();});
            b.colors.addView(color);if(selected==null){selected=v;color.setChecked(true);}
        }
        updateSelection();
    }
    private void updateSelection(){
        if(selected==null){b.addCart.setEnabled(false);b.stock.setText("Không có biến thể phù hợp.");return;}
        boolean sale=selected.promotion!=null&&selected.effectivePrice!=null&&selected.price!=null
                &&selected.effectivePrice.compareTo(selected.price)<0;
        if(sale){b.price.setText(Formatters.money(selected.price));b.price.setPaintFlags(b.price.getPaintFlags()|Paint.STRIKE_THRU_TEXT_FLAG);
            b.promotion.setText(Formatters.money(selected.effectivePrice)+" · "+selected.promotion.promotionName);
        }else{b.price.setText(Formatters.money(selected.price));b.price.setPaintFlags(b.price.getPaintFlags()&~Paint.STRIKE_THRU_TEXT_FLAG);
            b.promotion.setText("");}
        b.stock.setText(selected.quantity<=0?"Hết hàng":"Còn "+selected.quantity+" sản phẩm");
        b.addCart.setEnabled(selected.quantity>0);updateQuantity();
    }
    private void updateQuantity(){b.quantity.setText(String.valueOf(quantity));b.increase.setEnabled(selected!=null&&quantity<selected.quantity);}
    private void add(){
        if(selected==null||selected.quantity<quantity)return;
        var session=ApiClient.get(this).session();if(!session.hasCustomer())return;
        var price=selected.effectivePrice==null?selected.price:selected.effectivePrice;
        try{
            new CartStore(this,session.userId()).add(new CartStore.CartItem(selected.variantId,product.productId,
                    product.productName,selected.sku,selected.size.sizeName,selected.color.colorName,
                    product.imgUrl,price,quantity,selected.quantity));
            Toast.makeText(this,"Đã thêm vào giỏ hàng.",Toast.LENGTH_SHORT).show();finish();
        }catch(IllegalArgumentException e){b.error.setText("Số lượng vượt tồn kho hiển thị.");}
    }
}
