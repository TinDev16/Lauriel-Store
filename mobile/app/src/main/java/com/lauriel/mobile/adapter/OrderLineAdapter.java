package com.lauriel.mobile.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.lauriel.mobile.R;
import com.lauriel.mobile.data.api.ApiClient;
import com.lauriel.mobile.data.model.Models;
import com.lauriel.mobile.databinding.ItemOrderLineBinding;
import com.lauriel.mobile.util.Formatters;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class OrderLineAdapter extends RecyclerView.Adapter<OrderLineAdapter.Holder> {
    private final List<Models.OrderLine> items = new ArrayList<>();

    public void submit(List<Models.OrderLine> next) {
        items.clear();
        if (next != null) items.addAll(next);
        notifyDataSetChanged();
    }

    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemOrderLineBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
        Models.OrderLine item = items.get(position);
        holder.b.name.setText(item.productName == null ? holder.b.getRoot().getContext().getString(R.string.unknown_product) : item.productName);
        String size = item.size == null ? "—" : item.size.sizeName;
        String color = item.color == null ? "—" : item.color.colorName;
        holder.b.variant.setText(holder.b.getRoot().getContext().getString(R.string.order_variant, size, color, item.quantity));
        holder.b.unitPrice.setText(holder.b.getRoot().getContext().getString(R.string.price_times_quantity, Formatters.money(item.unitPrice), item.quantity));
        BigDecimal discount = item.discountAmount == null ? BigDecimal.ZERO : item.discountAmount;
        if (discount.signum() > 0) {
            holder.b.discount.setText(holder.b.getRoot().getContext().getString(R.string.discount_saved, Formatters.money(discount.multiply(BigDecimal.valueOf(item.quantity)))));
            holder.b.discount.setVisibility(android.view.View.VISIBLE);
        } else {
            holder.b.discount.setVisibility(android.view.View.GONE);
        }
        holder.b.lineTotal.setText(Formatters.money(item.subtotal));
        Glide.with(holder.b.image).load(ApiClient.imageUrl(item.imageUrl))
                .placeholder(R.drawable.image_placeholder).error(R.drawable.image_placeholder)
                .centerCrop().into(holder.b.image);
    }

    @Override public int getItemCount() { return items.size(); }

    static final class Holder extends RecyclerView.ViewHolder {
        final ItemOrderLineBinding b;
        Holder(ItemOrderLineBinding binding) { super(binding.getRoot()); b = binding; }
    }
}