package com.lauriel.mobile.ui.product;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.View;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewpager2.widget.ViewPager2;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import com.lauriel.mobile.R;
import com.lauriel.mobile.adapter.ProductImageAdapter;
import com.lauriel.mobile.adapter.RelatedProductAdapter;
import com.lauriel.mobile.data.api.ApiClient;
import com.lauriel.mobile.data.local.CartStore;
import com.lauriel.mobile.data.model.Models;
import com.lauriel.mobile.data.repository.ProductRepository;
import com.lauriel.mobile.databinding.ActivityProductDetailBinding;
import com.lauriel.mobile.databinding.BottomSheetVariantSelectionBinding;
import com.lauriel.mobile.ui.BaseActivity;
import com.lauriel.mobile.ui.checkout.CheckoutActivity;
import com.lauriel.mobile.util.Formatters;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductDetailActivity extends BaseActivity {
    private ActivityProductDetailBinding b;
    private Models.Product product;
    private ProductRepository repository;
    private ProductImageAdapter imageAdapter;
    private RelatedProductAdapter relatedAdapter;
    private boolean firstResume=true;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        b=ActivityProductDetailBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        com.lauriel.mobile.ui.SystemBars.apply(b.getRoot());
        repository=new ProductRepository(ApiClient.get(this).api());
        imageAdapter=new ProductImageAdapter();
        b.gallery.setAdapter(imageAdapter);
        b.gallery.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback(){
            @Override public void onPageSelected(int position){updateIndicator(position);}
        });
        relatedAdapter=new RelatedProductAdapter(id->startActivity(new Intent(this,ProductDetailActivity.class).putExtra("productId",id)));
        b.related.setLayoutManager(new LinearLayoutManager(this,LinearLayoutManager.HORIZONTAL,false));
        b.related.setAdapter(relatedAdapter);
        b.back.setOnClickListener(v->finish());
        b.error.setOnClickListener(v->load(getIntent().getLongExtra("productId",-1)));
        b.addCart.setOnClickListener(v->showVariantSheet(false));
        b.buyNow.setOnClickListener(v->showVariantSheet(true));
        b.addCart.setEnabled(false);b.buyNow.setEnabled(false);
        long id=getIntent().getLongExtra("productId",-1);
        if(id<=0){finish();return;}
        load(id);
    }

    @Override protected void onResume(){
        super.onResume();
        if(firstResume){firstResume=false;return;}
        long id=getIntent().getLongExtra("productId",-1);
        if(id>0)load(id);
    }

    private void load(long id){
        b.loading.setVisibility(View.VISIBLE);b.error.setText("");
        repository.detail(id,new Callback<Models.Product>(){
            @Override public void onResponse(Call<Models.Product> c,Response<Models.Product> r){
                if(isFinishing())return;b.loading.setVisibility(View.GONE);
                if(!r.isSuccessful()||r.body()==null){b.error.setText(ApiClient.get(ProductDetailActivity.this).error(r)+" Chạm để thử lại.");return;}
                product=r.body();render();loadRelated(id);
            }
            @Override public void onFailure(Call<Models.Product> c,Throwable e){
                if(!isFinishing()){b.loading.setVisibility(View.GONE);b.error.setText("Không thể kết nối máy chủ. Chạm để thử lại.");}
            }
        });
    }

    private void render(){
        b.name.setText(product.productName);
        b.description.setText(product.description==null||product.description.isBlank()?"Chưa có mô tả sản phẩm.":product.description);
        List<String> urls=new ArrayList<>();
        if(product.images!=null)for(var image:product.images)if(image.url!=null&&!image.url.isBlank())urls.add(image.url);
        if(urls.isEmpty()&&product.imgUrl!=null)urls.add(product.imgUrl);
        imageAdapter.submit(urls);updateIndicator(0);
        Models.Variant best=product.variants==null?null:product.variants.stream()
                .filter(v->"ACTIVE".equals(v.status)&&v.quantity>0)
                .min(Comparator.comparing(v->v.effectivePrice==null?v.price:v.effectivePrice)).orElse(null);
        if(best==null){
            b.effectivePrice.setText(Formatters.money(product.basePrice));
            b.addCart.setEnabled(false);b.buyNow.setEnabled(false);
            b.addCart.setText("Hết hàng");b.buyNow.setText("Hết hàng");
            return;
        }
        showPrice(best);
        b.addCart.setEnabled(true);b.buyNow.setEnabled(true);
    }

    private void showPrice(Models.Variant variant){
        BigDecimal effective=variant.effectivePrice==null?variant.price:variant.effectivePrice;
        boolean sale=variant.promotion!=null&&effective!=null&&variant.price!=null&&effective.compareTo(variant.price)<0;
        b.effectivePrice.setText(Formatters.money(effective));
        if(sale){
            b.price.setVisibility(View.VISIBLE);b.price.setText(Formatters.money(variant.price));
            b.price.setPaintFlags(b.price.getPaintFlags()|Paint.STRIKE_THRU_TEXT_FLAG);
            b.promotion.setText("-"+variant.promotion.discountPercent.stripTrailingZeros().toPlainString()+"%");
            b.promotion.setVisibility(View.VISIBLE);
        }else{b.price.setVisibility(View.GONE);b.promotion.setVisibility(View.GONE);}
    }

    private void updateIndicator(int position){
        int count=imageAdapter==null?0:imageAdapter.getItemCount();
        b.indicator.setVisibility(count>1?View.VISIBLE:View.GONE);
        b.indicator.setText(count==0?"":(position+1)+" / "+count);
    }

    private void loadRelated(long id){
        repository.related(id,new Callback<List<Models.Product>>(){
            @Override public void onResponse(Call<List<Models.Product>> c,Response<List<Models.Product>> r){
                if(isFinishing())return;
                List<Models.Product> data=r.isSuccessful()&&r.body()!=null?r.body():List.of();
                relatedAdapter.submit(data);b.relatedTitle.setVisibility(data.isEmpty()?View.GONE:View.VISIBLE);
                b.related.setVisibility(data.isEmpty()?View.GONE:View.VISIBLE);
            }
            @Override public void onFailure(Call<List<Models.Product>> c,Throwable e){
                if(!isFinishing()){b.relatedTitle.setVisibility(View.GONE);b.related.setVisibility(View.GONE);}
            }
        });
    }

    private void showVariantSheet(boolean buyNow){
        if(product==null||product.variants==null)return;
        BottomSheetDialog dialog=new BottomSheetDialog(this);
        BottomSheetVariantSelectionBinding sheet=BottomSheetVariantSelectionBinding.inflate(getLayoutInflater());
        dialog.setContentView(sheet.getRoot());
        sheet.name.setText(product.productName);
        Glide.with(sheet.image).load(ApiClient.imageUrl(product.imgUrl)).placeholder(R.drawable.image_placeholder)
                .error(R.drawable.image_placeholder).centerCrop().into(sheet.image);

        VariantAvailability availability=new VariantAvailability(product.variants);
        VariantSelection selection=new VariantSelection(availability);
        final Models.Variant[] selected={null};
        final int[] quantity={1};
        List<Chip> sizeChips=new ArrayList<>();
        List<Chip> colorChips=new ArrayList<>();

        Runnable update=()->{
            Models.Variant variant=selected[0];
            BigDecimal price=variant==null?product.basePrice:
                    (variant.effectivePrice==null?variant.price:variant.effectivePrice);
            sheet.price.setText(Formatters.money(price));
            boolean sale=variant!=null&&variant.promotion!=null&&variant.price!=null&&price!=null
                    && price.compareTo(variant.price)<0;
            if(sale){
                sheet.originalPrice.setText(Formatters.money(variant.price));
                sheet.originalPrice.setPaintFlags(sheet.originalPrice.getPaintFlags()|Paint.STRIKE_THRU_TEXT_FLAG);
                sheet.originalPrice.setVisibility(View.VISIBLE);
                sheet.promotion.setText("-"+variant.promotion.discountPercent.stripTrailingZeros().toPlainString()+"%");
                sheet.promotion.setVisibility(View.VISIBLE);
            }else{
                sheet.originalPrice.setVisibility(View.GONE);
                sheet.promotion.setVisibility(View.GONE);
            }
            Glide.with(sheet.image).load(ApiClient.imageUrl(imageForColor(selection.colorId())))
                    .placeholder(R.drawable.image_placeholder).error(R.drawable.image_placeholder)
                    .centerCrop().into(sheet.image);
            sheet.stock.setText(variant==null?"Ch\u1ECDn k\u00EDch th\u01B0\u1EDBc v\u00E0 m\u00E0u":
                    "C\u00F2n "+variant.quantity+" s\u1EA3n ph\u1EA9m");
            sheet.quantity.setText(String.valueOf(quantity[0]));
            sheet.decrease.setEnabled(quantity[0]>1);
            sheet.increase.setEnabled(variant!=null&&quantity[0]<variant.quantity);
            sheet.confirm.setEnabled(variant!=null);
        };

        final Runnable[] refresh={null};
        refresh[0]=()->{
            for(Chip chip:sizeChips){
                long id=(Long)chip.getTag();
                chip.setEnabled(selection.isSizeEnabled(id));
            }
            for(Chip chip:colorChips){
                long id=(Long)chip.getTag();
                chip.setEnabled(selection.isColorEnabled(id));
            }
            selected[0]=selection.selectedVariant();
            quantity[0]=1;
            update.run();
        };

        for(Models.Size size:availability.sizes()){
            Chip chip=chip(size.sizeName);
            chip.setTag(size.sizeId);
            chip.setOnClickListener(v->{
                Long oldColorId=selection.colorId();
                selection.selectSize(chip.isChecked()?(Long)chip.getTag():null);
                if(oldColorId!=null&&selection.colorId()==null)sheet.colors.clearCheck();
                refresh[0].run();
            });
            sizeChips.add(chip);
            sheet.sizes.addView(chip);
        }
        for(Models.Color color:availability.colors()){
            Chip chip=chip(color.colorName);
            chip.setTag(color.colorId);
            chip.setOnClickListener(v->{
                Long oldSizeId=selection.sizeId();
                selection.selectColor(chip.isChecked()?(Long)chip.getTag():null);
                if(oldSizeId!=null&&selection.sizeId()==null)sheet.sizes.clearCheck();
                refresh[0].run();
            });
            colorChips.add(chip);
            sheet.colors.addView(chip);
        }
        refresh[0].run();

        sheet.decrease.setOnClickListener(v->{if(quantity[0]>1){quantity[0]--;update.run();}});
        sheet.increase.setOnClickListener(v->{if(selected[0]!=null&&quantity[0]<selected[0].quantity){quantity[0]++;update.run();}});
        sheet.confirm.setText(buyNow?"Mua ngay":"Th\u00EAm v\u00E0o gi\u1ECF h\u00E0ng");
        sheet.confirm.setOnClickListener(v->{
            if(selected[0]==null||selected[0].quantity<quantity[0])return;
            if(buyNow)buyNow(selected[0],quantity[0]);else addToCart(selected[0],quantity[0]);
            dialog.dismiss();
        });
        dialog.show();
    }
    private Chip chip(String text){Chip chip=new Chip(this);chip.setText(text);chip.setCheckable(true);chip.setMinHeight(dp(48));return chip;}

    private void addToCart(Models.Variant selected,int quantity){
        var session=ApiClient.get(this).session();if(!session.hasCustomer())return;
        BigDecimal price=selected.effectivePrice==null?selected.price:selected.effectivePrice;
        try{
            new CartStore(this,session.userId()).add(new CartStore.CartItem(selected.variantId,product.productId,
                    product.productName,selected.sku,selected.size.sizeName,selected.color.colorName,
                    product.imgUrl,price,quantity,selected.quantity));
            b.addCart.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            Snackbar.make(b.getRoot(),"Đã thêm vào giỏ hàng",Snackbar.LENGTH_SHORT).show();
        }catch(IllegalArgumentException e){b.error.setText("Số lượng vượt tồn kho hiện tại.");}
    }

    private void buyNow(Models.Variant selected,int quantity){
        BigDecimal price=selected.effectivePrice==null?selected.price:selected.effectivePrice;
        startActivity(new Intent(this,CheckoutActivity.class)
                .putExtra(CheckoutActivity.EXTRA_BUY_NOW,true)
                .putExtra(CheckoutActivity.EXTRA_VARIANT_ID,selected.variantId)
                .putExtra(CheckoutActivity.EXTRA_PRODUCT_ID,product.productId)
                .putExtra(CheckoutActivity.EXTRA_PRODUCT_NAME,product.productName)
                .putExtra(CheckoutActivity.EXTRA_SKU,selected.sku)
                .putExtra(CheckoutActivity.EXTRA_SIZE,selected.size.sizeName)
                .putExtra(CheckoutActivity.EXTRA_COLOR,selected.color.colorName)
                .putExtra(CheckoutActivity.EXTRA_IMAGE,product.imgUrl)
                .putExtra(CheckoutActivity.EXTRA_PRICE,price.toPlainString())
                .putExtra(CheckoutActivity.EXTRA_QUANTITY,quantity)
                .putExtra(CheckoutActivity.EXTRA_STOCK,selected.quantity));
    }
    private String imageForColor(Long colorId){
        if(product!=null&&product.images!=null){
            if(colorId!=null)for(var image:product.images)
                if(image.colorId!=null&&image.colorId.equals(colorId)&&image.url!=null&&!image.url.isBlank())return image.url;
            for(var image:product.images)
                if(image.primary&&image.url!=null&&!image.url.isBlank())return image.url;
            for(var image:product.images)
                if(image.colorId==null&&image.url!=null&&!image.url.isBlank())return image.url;
        }
        return product==null?null:product.imgUrl;
    }
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
