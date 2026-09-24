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
import com.lauriel.mobile.data.model.Models;
import com.lauriel.mobile.databinding.ItemProductBinding;
import com.lauriel.mobile.util.Formatters;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ProductAdapter extends ListAdapter<Models.Product, ProductAdapter.Holder> {
    public interface Open { void open(Models.Product product); }
    private final Open onOpen;
    public ProductAdapter(Open onOpen) { super(DIFF); this.onOpen = onOpen; }
    public void submit(List<Models.Product> products) { submitList(products == null ? List.of() : new ArrayList<>(products)); }
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new Holder(ItemProductBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }
    @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
        Models.Product p = getItem(position);
        holder.b.name.setText(p.productName);
        holder.b.price.setText(Formatters.money(p.price));
        Glide.with(holder.b.image).load(ApiClient.imageUrl(p.imgUrl)).placeholder(R.drawable.image_placeholder)
                .error(R.drawable.image_placeholder).centerCrop().transition(DrawableTransitionOptions.withCrossFade(180)).into(holder.b.image);
        holder.b.image.setContentDescription(p.productName);
        holder.b.getRoot().setOnClickListener(v -> v.animate().scaleX(.98f).scaleY(.98f).setDuration(80)
                .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(80)
                        .withEndAction(() -> onOpen.open(p)).start()).start());
    }
    @Override public void onViewRecycled(@NonNull Holder holder) { Glide.with(holder.b.image).clear(holder.b.image); super.onViewRecycled(holder); }
    static final class Holder extends RecyclerView.ViewHolder { final ItemProductBinding b; Holder(ItemProductBinding b) { super(b.getRoot()); this.b = b; } }
    private static final DiffUtil.ItemCallback<Models.Product> DIFF = new DiffUtil.ItemCallback<>() {
        @Override public boolean areItemsTheSame(@NonNull Models.Product a, @NonNull Models.Product b) { return a.productId == b.productId; }
        @Override public boolean areContentsTheSame(@NonNull Models.Product a, @NonNull Models.Product b) {
            return Objects.equals(a.productName, b.productName) && Objects.equals(a.imgUrl, b.imgUrl)
                    && Objects.equals(a.price, b.price) && Objects.equals(a.status, b.status);
        }
    };
}