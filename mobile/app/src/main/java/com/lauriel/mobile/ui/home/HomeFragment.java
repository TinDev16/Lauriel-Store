package com.lauriel.mobile.ui.home;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.chip.Chip;
import com.lauriel.mobile.R;
import com.lauriel.mobile.adapter.ProductAdapter;
import com.lauriel.mobile.adapter.PromotionOfferAdapter;
import com.lauriel.mobile.data.api.ApiClient;
import com.lauriel.mobile.data.model.Models;
import com.lauriel.mobile.data.repository.ProductRepository;
import com.lauriel.mobile.databinding.FragmentHomeBinding;
import com.lauriel.mobile.ui.MainActivity;
import com.lauriel.mobile.ui.common.GridSpacingDecoration;
import com.lauriel.mobile.ui.notification.NotificationActivity;
import com.lauriel.mobile.ui.product.ProductDetailActivity;
import com.lauriel.mobile.util.Formatters;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding binding;
    private ProductRepository repository;
    private ProductAdapter adapter;
    private PromotionOfferAdapter promotionAdapter;
    private final EnumMap<PromotionOfferAdapter.Type, PromotionOfferAdapter.Offer> offers =
            new EnumMap<>(PromotionOfferAdapter.Type.class);
    private int pendingPromotionRequests;
    private int refreshGeneration;
    private int productRequestGeneration;
    private Long selectedCategoryId;
    private String selectedCategoryName;
    private boolean buildingCategoryMenu;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle state) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        repository = new ProductRepository(ApiClient.get(requireContext()).api());
        adapter = new ProductAdapter(product -> startActivity(
                new Intent(requireContext(), ProductDetailActivity.class)
                        .putExtra("productId", product.productId)));
        binding.list.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.list.addItemDecoration(new GridSpacingDecoration(dp(8), 2));
        binding.list.setAdapter(adapter);
        binding.list.setHasFixedSize(true);
        binding.list.setLayoutAnimation(android.view.animation.AnimationUtils.loadLayoutAnimation(
                requireContext(), R.anim.layout_fade_in));

        promotionAdapter = new PromotionOfferAdapter(new PromotionOfferAdapter.Listener() {
            @Override public void onOpen(PromotionOfferAdapter.Offer offer) {
                showPromotionDetail(offer);
            }
            @Override public void onAction(PromotionOfferAdapter.Offer offer) {
                handlePromotionAction(offer);
            }
        });
        binding.promotions.setLayoutManager(new LinearLayoutManager(
                requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.promotions.setAdapter(promotionAdapter);
        new PagerSnapHelper().attachToRecyclerView(binding.promotions);

        binding.categoriesToggle.setOnClickListener(item -> toggleCategoryMenu());
        binding.categoryList.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (buildingCategoryMenu || checkedIds.isEmpty()) return;
            View selected = group.findViewById(checkedIds.get(0));
            Object tag = selected == null ? null : selected.getTag();
            if (tag instanceof Models.Category category) {
                selectCategory(category.categoryId, category.categoryName);
            } else {
                selectCategory(null, null);
            }
        });
        binding.shop.setOnClickListener(item ->
                ((MainActivity) requireActivity()).showTab(R.id.nav_shop));
        binding.notifications.setOnClickListener(item ->
                startActivity(new Intent(requireContext(), NotificationActivity.class)));
        binding.error.setOnClickListener(item -> refresh());
        binding.loadMore.setOnClickListener(item ->
                ((MainActivity) requireActivity()).showTab(R.id.nav_shop));
        refresh();
    }

    public void refresh() {
        if (binding == null || repository == null || adapter == null) return;
        int generation = ++refreshGeneration;
        binding.list.scrollToPosition(0);
        loadProducts(++productRequestGeneration);
        loadPromotions(generation);
        loadCategories(generation);
    }

    private void toggleCategoryMenu() {
        boolean show = binding.categoryPanel.getVisibility() != View.VISIBLE;
        binding.categoryPanel.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.categoriesToggle.setSelected(show);
        binding.categoriesToggle.setContentDescription(getString(show
                ? R.string.close_category_menu : R.string.open_category_menu));
    }

    private void loadCategories(int generation) {
        repository.categories(new Callback<List<Models.Category>>() {
            @Override public void onResponse(Call<List<Models.Category>> call,
                    Response<List<Models.Category>> response) {
                if (binding == null || generation != refreshGeneration) return;
                if (!response.isSuccessful() || response.body() == null) return;
                renderCategoryMenu(response.body());
            }
            @Override public void onFailure(Call<List<Models.Category>> call, Throwable error) {
                // Product and promotion content remains usable when category loading fails.
            }
        });
    }

    private void renderCategoryMenu(List<Models.Category> categories) {
        buildingCategoryMenu = true;
        binding.categoryList.removeAllViews();
        Chip all = categoryChip(getString(R.string.all_products), null,
                selectedCategoryId == null);
        binding.categoryList.addView(all);
        boolean selectedStillExists = selectedCategoryId == null;
        for (Models.Category category : categories) {
            boolean checked = selectedCategoryId != null
                    && selectedCategoryId == category.categoryId;
            selectedStillExists |= checked;
            binding.categoryList.addView(categoryChip(category.categoryName, category, checked));
        }
        if (!selectedStillExists) {
            selectedCategoryId = null;
            selectedCategoryName = null;
            all.setChecked(true);
            updateProductSectionTitle();
            loadProducts(++productRequestGeneration);
        }
        buildingCategoryMenu = false;
    }

    private Chip categoryChip(String text, Models.Category category, boolean checked) {
        Chip chip = new Chip(requireContext());
        chip.setId(View.generateViewId());
        chip.setText(text);
        chip.setTag(category);
        chip.setCheckable(true);
        chip.setChecked(checked);
        chip.setMinHeight(dp(48));
        chip.setEnsureMinTouchTargetSize(true);
        chip.setContentDescription("Lọc theo " + text);
        return chip;
    }

    private void selectCategory(Long categoryId, String categoryName) {
        if (java.util.Objects.equals(selectedCategoryId, categoryId)) {
            binding.categoryPanel.setVisibility(View.GONE);
            binding.categoriesToggle.setSelected(false);
            binding.categoriesToggle.setContentDescription(getString(R.string.open_category_menu));
            return;
        }
        selectedCategoryId = categoryId;
        selectedCategoryName = categoryName;
        updateProductSectionTitle();
        binding.categoryPanel.setVisibility(View.GONE);
        binding.categoriesToggle.setSelected(false);
        binding.categoriesToggle.setContentDescription(getString(R.string.open_category_menu));
        binding.list.scrollToPosition(0);
        loadProducts(++productRequestGeneration);
    }

    private void updateProductSectionTitle() {
        binding.productSectionTitle.setText(selectedCategoryName == null
                ? getString(R.string.featured_products) : selectedCategoryName);
    }

    private void loadPromotions(int generation) {
        offers.clear();
        promotionAdapter.submit(List.of());
        pendingPromotionRequests = 3;
        binding.promotionTitle.setVisibility(View.VISIBLE);
        binding.promotionHint.setVisibility(View.VISIBLE);
        binding.promotions.setVisibility(View.VISIBLE);

        repository.promotions(new Callback<Models.Page<Models.Promotion>>() {
            @Override public void onResponse(Call<Models.Page<Models.Promotion>> call,
                    Response<Models.Page<Models.Promotion>> response) {
                if (binding == null || generation != refreshGeneration) return;
                if (response.isSuccessful() && response.body() != null
                        && response.body().content != null && !response.body().content.isEmpty()) {
                    Models.Promotion best = response.body().content.get(0);
                    for (Models.Promotion value : response.body().content) {
                        if (number(value.discountPercent).compareTo(number(best.discountPercent)) > 0) best = value;
                    }
                    offers.put(PromotionOfferAdapter.Type.PRODUCT, new PromotionOfferAdapter.Offer(
                            PromotionOfferAdapter.Type.PRODUCT, "GIẢM GIÁ SẢN PHẨM", best.promotionName,
                            "Giảm " + plain(best.discountPercent) + "%",
                            best.description == null || best.description.isBlank()
                                    ? "Áp dụng cho sản phẩm trong chương trình" : best.description,
                            "Xem sản phẩm", null));
                }
                promotionRequestFinished(generation);
            }
            @Override public void onFailure(Call<Models.Page<Models.Promotion>> call, Throwable error) {
                promotionRequestFinished(generation);
            }
        });

        ApiClient.get(requireContext()).api().activeOrderPromotions(0, 20)
                .enqueue(new Callback<Models.Page<Models.OrderPromotion>>() {
            @Override public void onResponse(Call<Models.Page<Models.OrderPromotion>> call,
                    Response<Models.Page<Models.OrderPromotion>> response) {
                if (binding == null || generation != refreshGeneration) return;
                if (response.isSuccessful() && response.body() != null
                        && response.body().content != null && !response.body().content.isEmpty()) {
                    Models.OrderPromotion value = response.body().content.get(0);
                    String maximum = value.maxDiscountAmount == null
                            ? "" : " · tối đa " + Formatters.money(value.maxDiscountAmount);
                    offers.put(PromotionOfferAdapter.Type.ORDER, new PromotionOfferAdapter.Offer(
                            PromotionOfferAdapter.Type.ORDER, "KHUYẾN MÃI ĐƠN HÀNG", value.promotionName,
                            "Giảm " + plain(value.discountPercent) + "%" + maximum,
                            "Đơn từ " + Formatters.money(number(value.minOrderAmount)) + " · Tự động áp dụng",
                            "Mua ngay", null));
                }
                promotionRequestFinished(generation);
            }
            @Override public void onFailure(Call<Models.Page<Models.OrderPromotion>> call, Throwable error) {
                promotionRequestFinished(generation);
            }
        });

        ApiClient.get(requireContext()).api().activeVouchers(0, 20)
                .enqueue(new Callback<Models.Page<Models.Voucher>>() {
            @Override public void onResponse(Call<Models.Page<Models.Voucher>> call,
                    Response<Models.Page<Models.Voucher>> response) {
                if (binding == null || generation != refreshGeneration) return;
                if (response.isSuccessful() && response.body() != null
                        && response.body().content != null && !response.body().content.isEmpty()) {
                    Models.Voucher value = response.body().content.get(0);
                    String benefit = "PERCENT".equals(value.discountType)
                            ? "Giảm " + plain(value.discountValue) + "%"
                            : "Giảm " + Formatters.money(number(value.discountValue));
                    offers.put(PromotionOfferAdapter.Type.VOUCHER, new PromotionOfferAdapter.Offer(
                            PromotionOfferAdapter.Type.VOUCHER, "MÃ GIẢM GIÁ", value.voucherName,
                            benefit, "Mã " + value.code + " · Đơn từ "
                                    + Formatters.money(number(value.minOrderAmount)) + expiry(value.endDate),
                            "Sao chép", value.code));
                }
                promotionRequestFinished(generation);
            }
            @Override public void onFailure(Call<Models.Page<Models.Voucher>> call, Throwable error) {
                promotionRequestFinished(generation);
            }
        });
    }

    private void promotionRequestFinished(int generation) {
        if (binding == null || generation != refreshGeneration) return;
        pendingPromotionRequests--;
        List<PromotionOfferAdapter.Offer> values = new ArrayList<>();
        for (PromotionOfferAdapter.Type type : PromotionOfferAdapter.Type.values()) {
            PromotionOfferAdapter.Offer offer = offers.get(type);
            if (offer != null) values.add(offer);
        }
        promotionAdapter.submit(values);
        if (pendingPromotionRequests == 0 && values.isEmpty()) hidePromotions();
    }

    private void handlePromotionAction(PromotionOfferAdapter.Offer offer) {
        if (offer.copyText != null) {
            ClipboardManager clipboard = (ClipboardManager) requireContext()
                    .getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("Mã giảm giá", offer.copyText));
            Toast.makeText(requireContext(), "Đã sao chép mã " + offer.copyText, Toast.LENGTH_SHORT).show();
            return;
        }
        ((MainActivity) requireActivity()).showTab(R.id.nav_shop);
    }

    private void showPromotionDetail(PromotionOfferAdapter.Offer offer) {
        String message = offer.benefit + "\n\n" + offer.condition;
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(offer.title)
                .setMessage(message)
                .setNegativeButton("Đóng", null);
        if (offer.action != null) {
            dialog.setPositiveButton(offer.action,
                    (ignored, which) -> handlePromotionAction(offer));
        }
        dialog.show();
    }

    private static BigDecimal number(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String plain(BigDecimal value) {
        return number(value).stripTrailingZeros().toPlainString();
    }

    private static String expiry(String value) {
        if (value == null || value.isBlank()) return "";
        return " · HSD " + value.substring(0, Math.min(10, value.length()));
    }

    private void loadProducts(int generation) {
        if (binding == null || generation != productRequestGeneration) return;
        binding.loading.setVisibility(View.VISIBLE);
        binding.loadMore.setVisibility(View.GONE);
        binding.error.setText("");
        binding.empty.setVisibility(View.GONE);
        binding.stateIcon.setVisibility(View.GONE);
        repository.list(0, null, selectedCategoryId, null, null, new Callback<Models.Page<Models.Product>>() {
            @Override public void onResponse(Call<Models.Page<Models.Product>> call,
                    Response<Models.Page<Models.Product>> response) {
                if (binding == null || generation != productRequestGeneration) return;
                binding.loading.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    adapter.submit(response.body().content);
                    boolean empty = adapter.getItemCount() == 0;
                    binding.empty.setVisibility(empty ? View.VISIBLE : View.GONE);
                    binding.empty.setText(selectedCategoryName == null
                            ? getString(R.string.empty_products)
                            : "Chưa có sản phẩm thuộc " + selectedCategoryName + ".");
                    binding.stateIcon.setVisibility(empty ? View.VISIBLE : View.GONE);
                    binding.loadMore.setVisibility(response.body().totalPages > 1 ? View.VISIBLE : View.GONE);
                } else {
                    binding.error.setText("Không thể tải sản phẩm. Chạm để thử lại.");
                    binding.stateIcon.setVisibility(View.VISIBLE);
                }
            }
            @Override public void onFailure(Call<Models.Page<Models.Product>> call, Throwable error) {
                if (binding != null && generation == productRequestGeneration) {
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
            binding.promotionHint.setVisibility(View.GONE);
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
