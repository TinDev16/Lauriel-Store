package com.lauriel.mobile.adapter;
import android.view.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.lauriel.mobile.R;
import com.lauriel.mobile.data.model.Models;
import com.lauriel.mobile.databinding.ItemProductBinding;
import com.lauriel.mobile.util.Formatters;
import java.util.*;
public final class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.Holder> {
    public interface Open {void open(Models.Product product);}
    private final List<Models.Product> items=new ArrayList<>();
    private final Open onOpen;
    public ProductAdapter(Open onOpen){this.onOpen=onOpen;}
    public void submit(List<Models.Product> products){items.clear();if(products!=null)items.addAll(products);notifyDataSetChanged();}
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent,int type){
        return new Holder(ItemProductBinding.inflate(LayoutInflater.from(parent.getContext()),parent,false));
    }
    @Override public void onBindViewHolder(@NonNull Holder holder,int position){
        Models.Product p=items.get(position);
        holder.b.name.setText(p.productName);
        holder.b.price.setText(Formatters.money(p.price)); // Summary endpoint has only base price.
        Glide.with(holder.b.image).load(p.imgUrl).placeholder(R.drawable.image_placeholder)
                .error(R.drawable.image_placeholder).centerCrop().into(holder.b.image);
        holder.b.getRoot().setOnClickListener(v->onOpen.open(p));
    }
    @Override public int getItemCount(){return items.size();}
    static final class Holder extends RecyclerView.ViewHolder {
        final ItemProductBinding b; Holder(ItemProductBinding b){super(b.getRoot());this.b=b;}
    }
}
