package com.lauriel.mobile.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.lauriel.mobile.R;
import com.lauriel.mobile.data.api.ApiClient;
import com.lauriel.mobile.databinding.ItemProductImageBinding;
import java.util.ArrayList;
import java.util.List;

public final class ProductImageAdapter
    extends RecyclerView.Adapter<ProductImageAdapter.Holder>
{

    private final List<String> urls = new ArrayList<>();

    public void submit(List<String> next) {
        urls.clear();
        if (next != null) urls.addAll(next);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup p, int t) {
        return new Holder(
            ItemProductImageBinding.inflate(
                LayoutInflater.from(p.getContext()),
                p,
                false
            )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Glide.with(h.b.image)
            .load(ApiClient.imageUrl(urls.get(position)))
            .placeholder(R.drawable.image_placeholder)
            .error(R.drawable.image_placeholder)
            .centerCrop()
            .into(h.b.image);
    }

    @Override
    public int getItemCount() {
        return urls.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {

        final ItemProductImageBinding b;

        Holder(ItemProductImageBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
