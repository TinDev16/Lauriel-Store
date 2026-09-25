package com.lauriel.mobile.ui.checkout;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.lauriel.mobile.R;
import com.lauriel.mobile.adapter.CheckoutProductAdapter;
import com.lauriel.mobile.data.api.ApiClient;
import com.lauriel.mobile.data.local.CartStore;
import com.lauriel.mobile.data.model.Models;
import com.lauriel.mobile.data.repository.CheckoutUseCase;
import com.lauriel.mobile.data.repository.OrderRepository;
import com.lauriel.mobile.data.repository.VoucherRepository;
import com.lauriel.mobile.databinding.ActivityCheckoutBinding;
import com.lauriel.mobile.ui.BaseActivity;
import com.lauriel.mobile.ui.MainActivity;
import com.lauriel.mobile.ui.order.OrderDetailActivity;
import com.lauriel.mobile.util.Formatters;
import java.math.BigDecimal;
import java.util.List;
import retrofit2.*;

public class CheckoutActivity extends BaseActivity {
    public static final String EXTRA_BUY_NOW="buyNow";
    public static final String EXTRA_VARIANT_ID="variantId",EXTRA_PRODUCT_ID="productId",EXTRA_PRODUCT_NAME="productName";
    public static final String EXTRA_SKU="sku",EXTRA_SIZE="size",EXTRA_COLOR="color",EXTRA_IMAGE="image";
    public static final String EXTRA_PRICE="price",EXTRA_QUANTITY="quantity",EXTRA_STOCK="stock";
    private static final BigDecimal SHIPPING_PREVIEW=new BigDecimal("20000");

    private ActivityCheckoutBinding b;
    private CartStore cart;
    private List<CartStore.CartItem> items;
    private boolean buyNow;
    private Models.VoucherPreview preview;
    private long previewRevision=-1;
    private String previewCode;
    private boolean submitting,validating;

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        b=ActivityCheckoutBinding.inflate(getLayoutInflater());setContentView(b.getRoot());
        com.lauriel.mobile.ui.SystemBars.apply(b.getRoot());
        buyNow=getIntent().getBooleanExtra(EXTRA_BUY_NOW,false);
        if(buyNow){
            try{items=List.of(intentItem());}catch(RuntimeException error){finish();return;}
        }else{
            cart=new CartStore(this,ApiClient.get(this).session().userId());
            items=cart.items();
        }
        CheckoutProductAdapter adapter=new CheckoutProductAdapter();
        b.products.setLayoutManager(new LinearLayoutManager(this));b.products.setAdapter(adapter);adapter.submit(items);
        b.back.setOnClickListener(v->finish());b.apply.setOnClickListener(v->validateVoucher());b.checkout.setOnClickListener(v->checkout());
        b.voucher.addTextChangedListener(new android.text.TextWatcher(){
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count){clearPreview();renderPrice();}
            @Override public void afterTextChanged(android.text.Editable s){}
        });
        loadAddress();renderPrice();
    }

    private CartStore.CartItem intentItem(){
        long variant=getIntent().getLongExtra(EXTRA_VARIANT_ID,-1);
        long product=getIntent().getLongExtra(EXTRA_PRODUCT_ID,-1);
        int quantity=getIntent().getIntExtra(EXTRA_QUANTITY,0),stock=getIntent().getIntExtra(EXTRA_STOCK,0);
        BigDecimal price=new BigDecimal(getIntent().getStringExtra(EXTRA_PRICE));
        if(variant<=0||product<=0||quantity<=0||stock<=0)throw new IllegalArgumentException();
        return new CartStore.CartItem(variant,product,getIntent().getStringExtra(EXTRA_PRODUCT_NAME),
                getIntent().getStringExtra(EXTRA_SKU),getIntent().getStringExtra(EXTRA_SIZE),
                getIntent().getStringExtra(EXTRA_COLOR),getIntent().getStringExtra(EXTRA_IMAGE),price,quantity,stock);
    }

    @Override protected void onResume(){
        super.onResume();
        if(!buyNow&&cart!=null&&previewRevision!=cart.revision()){clearPreview();items=cart.items();
            ((CheckoutProductAdapter)b.products.getAdapter()).submit(items);}
        if(b!=null)renderPrice();
    }

    private void loadAddress(){
        ApiClient.get(this).api().profile().enqueue(new Callback<Models.Profile>(){
            @Override public void onResponse(Call<Models.Profile> c,Response<Models.Profile> r){
                if(!isFinishing()&&r.isSuccessful()&&r.body()!=null&&r.body().address!=null&&b.address.getText()!=null
                        &&b.address.getText().length()==0)b.address.setText(r.body().address);
            }
            @Override public void onFailure(Call<Models.Profile> c,Throwable e){}
        });
    }

    private List<Models.Item> payload(){
        if(!buyNow)return cart.payload();
        java.util.ArrayList<Models.Item> result=new java.util.ArrayList<>();
        for(CartStore.CartItem item:items)result.add(new Models.Item(item.variantId,item.quantity));
        return result;
    }
    private long revision(){return buyNow?0:cart.revision();}
    private BigDecimal subtotal(){
        BigDecimal total=BigDecimal.ZERO;
        for(var item:items)if(item.displayPrice!=null)total=total.add(item.displayPrice.multiply(BigDecimal.valueOf(item.quantity)));
        return total;
    }
    private boolean empty(){return items==null||items.isEmpty();}

    private void renderPrice(){
        BigDecimal base=preview==null?subtotal():preview.orderAmount;
        BigDecimal discount=preview==null?BigDecimal.ZERO:preview.discountAmount;
        BigDecimal shipping=preview==null?SHIPPING_PREVIEW:preview.shippingFee;
        BigDecimal finalValue=preview==null?base.add(shipping):preview.finalAmount;
        b.subtotal.setText("Tạm tính: "+Formatters.money(base));
        b.discount.setText("Giảm giá: -"+Formatters.money(discount));
        b.shipping.setText("Phí giao hàng: "+Formatters.money(shipping));
        b.total.setText("Tổng thanh toán: "+Formatters.money(finalValue));
        b.checkout.setEnabled(!empty()&&!submitting);
    }

    private void clearPreview(){
        preview=null;previewRevision=-1;previewCode=null;
        if(b!=null){b.preview.setText("");b.previewCard.setVisibility(View.GONE);}
    }

    private void validateVoucher(){
        String code=b.voucher.getText().toString().trim();
        if(code.isEmpty()){clearPreview();renderPrice();b.error.setText("Nhập mã giảm giá.");return;}
        if(empty()||validating)return;
        long revision=revision();validating=true;b.apply.setEnabled(false);b.loading.setVisibility(View.VISIBLE);b.error.setText("");
        new VoucherRepository(ApiClient.get(this).api()).preview(code,payload(),new Callback<Models.VoucherPreview>(){
            @Override public void onResponse(Call<Models.VoucherPreview> c,Response<Models.VoucherPreview> r){
                if(isFinishing())return;validating=false;b.apply.setEnabled(true);b.loading.setVisibility(View.GONE);
                if(revision!=revision()||!code.equalsIgnoreCase(b.voucher.getText().toString().trim())){clearPreview();renderPrice();return;}
                if(r.isSuccessful()&&r.body()!=null&&r.body().valid){
                    preview=r.body();previewRevision=revision;previewCode=code;
                    b.previewCard.setVisibility(View.VISIBLE);
                    b.preview.setText("✓ Đã áp dụng "+preview.code+"\n-"+Formatters.money(preview.discountAmount));
                    renderPrice();
                }else{clearPreview();renderPrice();b.error.setText(ApiClient.get(CheckoutActivity.this).error(r));}
            }
            @Override public void onFailure(Call<Models.VoucherPreview> c,Throwable e){
                if(isFinishing())return;validating=false;b.apply.setEnabled(true);b.loading.setVisibility(View.GONE);
                clearPreview();renderPrice();b.error.setText(R.string.network_error);
            }
        });
    }

    private void checkout(){
        if(submitting||validating||empty())return;
        String address=b.address.getText().toString().trim(),code=b.voucher.getText().toString().trim();
        b.addressLayout.setError(null);
        if(address.isEmpty()){b.addressLayout.setError("Vui lòng nhập địa chỉ giao hàng");b.address.requestFocus();return;}
        if(!code.isEmpty()&&(preview==null||previewRevision!=revision()||!code.equalsIgnoreCase(previewCode))){
            b.error.setText("Giỏ hàng hoặc mã đã thay đổi. Vui lòng áp dụng voucher lại.");return;
        }
        submitting=true;b.checkout.setEnabled(false);b.checkout.setText("Đang đặt hàng…");b.loading.setVisibility(View.VISIBLE);b.error.setText("");
        OrderRepository orders=new OrderRepository(ApiClient.get(this).api());
        CheckoutUseCase useCase=buyNow?new CheckoutUseCase(orders,payload()):new CheckoutUseCase(orders,cart);
        useCase.place(address,code.isEmpty()?null:code,new Callback<Models.Order>(){
            @Override public void onResponse(Call<Models.Order> c,Response<Models.Order> r){
                if(isFinishing())return;submitting=false;b.loading.setVisibility(View.GONE);
                if(r.isSuccessful()&&r.body()!=null){
                    clearPreview();long id=r.body().orderId;
                    new MaterialAlertDialogBuilder(CheckoutActivity.this).setIcon(R.drawable.ic_check)
                            .setTitle("Đặt hàng thành công")
                            .setMessage("Đơn #"+id+" đã được tạo.\nTổng thanh toán: "+Formatters.money(r.body().finalAmount))
                            .setPositiveButton("Xem đơn hàng",(d,w)->{startActivity(new Intent(CheckoutActivity.this,OrderDetailActivity.class).putExtra("orderId",id));finish();})
                            .setNegativeButton("Tiếp tục mua",(d,w)->{startActivity(new Intent(CheckoutActivity.this,MainActivity.class)
                                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));finish();})
                            .setCancelable(false).show();
                }else{b.checkout.setEnabled(true);b.checkout.setText(R.string.checkout);clearPreview();renderPrice();b.error.setText(ApiClient.get(CheckoutActivity.this).error(r));}
            }
            @Override public void onFailure(Call<Models.Order> c,Throwable e){
                if(isFinishing())return;submitting=false;b.loading.setVisibility(View.GONE);b.checkout.setText(R.string.checkout);
                clearPreview();renderPrice();b.error.setText(R.string.network_error);
            }
        });
    }
}
