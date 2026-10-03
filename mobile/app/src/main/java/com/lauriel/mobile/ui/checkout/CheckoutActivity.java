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
import com.lauriel.mobile.data.local.VoucherStore;
import com.lauriel.mobile.data.model.Models;
import com.lauriel.mobile.data.repository.CheckoutUseCase;
import com.lauriel.mobile.data.repository.OrderRepository;
import com.lauriel.mobile.data.repository.VoucherRepository;
import com.lauriel.mobile.databinding.ActivityCheckoutBinding;
import com.lauriel.mobile.ui.BaseActivity;
import com.lauriel.mobile.ui.MainActivity;
import com.lauriel.mobile.ui.order.OrderDetailActivity;
import com.lauriel.mobile.ui.product.ProductDetailActivity;
import com.lauriel.mobile.util.Formatters;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import retrofit2.*;

public class CheckoutActivity extends BaseActivity {
    public static final String EXTRA_BUY_NOW="buyNow";
    public static final String EXTRA_VARIANT_ID="variantId",EXTRA_PRODUCT_ID="productId",EXTRA_PRODUCT_NAME="productName";
    public static final String EXTRA_SKU="sku",EXTRA_SIZE="size",EXTRA_COLOR="color",EXTRA_IMAGE="image";
    public static final String EXTRA_PRICE="price",EXTRA_QUANTITY="quantity",EXTRA_STOCK="stock";
    private ActivityCheckoutBinding b;
    private CartStore cart;
    private VoucherStore voucherStore;
    private List<CartStore.CartItem> items;
    private boolean buyNow;
    private Models.VoucherPreview preview;
    private Models.VoucherPreview automaticPreview;
    private long previewRevision=-1;
    private String previewCode;
    private boolean submitting,validating,vouchersLoading;

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        b=ActivityCheckoutBinding.inflate(getLayoutInflater());setContentView(b.getRoot());
        com.lauriel.mobile.ui.SystemBars.apply(b.getRoot());
        buyNow=getIntent().getBooleanExtra(EXTRA_BUY_NOW,false);
        voucherStore=new VoucherStore(this,ApiClient.get(this).session().userId());
        if(buyNow){
            try{items=List.of(intentItem());}catch(RuntimeException error){finish();return;}
        }else{
            cart=new CartStore(this,ApiClient.get(this).session().userId());
            items=cart.items();
        }
        CheckoutProductAdapter adapter=new CheckoutProductAdapter(item -> startActivity(
                new Intent(this, ProductDetailActivity.class).putExtra("productId", item.productId)));
        b.products.setLayoutManager(new LinearLayoutManager(this));b.products.setAdapter(adapter);adapter.submit(items);
        b.back.setOnClickListener(v->finish());b.apply.setOnClickListener(v->validateVoucher());b.checkout.setOnClickListener(v->checkout());
        b.chooseSavedVoucher.setOnClickListener(v->chooseSavedVoucher());
        b.removeVoucher.setOnClickListener(v->removeVoucher());
        b.paymentCod.setOnCheckedChangeListener((button, checked) -> {
            if (checked) b.paymentError.setVisibility(View.GONE);
        });
        b.voucher.addTextChangedListener(new android.text.TextWatcher(){
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count){clearPreview();renderPrice();}
            @Override public void afterTextChanged(android.text.Editable s){}
        });
        loadAddress();renderPrice();loadAutomaticPreview();
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
        if(!buyNow&&cart!=null&&previewRevision!=cart.revision()){clearPreview();automaticPreview=null;items=cart.items();
            ((CheckoutProductAdapter)b.products.getAdapter()).submit(items);}
        if(b!=null){renderPrice();loadAutomaticPreview();}
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
    private boolean empty(){return items==null||items.isEmpty();}

    private void renderPrice(){
        Models.VoucherPreview shown=preview!=null?preview:automaticPreview;
        if(shown==null){
            b.subtotal.setText("Tạm tính: Đang tính…");
            b.productPromotionDiscount.setVisibility(View.GONE);
            b.orderPromotionDiscount.setVisibility(View.GONE);
            b.discount.setVisibility(View.GONE);
            b.shipping.setText("Phí giao hàng: Đang tính…");
            b.total.setText("Tổng thanh toán: Đang tính…");
            b.checkout.setEnabled(false);
            return;
        }
        BigDecimal productDiscount=amount(shown.productPromotionDiscount);
        BigDecimal discountedSubtotal=amount(shown.subtotal);
        BigDecimal grossSubtotal=discountedSubtotal.add(productDiscount);
        BigDecimal orderDiscount=amount(shown.orderPromotionDiscount);
        BigDecimal voucherDiscount=amount(shown.discountAmount);
        b.subtotal.setText("Tạm tính: "+Formatters.money(grossSubtotal));
        showDiscount(b.productPromotionDiscount,"Khuyến mãi sản phẩm",productDiscount);
        String campaign=shown.orderPromotionName==null?"Khuyến mãi đơn hàng":shown.orderPromotionName;
        showDiscount(b.orderPromotionDiscount,campaign,orderDiscount);
        showDiscount(b.discount,"Mã giảm giá",voucherDiscount);
        b.shipping.setText("Phí giao hàng: "+Formatters.money(amount(shown.shippingFee)));
        b.total.setText("Tổng thanh toán: "+Formatters.money(amount(shown.finalAmount)));
        b.checkout.setEnabled(!empty()&&!submitting&&!validating);
    }

    private static BigDecimal amount(BigDecimal value){return value==null?BigDecimal.ZERO:value;}
    private void showDiscount(android.widget.TextView view,String label,BigDecimal value){
        view.setVisibility(value.signum()>0?View.VISIBLE:View.GONE);
        view.setText(label+": -"+Formatters.money(value));
    }

    private void loadAutomaticPreview(){
        if(empty())return;
        long requestedRevision=revision();
        automaticPreview=null;
        renderPrice();
        ApiClient.get(this).api().previewOrder(new Models.OrderPreviewRequest(payload(),null)).enqueue(new Callback<Models.VoucherPreview>(){
            @Override public void onResponse(Call<Models.VoucherPreview> c,Response<Models.VoucherPreview> r){
                if(!isFinishing()&&requestedRevision==revision()&&r.isSuccessful()&&r.body()!=null){
                    automaticPreview=r.body();
                    b.error.setText("");
                    renderPrice();
                }else if(!isFinishing()&&requestedRevision==revision()){
                    b.error.setText(ApiClient.get(CheckoutActivity.this).error(r));
                    renderPrice();
                }
            }
            @Override public void onFailure(Call<Models.VoucherPreview> c,Throwable e){
                if(!isFinishing()&&requestedRevision==revision()){
                    b.error.setText("Không thể tính giá từ máy chủ. Vui lòng thử lại.");
                    renderPrice();
                }
            }
        });
    }

    private void clearPreview(){
        preview=null;previewRevision=-1;previewCode=null;
        if(b!=null){
            b.preview.setText("");
            b.previewCard.setVisibility(View.GONE);
            b.voucherEntryCard.setVisibility(View.VISIBLE);
        }
    }

    private void removeVoucher(){
        b.voucher.setText("");
        clearPreview();
        b.chooseSavedVoucher.setText(R.string.choose_saved_voucher);
        b.error.setText("");
        renderPrice();
        loadAutomaticPreview();
    }

    private void chooseSavedVoucher(){
        if(vouchersLoading)return;
        vouchersLoading=true;
        b.chooseSavedVoucher.setEnabled(false);
        b.chooseSavedVoucher.setText(R.string.loading_vouchers);
        ApiClient.get(this).api().activeVouchers(0,100).enqueue(new Callback<Models.Page<Models.Voucher>>(){
            @Override public void onResponse(Call<Models.Page<Models.Voucher>> call,
                    Response<Models.Page<Models.Voucher>> response){
                finishVoucherLoading();
                if(isFinishing())return;
                if(!response.isSuccessful()||response.body()==null){
                    b.error.setText(ApiClient.get(CheckoutActivity.this).error(response));return;
                }
                Set<Long> saved=voucherStore.savedIds();
                List<Models.Voucher> available=new ArrayList<>();
                if(response.body().content!=null){
                    for(Models.Voucher voucher:response.body().content){
                        if(saved.contains(voucher.voucherId))available.add(voucher);
                    }
                }
                if(available.isEmpty()){
                    new MaterialAlertDialogBuilder(CheckoutActivity.this)
                            .setTitle(R.string.choose_voucher_title)
                            .setMessage(R.string.no_saved_vouchers)
                            .setPositiveButton(android.R.string.ok,null).show();
                    return;
                }
                CharSequence[] labels=new CharSequence[available.size()];
                for(int index=0;index<available.size();index++){
                    labels[index]=savedVoucherLabel(available.get(index));
                }
                new MaterialAlertDialogBuilder(CheckoutActivity.this)
                        .setTitle("Mã giảm giá đã lưu")
                        .setIcon(R.drawable.ic_bookmark)
                        .setItems(labels,(dialog,index)->{
                            Models.Voucher selected=available.get(index);
                            BigDecimal shortfall=amount(selected.minOrderAmount).subtract(voucherEligibleAmount());
                            if(shortfall.signum()>0){
                                b.error.setText("Thêm "+Formatters.money(shortfall)+" để sử dụng mã "+selected.code);
                                return;
                            }
                            b.voucher.setText(selected.code);
                            b.voucher.setSelection(b.voucher.length());
                            b.chooseSavedVoucher.setText("Đã chọn mã đã lưu: "+selected.code);
                            validateVoucher();
                        })
                        .setNegativeButton("Đóng",null).show();
            }
            @Override public void onFailure(Call<Models.Page<Models.Voucher>> call,Throwable error){
                finishVoucherLoading();
                if(!isFinishing())b.error.setText(R.string.network_error);
            }
        });
    }

    private String savedVoucherLabel(Models.Voucher voucher){
        String benefit;
        if("PERCENT".equals(voucher.discountType)){
            String percent=voucher.discountValue==null?"0":voucher.discountValue.stripTrailingZeros().toPlainString();
            benefit="Giảm "+percent+"%";
            if(voucher.maxDiscountAmount!=null)benefit+=" · tối đa "+Formatters.money(voucher.maxDiscountAmount);
        }else{
            benefit="Giảm "+Formatters.money(voucher.discountValue);
        }
        BigDecimal minimum=voucher.minOrderAmount==null?BigDecimal.ZERO:voucher.minOrderAmount;
        String condition=minimum.signum()<=0?"Mọi đơn hàng":"Đơn từ "+Formatters.money(minimum);
        BigDecimal shortfall=minimum.subtract(voucherEligibleAmount());
        String eligibility=shortfall.signum()>0
                ? "\nChưa đủ điều kiện · Thêm "+Formatters.money(shortfall)
                : "\nCó thể sử dụng";
        String expiry=voucher.endDate==null?"":" · HSD "+voucher.endDate.substring(0,Math.min(10,voucher.endDate.length()));
        return "✓ ĐÃ LƯU  "+voucher.code+"\n"
                +voucher.voucherName+"\n"
                +benefit+" · "+condition+expiry+eligibility;
    }

    private BigDecimal voucherEligibleAmount(){
        if(automaticPreview==null)return BigDecimal.ZERO;
        return amount(automaticPreview.orderAmount).subtract(amount(automaticPreview.orderPromotionDiscount));
    }

    private void finishVoucherLoading(){
        vouchersLoading=false;
        if(b==null)return;
        b.chooseSavedVoucher.setEnabled(true);
        b.chooseSavedVoucher.setText(R.string.choose_saved_voucher);
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
                    automaticPreview=r.body();
                    b.previewCard.setVisibility(View.VISIBLE);
                    b.voucherEntryCard.setVisibility(View.GONE);
                    b.preview.setText("✓ Đã áp dụng "+preview.code+"\nTiết kiệm "+Formatters.money(preview.discountAmount));
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
        if(!b.paymentCod.isChecked()){
            b.paymentError.setVisibility(View.VISIBLE);
            b.paymentCod.requestFocus();
            return;
        }
        b.paymentError.setVisibility(View.GONE);
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
