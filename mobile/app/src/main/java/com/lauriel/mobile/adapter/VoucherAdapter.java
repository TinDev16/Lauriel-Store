package com.lauriel.mobile.adapter;

import android.view.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.lauriel.mobile.data.model.Models;
import com.lauriel.mobile.databinding.ItemVoucherBinding;
import com.lauriel.mobile.util.Formatters;
import java.math.BigDecimal;
import java.util.*;

public final class VoucherAdapter extends RecyclerView.Adapter<VoucherAdapter.Holder> {
    public interface Listener { void onSave(Models.Voucher voucher); }
    private final Listener listener;
    private final List<Models.Voucher> items = new ArrayList<>();
    private Set<Long> savedIds = Set.of();

    public VoucherAdapter(Listener listener) { this.listener = listener; }

    public void submit(List<Models.Voucher> values, Set<Long> saved) {
        items.clear();
        if (values != null) items.addAll(values);
        savedIds = new HashSet<>(saved);
        notifyDataSetChanged();
    }

    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new Holder(ItemVoucherBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
        Models.Voucher voucher = items.get(position);
        holder.binding.name.setText(voucher.voucherName);
        holder.binding.code.setText(voucher.code);
        holder.binding.discount.setText(discount(voucher));
        holder.binding.condition.setText(condition(voucher));
        holder.binding.expiry.setText("Hết hạn: " + Formatters.date(voucher.endDate));
        boolean saved = savedIds.contains(voucher.voucherId);
        holder.binding.save.setText(saved ? "Đã lưu" : "Lưu mã");
        holder.binding.save.setIconResource(saved
                ? com.lauriel.mobile.R.drawable.ic_check
                : com.lauriel.mobile.R.drawable.ic_bookmark);
        holder.binding.save.setOnClickListener(view -> listener.onSave(voucher));
    }

    private String discount(Models.Voucher voucher) {
        if ("PERCENT".equals(voucher.discountType)) {
            String value = voucher.discountValue == null ? "0" : voucher.discountValue.stripTrailingZeros().toPlainString();
            String maximum = voucher.maxDiscountAmount == null ? "" : " · Tối đa " + Formatters.money(voucher.maxDiscountAmount);
            return "Giảm " + value + "%" + maximum;
        }
        return "Giảm " + Formatters.money(voucher.discountValue);
    }

    private String condition(Models.Voucher voucher) {
        BigDecimal minimum = voucher.minOrderAmount == null ? BigDecimal.ZERO : voucher.minOrderAmount;
        return minimum.signum() <= 0 ? "Áp dụng cho mọi đơn hàng" : "Đơn tối thiểu " + Formatters.money(minimum);
    }

    @Override public int getItemCount() { return items.size(); }
    static final class Holder extends RecyclerView.ViewHolder {
        final ItemVoucherBinding binding;
        Holder(ItemVoucherBinding binding) { super(binding.getRoot()); this.binding = binding; }
    }
}