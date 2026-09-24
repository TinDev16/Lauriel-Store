package com.lauriel.mobile.ui.order;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.lauriel.mobile.R;
import com.lauriel.mobile.adapter.OrderAdapter;
import com.lauriel.mobile.data.api.ApiClient;
import com.lauriel.mobile.data.model.Models;
import com.lauriel.mobile.data.repository.OrderRepository;
import com.lauriel.mobile.databinding.FragmentOrdersBinding;
import java.util.*;
import retrofit2.*;

public class OrdersFragment extends Fragment {
    private FragmentOrdersBinding b;
    private OrderAdapter adapter;
    private final List<Models.OrderSummary> orders = new ArrayList<>();
    private boolean loading, hasMore = true;
    private int page, generation;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentOrdersBinding.inflate(i, c, false);
        return b.getRoot();
    }
    @Override public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        adapter = new OrderAdapter(id -> startActivity(new Intent(requireContext(), OrderDetailActivity.class)
                .putExtra("orderId", id)));
        b.list.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.list.setAdapter(adapter);
        b.loadMore.setOnClickListener(w -> load());
        b.empty.setOnClickListener(w -> load());
        refresh();
    }
    @Override public void onResume() {
        super.onResume();
        if (b != null && page > 0) refresh();
    }
    private void refresh() {
        generation++;
        orders.clear();
        page = 0;
        hasMore = true;
        loading = false;
        adapter.submit(orders);
        load();
    }
    private void load() {
        if (loading || !hasMore) return;
        loading = true;
        b.loading.setVisibility(View.VISIBLE);
        b.error.setText("");
        int requestGeneration = generation;
        new OrderRepository(ApiClient.get(requireContext()).api()).list(page,
                new Callback<Models.Page<Models.OrderSummary>>() {
            @Override public void onResponse(Call<Models.Page<Models.OrderSummary>> c,
                    Response<Models.Page<Models.OrderSummary>> r) {
                if (b == null || requestGeneration != generation) return;
                loading = false;
                b.loading.setVisibility(View.GONE);
                if (!r.isSuccessful() || r.body() == null) {
                    b.error.setText(ApiClient.get(requireContext()).error(r));
                    update();
                    return;
                }
                var result = r.body();
                Set<Long> ids = new HashSet<>();
                for (var old : orders) ids.add(old.orderId);
                if (result.content != null) for (var order : result.content) {
                    if (ids.add(order.orderId)) orders.add(order);
                }
                page = result.page + 1;
                hasMore = page < result.totalPages;
                update();
            }
            @Override public void onFailure(Call<Models.Page<Models.OrderSummary>> c, Throwable error) {
                if (b == null || requestGeneration != generation) return;
                loading = false;
                b.loading.setVisibility(View.GONE);
                b.error.setText(R.string.network_error);
                update();
            }
        });
    }
    private void update() {
        adapter.submit(orders);
        b.empty.setVisibility(orders.isEmpty() ? View.VISIBLE : View.GONE);
        b.stateIcon.setVisibility(orders.isEmpty() ? View.VISIBLE : View.GONE);
        b.loadMore.setVisibility(hasMore && !orders.isEmpty() ? View.VISIBLE : View.GONE);
    }
    @Override public void onDestroyView() { super.onDestroyView(); b = null; }
}
