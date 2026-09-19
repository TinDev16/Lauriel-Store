package com.lauriel.mobile.adapter;
import android.view.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.lauriel.mobile.data.model.Models;
import com.lauriel.mobile.databinding.ItemOrderBinding;
import com.lauriel.mobile.util.Formatters;
import java.util.*;
public final class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.Holder> {
    public interface Open {void open(long id);}
    private final List<Models.OrderSummary> items=new ArrayList<>();private final Open open;
    public OrderAdapter(Open open){this.open=open;}
    public void submit(List<Models.OrderSummary> updated){items.clear();items.addAll(updated);notifyDataSetChanged();}
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup p,int type){
        return new Holder(ItemOrderBinding.inflate(LayoutInflater.from(p.getContext()),p,false));
    }
    @Override public void onBindViewHolder(@NonNull Holder h,int pos){
        var o=items.get(pos);h.b.id.setText("Đơn #"+o.orderId);
        h.b.status.setText(Formatters.status(o.status));h.b.date.setText(Formatters.date(o.createdAt));
        h.b.amount.setText(Formatters.money(o.finalAmount));h.b.getRoot().setOnClickListener(v->open.open(o.orderId));
    }
    @Override public int getItemCount(){return items.size();}
    static final class Holder extends RecyclerView.ViewHolder {final ItemOrderBinding b;Holder(ItemOrderBinding b){super(b.getRoot());this.b=b;}}
}
