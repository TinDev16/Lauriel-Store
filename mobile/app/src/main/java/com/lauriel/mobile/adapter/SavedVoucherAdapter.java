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
import com.lauriel.mobile.databinding.ItemCheckoutVoucherBinding;
import com.lauriel.mobile.util.Formatters;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Renders saved vouchers as distinct, selectable cards in the checkout bottom sheet. */
public final class SavedVoucherAdapter
    extends RecyclerView.Adapter<SavedVoucherAdapter.Holder>
{

    public interface Listener {
        void onSelectionChanged(Models.Voucher voucher);
    }

    private final Listener listener;
    private final List<Models.Voucher> items = new ArrayList<>();
    private BigDecimal eligibleAmount = BigDecimal.ZERO;
    private long selectedId = -1;

    public SavedVoucherAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(
        List<Models.Voucher> values,
        BigDecimal amount,
        String selectedCode
    ) {
        eligibleAmount = amount == null ? BigDecimal.ZERO : amount;
        items.clear();
        if (values != null) items.addAll(values);
        items.sort(
            Comparator.comparing(
                (Models.Voucher voucher) -> !isEligible(voucher)
            ).thenComparing(voucher ->
                voucher.endDate == null ? "" : voucher.endDate
            )
        );
        selectedId = -1;
        if (selectedCode != null && !selectedCode.isBlank()) {
            for (Models.Voucher voucher : items) {
                if (
                    selectedCode.equalsIgnoreCase(voucher.code) &&
                    isEligible(voucher)
                ) {
                    selectedId = voucher.voucherId;
                    break;
                }
            }
        }
        notifyDataSetChanged();
    }

    public Models.Voucher selected() {
        for (Models.Voucher voucher : items) {
            if (voucher.voucherId == selectedId) return voucher;
        }
        return null;
    }

    public int eligibleCount() {
        int count = 0;
        for (Models.Voucher voucher : items) if (isEligible(voucher)) count++;
        return count;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(
            ItemCheckoutVoucherBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
            )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Models.Voucher voucher = items.get(position);
        boolean eligible = isEligible(voucher);
        boolean selected = voucher.voucherId == selectedId;
        var context = holder.binding.getRoot().getContext();

        holder.binding.code.setText(voucher.code);
        holder.binding.name.setText(voucher.voucherName);
        holder.binding.discount.setText(discount(voucher));
        holder.binding.condition.setText(condition(voucher));
        holder.binding.expiry.setText(
            "Hạn dùng: " + Formatters.date(voucher.endDate)
        );
        holder.binding.selectedIcon.setVisibility(
            selected ? View.VISIBLE : View.GONE
        );

        int stroke = ContextCompat.getColor(
            context,
            selected
                ? R.color.success
                : eligible
                  ? R.color.divider
                  : R.color.surface_strong
        );
        holder.binding.card.setStrokeColor(stroke);
        holder.binding.card.setStrokeWidth(dp(context, selected ? 2 : 1));
        holder.binding.card.setCardBackgroundColor(
            ContextCompat.getColor(
                context,
                selected ? R.color.success_surface : R.color.background
            )
        );
        holder.binding.ticketMark.setBackgroundTintList(
            ColorStateList.valueOf(
                ContextCompat.getColor(
                    context,
                    eligible ? R.color.ink : R.color.muted
                )
            )
        );
        holder.binding.card.setAlpha(eligible ? 1f : 0.66f);
        holder.binding.card.setEnabled(eligible);
        holder.binding.card.setClickable(eligible);

        if (eligible) {
            holder.binding.eligibility.setText(
                selected ? "Đang chọn" : "Có thể sử dụng"
            );
            holder.binding.eligibility.setTextColor(
                ContextCompat.getColor(context, R.color.success)
            );
            holder.binding.eligibility.setBackgroundResource(
                R.drawable.bg_saved_badge
            );
        } else {
            BigDecimal shortfall = minimum(voucher).subtract(eligibleAmount);
            holder.binding.eligibility.setText(
                "Cần thêm " + Formatters.money(shortfall)
            );
            holder.binding.eligibility.setTextColor(
                ContextCompat.getColor(context, R.color.muted)
            );
            holder.binding.eligibility.setBackgroundResource(
                R.drawable.bg_unavailable_badge
            );
        }

        holder.binding.card.setContentDescription(
            voucher.code +
                ", " +
                voucher.voucherName +
                ", " +
                holder.binding.eligibility.getText()
        );
        holder.binding.card.setOnClickListener(view -> {
            long oldId = selectedId;
            selectedId = voucher.voucherId;
            notifySelectionChanged(oldId, selectedId);
            listener.onSelectionChanged(voucher);
        });
    }

    private void notifySelectionChanged(long oldId, long newId) {
        for (int index = 0; index < items.size(); index++) {
            long id = items.get(index).voucherId;
            if (id == oldId || id == newId) notifyItemChanged(index);
        }
    }

    private boolean isEligible(Models.Voucher voucher) {
        return eligibleAmount.compareTo(minimum(voucher)) >= 0;
    }

    private static BigDecimal minimum(Models.Voucher voucher) {
        return voucher.minOrderAmount == null
            ? BigDecimal.ZERO
            : voucher.minOrderAmount;
    }

    private static String discount(Models.Voucher voucher) {
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

    private static String condition(Models.Voucher voucher) {
        BigDecimal minimum = minimum(voucher);
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

        final ItemCheckoutVoucherBinding binding;

        Holder(ItemCheckoutVoucherBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
