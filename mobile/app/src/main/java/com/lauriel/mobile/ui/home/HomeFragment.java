package com.lauriel.mobile.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import com.lauriel.mobile.R;
import com.lauriel.mobile.adapter.ProductAdapter;
import com.lauriel.mobile.data.api.ApiClient;
import com.lauriel.mobile.data.model.Models;
import com.lauriel.mobile.data.repository.ProductRepository;
import com.lauriel.mobile.databinding.FragmentHomeBinding;
import com.lauriel.mobile.ui.MainActivity;
import com.lauriel.mobile.ui.common.GridSpacingDecoration;
import com.lauriel.mobile.ui.notification.NotificationActivity;
import com.lauriel.mobile.ui.product.ProductDetailActivity;
import retrofit2.*;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle state) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        var repository = new ProductRepository(ApiClient.get(requireContext()).api());
        var adapter = new ProductAdapter(product -> startActivity(
                new Intent(requireContext(), ProductDetailActivity.class)
                        .putExtra("productId", product.productId)));
        binding.list.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.list.addItemDecoration(new GridSpacingDecoration(dp(8), 2));
        binding.list.setAdapter(adapter);
        binding.list.setHasFixedSize(true);
        binding.list.setLayoutAnimation(android.view.animation.AnimationUtils.loadLayoutAnimation(
                requireContext(), R.anim.layout_fade_in));
        binding.shop.setOnClickListener(item ->
                ((MainActivity) requireActivity()).showTab(R.id.nav_shop));
        binding.notifications.setOnClickListener(item ->
                startActivity(new Intent(requireContext(), NotificationActivity.class)));
        binding.error.setOnClickListener(item -> loadProducts(repository, adapter));
        binding.loadMore.setOnClickListener(item ->
                ((MainActivity) requireActivity()).showTab(R.id.nav_shop));
        loadProducts(repository, adapter);
        loadPromotions(repository);
    }

    private void loadPromotions(ProductRepository repository) {
        repository.promotions(new Callback<Models.Page<Models.Promotion>>() {
            @Override public void onResponse(Call<Models.Page<Models.Promotion>> call,
                    Response<Models.Page<Models.Promotion>> response) {
                if (binding == null) return;
                if (response.isSuccessful() && response.body() != null
                        && response.body().content != null && !response.body().content.isEmpty()) {
                    StringBuilder names = new StringBuilder();
                    for (var promotion : response.body().content) {
                        names.append("• ").append(promotion.promotionName).append(" · giảm ")
                                .append(promotion.discountPercent).append("%\n");
                    }
                    binding.promotions.setText(names.toString().trim());
                } else hidePromotions();
            }
            @Override public void onFailure(Call<Models.Page<Models.Promotion>> call, Throwable error) {
                hidePromotions();
            }
        });
    }

    private void loadProducts(ProductRepository repository, ProductAdapter adapter) {
        if (binding == null) return;
        binding.loading.setVisibility(View.VISIBLE);
        binding.loadMore.setVisibility(View.GONE);
        binding.error.setText("");
        binding.empty.setVisibility(View.GONE);
        binding.stateIcon.setVisibility(View.GONE);
        repository.list(0, null, null, null, null, new Callback<Models.Page<Models.Product>>() {
            @Override public void onResponse(Call<Models.Page<Models.Product>> call,
                    Response<Models.Page<Models.Product>> response) {
                if (binding == null) return;
                binding.loading.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    adapter.submit(response.body().content);
                    boolean empty = adapter.getItemCount() == 0;
                    binding.empty.setVisibility(empty ? View.VISIBLE : View.GONE);
                    binding.stateIcon.setVisibility(empty ? View.VISIBLE : View.GONE);
                    binding.loadMore.setVisibility(response.body().totalPages > 1 ? View.VISIBLE : View.GONE);
                } else {
                    binding.error.setText("Không thể tải sản phẩm. Chạm để thử lại.");
                    binding.stateIcon.setVisibility(View.VISIBLE);
                }
            }
            @Override public void onFailure(Call<Models.Page<Models.Product>> call, Throwable error) {
                if (binding != null) {
                    binding.loading.setVisibility(View.GONE);
                    binding.error.setText("Không thể kết nối máy chủ. Chạm để thử lại.");
                    binding.stateIcon.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void hidePromotions() {
        if (binding != null) {
            binding.promotionTitle.setVisibility(View.GONE);
            binding.promotions.setVisibility(View.GONE);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}