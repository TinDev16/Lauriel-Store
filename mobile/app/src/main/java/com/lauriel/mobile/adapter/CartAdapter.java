package com.lauriel.mobile.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.lauriel.mobile.R;
import com.lauriel.mobile.data.api.ApiClient;
import com.lauriel.mobile.data.local.CartStore;
import com.lauriel.mobile.databinding.ItemCartBinding;
import com.lauriel.mobile.util.Formatters;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class CartAdapter extends ListAdapter<CartStore.CartItem, CartAdapter.Holder> {
    public interface Change { void set(long variantId, int quantity); void remove(long variantId); }
    private final Change change;
    public CartAdapter(Change change) { super(DIFF); this.change = change; }
    public void submit(List<CartStore.CartItem> items) { submitList(items == null ? List.of() : new ArrayList<>(items)); }
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) { return new Holder(ItemCartBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false)); }
    @Override public void onBindViewHolder(@NonNull Holder h, int position) {
        var item = getItem(position);
        h.b.name.setText(item.productName); h.b.options.setText(item.size + " / " + item.color + " · " + item.sku);
        h.b.price.setText(Formatters.money(item.displayPrice)); h.b.quantity.setText(String.valueOf(item.quantity));
        h.b.decrease.setEnabled(item.quantity > 1); h.b.increase.setEnabled(item.quantity < item.knownStock);
        Glide.with(h.b.image).load(ApiClient.imageUrl(item.image)).placeholder(R.drawable.image_placeholder)
                .error(R.drawable.image_placeholder).centerCrop().transition(DrawableTransitionOptions.withCrossFade(180)).into(h.b.image);
        h.b.image.setContentDescription(item.productName);
        h.b.decrease.setOnClickListener(v -> change.set(item.variantId, item.quantity - 1));
        h.b.increase.setOnClickListener(v -> change.set(item.variantId, item.quantity + 1));
        h.b.remove.setOnClickListener(v -> change.remove(item.variantId));
    }
    @Override public void onViewRecycled(@NonNull Holder holder) { Glide.with(holder.b.image).clear(holder.b.image); super.onViewRecycled(holder); }
    static final class Holder extends RecyclerView.ViewHolder { final ItemCartBinding b; Holder(ItemCartBinding b) { super(b.getRoot()); this.b = b; } }
    private static final DiffUtil.ItemCallback<CartStore.CartItem> DIFF = new DiffUtil.ItemCallback<>() {
        @Override public boolean areItemsTheSame(@NonNull CartStore.CartItem a, @NonNull CartStore.CartItem b) { return a.variantId == b.variantId; }
        @Override public boolean areContentsTheSame(@NonNull CartStore.CartItem a, @NonNull CartStore.CartItem b) {
            return a.quantity == b.quantity && a.knownStock == b.knownStock && Objects.equals(a.image,b.image)
                    && Objects.equals(a.displayPrice,b.displayPrice) && Objects.equals(a.productName,b.productName);
        }
    };
}