package com.lauriel.mobile.ui.order;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.android.material.chip.Chip;
import com.lauriel.mobile.R;
import com.lauriel.mobile.adapter.OrderAdapter;
import com.lauriel.mobile.data.api.ApiClient;
import com.lauriel.mobile.data.model.Models;
import com.lauriel.mobile.data.repository.OrderRepository;
import com.lauriel.mobile.databinding.FragmentOrdersBinding;
import java.util.*;
import retrofit2.*;

public class OrdersFragment extends Fragment {
    private static final String[][] FILTERS={{"Tất cả",""},{"Chờ xác nhận","PENDING"},
            {"Đã xác nhận","CONFIRMED"},{"Đang xử lý","PROCESSING"},{"Đang giao","SHIPPING"},
            {"Đã giao","DELIVERED"},{"Đã hủy","CANCELLED"}};
    private FragmentOrdersBinding b;
    private OrderAdapter adapter;
    private final List<Models.OrderSummary> orders=new ArrayList<>();
    private boolean loading,hasMore=true;
    private int page,generation;
    private String status;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater i,@Nullable ViewGroup c,@Nullable Bundle s){
        b=FragmentOrdersBinding.inflate(i,c,false);return b.getRoot();
    }
    @Override public void onViewCreated(@NonNull View v,@Nullable Bundle s){
        adapter=new OrderAdapter(id->startActivity(new Intent(requireContext(),OrderDetailActivity.class).putExtra("orderId",id)));
        b.list.setLayoutManager(new LinearLayoutManager(requireContext()));b.list.setAdapter(adapter);
        b.loadMore.setOnClickListener(w->load());b.empty.setOnClickListener(w->load());
        createFilters();refresh();
    }
    private void createFilters(){
        for(int i=0;i<FILTERS.length;i++){
            Chip chip=new Chip(requireContext());chip.setId(View.generateViewId());chip.setText(FILTERS[i][0]);
            chip.setCheckable(true);chip.setTag(FILTERS[i][1]);chip.setMinHeight(dp(40));
            int[][] states={{android.R.attr.state_checked},new int[]{}};
            chip.setChipBackgroundColor(new ColorStateList(states,new int[]{
                    ContextCompat.getColor(requireContext(),R.color.ink),
                    ContextCompat.getColor(requireContext(),R.color.surface)}));
            chip.setTextColor(new ColorStateList(states,new int[]{
                    ContextCompat.getColor(requireContext(),R.color.on_accent),
                    ContextCompat.getColor(requireContext(),R.color.ink)}));
            chip.setChipStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(requireContext(),R.color.divider)));
            chip.setChipStrokeWidth(dp(1));chip.setEnsureMinTouchTargetSize(false);
            chip.setOnClickListener(v->{String selected=(String)v.getTag();status=selected.isEmpty()?null:selected;refresh();});
            b.statuses.addView(chip);if(i==0)chip.setChecked(true);
        }
    }
    @Override public void onResume(){super.onResume();if(b!=null&&page>0)refresh();}
    private void refresh(){generation++;orders.clear();page=0;hasMore=true;loading=false;adapter.submit(orders);load();}
    private void load(){
        if(loading||!hasMore)return;loading=true;b.loading.setVisibility(View.VISIBLE);b.error.setText("");
        int requestGeneration=generation;
        new OrderRepository(ApiClient.get(requireContext()).api()).list(page,status,new Callback<Models.Page<Models.OrderSummary>>(){
            @Override public void onResponse(Call<Models.Page<Models.OrderSummary>> c,Response<Models.Page<Models.OrderSummary>> r){
                if(b==null||requestGeneration!=generation)return;loading=false;b.loading.setVisibility(View.GONE);
                if(!r.isSuccessful()||r.body()==null){b.error.setText(ApiClient.get(requireContext()).error(r));update();return;}
                var result=r.body();Set<Long> ids=new HashSet<>();for(var old:orders)ids.add(old.orderId);
                if(result.content!=null)for(var order:result.content)if(ids.add(order.orderId))orders.add(order);
                page=result.page+1;hasMore=page<result.totalPages;update();
            }
            @Override public void onFailure(Call<Models.Page<Models.OrderSummary>> c,Throwable e){
                if(b==null||requestGeneration!=generation)return;loading=false;b.loading.setVisibility(View.GONE);b.error.setText(R.string.network_error);update();
            }
        });
    }
    private void update(){
        adapter.submit(orders);b.empty.setVisibility(orders.isEmpty()?View.VISIBLE:View.GONE);
        b.stateIcon.setVisibility(orders.isEmpty()?View.VISIBLE:View.GONE);
        b.loadMore.setVisibility(hasMore&&!orders.isEmpty()?View.VISIBLE:View.GONE);
    }
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    @Override public void onDestroyView(){super.onDestroyView();b=null;}
}
