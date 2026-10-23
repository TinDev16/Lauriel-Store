package com.lauriel.mobile.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.lauriel.mobile.R;
import com.lauriel.mobile.data.model.Models;
import com.lauriel.mobile.databinding.ItemOrderBinding;
import com.lauriel.mobile.ui.common.StatusChip;
import com.lauriel.mobile.util.Formatters;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class OrderAdapter
    extends ListAdapter<Models.OrderSummary, OrderAdapter.Holder>
{

    public interface Open {
        void open(long id);
    }

    private final Open open;

    public OrderAdapter(Open open) {
        super(DIFF);
        this.open = open;
    }

    public void submit(List<Models.OrderSummary> items) {
        submitList(items == null ? List.of() : new ArrayList<>(items));
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup p, int type) {
        return new Holder(
            ItemOrderBinding.inflate(
                LayoutInflater.from(p.getContext()),
                p,
                false
            )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int pos) {
        var o = getItem(pos);
        h.b.id.setText(
            h.b
                .getRoot()
                .getContext()
                .getString(R.string.order_number, o.orderId)
        );
        StatusChip.apply(h.b.status, o.status);
        h.b.date.setText(
            h.b
                .getRoot()
                .getContext()
                .getString(R.string.ordered_at, Formatters.date(o.createdAt))
        );
        h.b.amount.setText(Formatters.money(o.finalAmount));
        h.b.getRoot().setOnClickListener(v -> open.open(o.orderId));
    }

    static final class Holder extends RecyclerView.ViewHolder {

        final ItemOrderBinding b;

        Holder(ItemOrderBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }

    private static final DiffUtil.ItemCallback<Models.OrderSummary> DIFF =
        new DiffUtil.ItemCallback<>() {
            @Override
            public boolean areItemsTheSame(
                @NonNull Models.OrderSummary a,
                @NonNull Models.OrderSummary b
            ) {
                return a.orderId == b.orderId;
            }

            @Override
            public boolean areContentsTheSame(
                @NonNull Models.OrderSummary a,
                @NonNull Models.OrderSummary b
            ) {
                return (
                    Objects.equals(a.status, b.status) &&
                    Objects.equals(a.createdAt, b.createdAt) &&
                    Objects.equals(a.finalAmount, b.finalAmount)
                );
            }
        };
}
