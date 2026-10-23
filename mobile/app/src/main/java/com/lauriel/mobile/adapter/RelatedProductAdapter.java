package com.lauriel.mobile.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.lauriel.mobile.R;
import com.lauriel.mobile.data.api.ApiClient;
import com.lauriel.mobile.data.model.Models;
import com.lauriel.mobile.databinding.ItemRelatedProductBinding;
import com.lauriel.mobile.util.ProductPriceView;
import java.util.ArrayList;
import java.util.List;

public final class RelatedProductAdapter
    extends RecyclerView.Adapter<RelatedProductAdapter.Holder>
{

    public interface Open {
        void open(long id);
    }

    private final Open open;
    private final List<Models.Product> items = new ArrayList<>();

    public RelatedProductAdapter(Open open) {
        this.open = open;
    }

    public void submit(List<Models.Product> next) {
        items.clear();
        if (next != null) items.addAll(next);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup p, int t) {
        return new Holder(
            ItemRelatedProductBinding.inflate(
                LayoutInflater.from(p.getContext()),
                p,
                false
            )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int pos) {
        var p = items.get(pos);
        h.b.name.setText(p.productName);
        ProductPriceView.bind(p, h.b.effectivePrice, h.b.price, h.b.promotion);
        Glide.with(h.b.image)
            .load(ApiClient.imageUrl(p.imgUrl))
            .placeholder(R.drawable.image_placeholder)
            .error(R.drawable.image_placeholder)
            .centerCrop()
            .into(h.b.image);
        h.b.getRoot().setOnClickListener(v -> open.open(p.productId));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {

        final ItemRelatedProductBinding b;

        Holder(ItemRelatedProductBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
