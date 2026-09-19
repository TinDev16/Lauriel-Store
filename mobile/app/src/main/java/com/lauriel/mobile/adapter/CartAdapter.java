package com.lauriel.mobile.adapter;
import android.view.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.lauriel.mobile.R;
import com.lauriel.mobile.data.local.CartStore;
import com.lauriel.mobile.databinding.ItemCartBinding;
import com.lauriel.mobile.util.Formatters;
import java.util.*;
public final class CartAdapter extends RecyclerView.Adapter<CartAdapter.Holder> {
    public interface Change {void set(long variantId,int quantity);void remove(long variantId);}
    private final Change change;private final List<CartStore.CartItem> items=new ArrayList<>();
    public CartAdapter(Change change){this.change=change;}
    public void submit(List<CartStore.CartItem> updated){items.clear();items.addAll(updated);notifyDataSetChanged();}
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent,int type){
        return new Holder(ItemCartBinding.inflate(LayoutInflater.from(parent.getContext()),parent,false));
    }
    @Override public void onBindViewHolder(@NonNull Holder h,int position){
        var item=items.get(position);h.b.name.setText(item.productName);
        h.b.options.setText(item.size+" / "+item.color+" · "+item.sku);
        h.b.price.setText(Formatters.money(item.displayPrice));
        h.b.quantity.setText(String.valueOf(item.quantity));
        Glide.with(h.b.image).load(item.image).placeholder(R.drawable.image_placeholder)
                .error(R.drawable.image_placeholder).centerCrop().into(h.b.image);
        h.b.decrease.setOnClickListener(v->change.set(item.variantId,item.quantity-1));
        h.b.increase.setOnClickListener(v->change.set(item.variantId,item.quantity+1));
        h.b.remove.setOnClickListener(v->change.remove(item.variantId));
    }
    @Override public int getItemCount(){return items.size();}
    static final class Holder extends RecyclerView.ViewHolder {
        final ItemCartBinding b;Holder(ItemCartBinding b){super(b.getRoot());this.b=b;}
    }
}
