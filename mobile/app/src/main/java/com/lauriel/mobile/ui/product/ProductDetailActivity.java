package com.lauriel.mobile.ui.product;

import android.graphics.Paint;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.View;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import com.lauriel.mobile.R;
import com.lauriel.mobile.data.api.ApiClient;
import com.lauriel.mobile.data.local.CartStore;
import com.lauriel.mobile.data.model.Models;
import com.lauriel.mobile.databinding.ActivityProductDetailBinding;
import com.lauriel.mobile.ui.BaseActivity;
import com.lauriel.mobile.util.Formatters;
import java.util.LinkedHashSet;
import java.util.Set;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductDetailActivity extends BaseActivity {
    private ActivityProductDetailBinding b; private Models.Product product; private Models.Variant selected; private Long sizeId; private int quantity=1;
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);b=ActivityProductDetailBinding.inflate(getLayoutInflater());setContentView(b.getRoot());com.lauriel.mobile.ui.SystemBars.apply(b.getRoot());
        b.back.setOnClickListener(v->finish());b.decrease.setOnClickListener(v->{if(quantity>1){quantity--;updateQuantity();}});b.increase.setOnClickListener(v->{if(selected!=null&&quantity<selected.quantity){quantity++;updateQuantity();}});
        b.addCart.setOnClickListener(v->add());b.addCart.setEnabled(false);long id=getIntent().getLongExtra("productId",-1);if(id<=0){finish();return;}b.error.setOnClickListener(v->load(id));load(id);
    }
    private void load(long id){b.loading.setVisibility(View.VISIBLE);b.error.setText("");new com.lauriel.mobile.data.repository.ProductRepository(ApiClient.get(this).api()).detail(id,new Callback<Models.Product>(){
        @Override public void onResponse(Call<Models.Product> c,Response<Models.Product> r){if(isFinishing())return;b.loading.setVisibility(View.GONE);if(!r.isSuccessful()||r.body()==null){b.error.setText(ApiClient.get(ProductDetailActivity.this).error(r)+" Chạm để thử lại.");return;}product=r.body();render();}
        @Override public void onFailure(Call<Models.Product> c,Throwable e){if(!isFinishing()){b.loading.setVisibility(View.GONE);b.error.setText("Không thể kết nối máy chủ. Chạm để thử lại.");}}
    });}
    private void render(){
        b.name.setText(product.productName);b.description.setText(product.description);b.effectivePrice.setText(Formatters.money(product.basePrice));b.price.setVisibility(View.GONE);b.promotion.setVisibility(View.GONE);
        Glide.with(b.image).load(ApiClient.imageUrl(product.imgUrl)).placeholder(R.drawable.image_placeholder).error(R.drawable.image_placeholder).centerCrop().transition(DrawableTransitionOptions.withCrossFade(180)).into(b.image);b.image.setContentDescription(product.productName);
        b.sizes.removeAllViews();b.colors.removeAllViews();if(product.variants==null||product.variants.isEmpty()){b.stock.setText("Không có biến thể khả dụng.");b.addCart.setText("Hết hàng");return;}
        Set<Long> sizes=new LinkedHashSet<>();Chip firstAvailable=null;
        for(var variant:product.variants)if("ACTIVE".equals(variant.status)&&variant.size!=null&&sizes.add(variant.size.sizeId)){
            boolean available=hasStock(variant.size.sizeId);Chip chip=chip(variant.size.sizeName);chip.setEnabled(available);long id=variant.size.sizeId;chip.setOnClickListener(w->{sizeId=id;showColors();});b.sizes.addView(chip);if(available&&firstAvailable==null){firstAvailable=chip;sizeId=id;}
        }
        if(firstAvailable!=null)firstAvailable.setChecked(true);showColors();
    }
    private boolean hasStock(long id){for(var v:product.variants)if("ACTIVE".equals(v.status)&&v.size!=null&&v.size.sizeId==id&&v.quantity>0)return true;return false;}
    private Chip chip(String text){Chip chip=new Chip(this);chip.setText(text);chip.setCheckable(true);chip.setMinHeight(dp(48));return chip;}
    private void showColors(){
        b.colors.removeAllViews();selected=null;quantity=1;if(product.variants==null||sizeId==null){updateSelection();return;}Chip first=null;
        for(var variant:product.variants)if("ACTIVE".equals(variant.status)&&variant.size!=null&&variant.color!=null&&variant.size.sizeId==sizeId){Chip color=chip(variant.color.colorName);color.setEnabled(variant.quantity>0);color.setOnClickListener(w->{selected=variant;quantity=1;updateSelection();});b.colors.addView(color);if(variant.quantity>0&&first==null){first=color;selected=variant;}}
        if(first!=null)first.setChecked(true);updateSelection();
    }
    private void updateSelection(){
        if(selected==null){b.addCart.setEnabled(false);b.addCart.setText("Hết hàng");b.stock.setText("Không có biến thể phù hợp.");return;}
        boolean sale=selected.promotion!=null&&selected.effectivePrice!=null&&selected.price!=null&&selected.effectivePrice.compareTo(selected.price)<0;
        b.effectivePrice.animate().alpha(0f).setDuration(80).withEndAction(()->{b.effectivePrice.setText(Formatters.money(sale?selected.effectivePrice:selected.price));b.effectivePrice.animate().alpha(1f).setDuration(120).start();}).start();
        if(sale){b.price.setVisibility(View.VISIBLE);b.price.setText(Formatters.money(selected.price));b.price.setPaintFlags(b.price.getPaintFlags()|Paint.STRIKE_THRU_TEXT_FLAG);b.promotion.setText(selected.promotion.promotionName);b.promotion.setVisibility(View.VISIBLE);}else{b.price.setVisibility(View.GONE);b.promotion.setVisibility(View.GONE);}
        b.stock.setText(selected.quantity<=0?"Hết hàng":"Còn "+selected.quantity+" sản phẩm");b.addCart.setEnabled(selected.quantity>0);b.addCart.setText(selected.quantity>0?getString(R.string.add_cart):"Hết hàng");updateQuantity();
    }
    private void updateQuantity(){b.quantity.setText(String.valueOf(quantity));b.decrease.setEnabled(quantity>1);b.increase.setEnabled(selected!=null&&quantity<selected.quantity);}
    private void add(){
        if(selected==null||selected.quantity<quantity)return;var session=ApiClient.get(this).session();if(!session.hasCustomer())return;var price=selected.effectivePrice==null?selected.price:selected.effectivePrice;
        try{new CartStore(this,session.userId()).add(new CartStore.CartItem(selected.variantId,product.productId,product.productName,selected.sku,selected.size.sizeName,selected.color.colorName,product.imgUrl,price,quantity,selected.quantity));b.addCart.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);Snackbar.make(b.getRoot(),"Đã thêm vào giỏ hàng",Snackbar.LENGTH_SHORT).setAction("Đóng",v->{}).show();}
        catch(IllegalArgumentException e){b.error.setText("Số lượng vượt tồn kho hiển thị.");}
    }
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}