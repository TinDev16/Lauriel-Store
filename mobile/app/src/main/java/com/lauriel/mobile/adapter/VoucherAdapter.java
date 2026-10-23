package com.lauriel.mobile.adapter;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.lauriel.mobile.R;
import com.lauriel.mobile.data.model.Models;
import com.lauriel.mobile.databinding.ItemVoucherBinding;
import com.lauriel.mobile.util.Formatters;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class VoucherAdapter
    extends RecyclerView.Adapter<VoucherAdapter.Holder>
{

    public interface Listener {
        void onSave(Models.Voucher voucher);
        void onCopy(Models.Voucher voucher);
    }

    private final Listener listener;
    private final List<Models.Voucher> items = new ArrayList<>();
    private Set<Long> savedIds = Set.of();

    public VoucherAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Models.Voucher> values, Set<Long> saved) {
        items.clear();
        if (values != null) items.addAll(values);
        savedIds = new HashSet<>(saved);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new Holder(
            ItemVoucherBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
            )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Models.Voucher voucher = items.get(position);
        holder.binding.name.setText(voucher.voucherName);
        holder.binding.code.setText(voucher.code);
        holder.binding.discount.setText(discount(voucher));
        holder.binding.condition.setText(condition(voucher));
        holder.binding.expiry.setText(
            "Hết hạn: " + Formatters.date(voucher.endDate)
        );

        boolean saved = savedIds.contains(voucher.voucherId);
        var context = holder.binding.getRoot().getContext();
        int accent = ContextCompat.getColor(
            context,
            saved ? R.color.success : R.color.ink
        );
        holder.binding.savedBadge.setVisibility(
            saved ? View.VISIBLE : View.GONE
        );
        holder.binding.card.setStrokeColor(
            ContextCompat.getColor(
                context,
                saved ? R.color.success : R.color.divider
            )
        );
        holder.binding.card.setStrokeWidth(
            saved ? dp(context, 2) : dp(context, 1)
        );
        holder.binding.card.setCardBackgroundColor(
            ContextCompat.getColor(
                context,
                saved ? R.color.success_surface : R.color.surface
            )
        );
        holder.binding.save.setText(saved ? "Đã lưu" : "Lưu mã");
        holder.binding.save.setTextColor(accent);
        holder.binding.save.setIconTint(ColorStateList.valueOf(accent));
        holder.binding.save.setIconResource(
            saved ? R.drawable.ic_check : R.drawable.ic_bookmark
        );
        holder.binding.save.setOnClickListener(view ->
            listener.onSave(voucher)
        );
        holder.binding.copy.setOnClickListener(view ->
            listener.onCopy(voucher)
        );
    }

    private String discount(Models.Voucher voucher) {
        if ("PERCENT".equals(voucher.discountType)) {
            String value =
                voucher.discountValue == null
                    ? "0"
                    : voucher.discountValue
                          .stripTrailingZeros()
                          .toPlainString();
            String maximum =
                voucher.maxDiscountAmount == null
                    ? ""
                    : " · Tối đa " +
                      Formatters.money(voucher.maxDiscountAmount);
            return "Giảm " + value + "%" + maximum;
        }
        return "Giảm " + Formatters.money(voucher.discountValue);
    }

    private String condition(Models.Voucher voucher) {
        BigDecimal minimum =
            voucher.minOrderAmount == null
                ? BigDecimal.ZERO
                : voucher.minOrderAmount;
        return minimum.signum() <= 0
            ? "Áp dụng cho mọi đơn hàng"
            : "Đơn tối thiểu " + Formatters.money(minimum);
    }

    private static int dp(android.content.Context context, int value) {
        return Math.round(
            value * context.getResources().getDisplayMetrics().density
        );
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {

        final ItemVoucherBinding binding;

        Holder(ItemVoucherBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
