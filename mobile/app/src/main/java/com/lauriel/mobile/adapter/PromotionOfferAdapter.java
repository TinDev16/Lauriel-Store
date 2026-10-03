package com.lauriel.mobile.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.lauriel.mobile.databinding.ItemPromotionOfferBinding;
import java.util.ArrayList;
import java.util.List;

public final class PromotionOfferAdapter extends RecyclerView.Adapter<PromotionOfferAdapter.Holder> {
    public enum Type { PRODUCT, ORDER, VOUCHER }

    public static final class Offer {
        public final Type type;
        public final String badge;
        public final String title;
        public final String benefit;
        public final String condition;
        public final String action;
        public final String copyText;

        public Offer(Type type, String badge, String title, String benefit,
                String condition, String action, String copyText) {
            this.type = type;
            this.badge = badge;
            this.title = title;
            this.benefit = benefit;
            this.condition = condition;
            this.action = action;
            this.copyText = copyText;
        }
    }

    public interface Listener {
        void onOpen(Offer offer);
        void onAction(Offer offer);
    }

    private final List<Offer> items = new ArrayList<>();
    private final Listener listener;

    public PromotionOfferAdapter(Listener listener) { this.listener = listener; }

    public void submit(List<Offer> next) {
        items.clear();
        if (next != null) items.addAll(next);
        notifyDataSetChanged();
    }

    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new Holder(ItemPromotionOfferBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
        Offer offer = items.get(position);
        holder.binding.badge.setText(offer.badge);
        holder.binding.title.setText(offer.title);
        holder.binding.benefit.setText(offer.benefit);
        holder.binding.condition.setText(offer.condition);
        holder.binding.getRoot().setContentDescription(offer.badge + ". " + offer.title + ". "
                + offer.benefit + ". " + offer.condition);
        holder.binding.action.setText(offer.action);
        holder.binding.action.setVisibility(offer.action == null ? View.GONE : View.VISIBLE);
        holder.binding.action.setOnClickListener(view -> listener.onAction(offer));
        holder.binding.getRoot().setOnClickListener(view -> listener.onOpen(offer));
    }

    @Override public int getItemCount() { return items.size(); }

    static final class Holder extends RecyclerView.ViewHolder {
        final ItemPromotionOfferBinding binding;
        Holder(ItemPromotionOfferBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
