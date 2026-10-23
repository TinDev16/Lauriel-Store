package com.lauriel.mobile.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.lauriel.mobile.R;
import com.lauriel.mobile.data.api.ApiClient;
import com.lauriel.mobile.data.local.CartStore;
import com.lauriel.mobile.databinding.ItemCheckoutProductBinding;
import com.lauriel.mobile.util.Formatters;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class CheckoutProductAdapter
    extends RecyclerView.Adapter<CheckoutProductAdapter.Holder>
{

    public interface Listener {
        void onProductClick(CartStore.CartItem item);
    }

    private final List<CartStore.CartItem> items = new ArrayList<>();
    private final Listener listener;

    public CheckoutProductAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<CartStore.CartItem> next) {
        items.clear();
        if (next != null) items.addAll(next);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new Holder(
            ItemCheckoutProductBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
            )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        var item = items.get(position);
        h.b.getRoot().setContentDescription("Xem chi tiết " + item.productName);
        h.b.name.setText(item.productName);
        h.b.variant.setText(
            "Size: " +
                item.size +
                "  ·  Màu: " +
                item.color +
                "  ·  Số lượng: " +
                item.quantity
        );
        h.b.unitPrice.setText(
            Formatters.money(item.displayPrice) + " × " + item.quantity
        );
        BigDecimal total =
            item.displayPrice == null
                ? BigDecimal.ZERO
                : item.displayPrice.multiply(BigDecimal.valueOf(item.quantity));
        h.b.lineTotal.setText(Formatters.money(total));
        Glide.with(h.b.image)
            .load(ApiClient.imageUrl(item.image))
            .placeholder(R.drawable.image_placeholder)
            .error(R.drawable.image_placeholder)
            .centerCrop()
            .into(h.b.image);
        h.b.getRoot().setOnClickListener(view -> listener.onProductClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {

        final ItemCheckoutProductBinding b;

        Holder(ItemCheckoutProductBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
