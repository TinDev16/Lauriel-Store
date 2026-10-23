package com.lauriel.mobile.ui.product;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.lauriel.mobile.R;
import com.lauriel.mobile.adapter.ProductAdapter;
import com.lauriel.mobile.data.api.ApiClient;
import com.lauriel.mobile.data.model.Models;
import com.lauriel.mobile.data.repository.ProductRepository;
import com.lauriel.mobile.databinding.BottomSheetProductFilterBinding;
import com.lauriel.mobile.databinding.FragmentShopBinding;
import com.lauriel.mobile.ui.common.GridSpacingDecoration;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ShopFragment extends Fragment {

    private FragmentShopBinding b;
    private ShopViewModel vm;
    private ProductRepository products;
    private boolean firstResume = true;

    @Nullable
    @Override
    public View onCreateView(
        @NonNull LayoutInflater inflater,
        @Nullable ViewGroup parent,
        @Nullable Bundle saved
    ) {
        b = FragmentShopBinding.inflate(inflater, parent, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle saved) {
        products = new ProductRepository(ApiClient.get(requireContext()).api());
        vm = new ViewModelProvider(this).get(ShopViewModel.class);
        var adapter = new ProductAdapter(p ->
            startActivity(
                new Intent(
                    requireContext(),
                    ProductDetailActivity.class
                ).putExtra("productId", p.productId)
            )
        );
        b.list.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        b.list.addItemDecoration(new GridSpacingDecoration(dp(8), 2));
        b.list.setAdapter(adapter);
        b.list.setHasFixedSize(true);
        vm.state().observe(getViewLifecycleOwner(), state -> {
            adapter.submit(state.products);
            b.loading.setVisibility(state.loading ? View.VISIBLE : View.GONE);
            b.loadMore.setVisibility(
                state.hasMore && !state.loading && !state.products.isEmpty()
                    ? View.VISIBLE
                    : View.GONE
            );
            boolean noItems = !state.loading && state.products.isEmpty();
            b.empty.setVisibility(noItems ? View.VISIBLE : View.GONE);
            b.stateIcon.setVisibility(noItems ? View.VISIBLE : View.GONE);
            b.empty.setText(
                state.error == null
                    ? R.string.empty_products
                    : R.string.network_error
            );
            b.error.setText(state.error == null ? "" : state.error);
        });
        b.search.setText(vm.keyword());
        b.search.setSingleLine(true);
        b.search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        b.search.setOnEditorActionListener((text, action, event) -> {
            if (action == EditorInfo.IME_ACTION_SEARCH) {
                vm.query(
                    b.search.getText().toString(),
                    vm.categoryId(),
                    vm.sizeId(),
                    vm.colorId(),
                    vm.sort()
                );
                return true;
            }
            return false;
        });
        b.filter.setOnClickListener(v -> showFilters());
        b.loadMore.setOnClickListener(v -> vm.loadMore());
        b.error.setOnClickListener(v -> vm.retry());
        b.empty.setOnClickListener(v -> vm.retry());
        vm.init(products);
    }

    private void showFilters() {
        products.categories(
            new Callback<>() {
                @Override
                public void onResponse(
                    Call<List<Models.Category>> call,
                    Response<List<Models.Category>> response
                ) {
                    if (!valid(response)) {
                        showError();
                        return;
                    }
                    List<Models.Category> categories = response.body();
                    products.sizes(
                        new Callback<>() {
                            @Override
                            public void onResponse(
                                Call<List<Models.Size>> call,
                                Response<List<Models.Size>> sizeResponse
                            ) {
                                if (!valid(sizeResponse)) {
                                    showError();
                                    return;
                                }
                                List<Models.Size> sizes = sizeResponse.body();
                                products.colors(
                                    new Callback<>() {
                                        @Override
                                        public void onResponse(
                                            Call<List<Models.Color>> call,
                                            Response<
                                                List<Models.Color>
                                            > colorResponse
                                        ) {
                                            if (!valid(colorResponse)) {
                                                showError();
                                                return;
                                            }
                                            showSheet(
                                                categories,
                                                sizes,
                                                colorResponse.body()
                                            );
                                        }

                                        @Override
                                        public void onFailure(
                                            Call<List<Models.Color>> call,
                                            Throwable error
                                        ) {
                                            showError();
                                        }
                                    }
                                );
                            }

                            @Override
                            public void onFailure(
                                Call<List<Models.Size>> call,
                                Throwable error
                            ) {
                                showError();
                            }
                        }
                    );
                }

                @Override
                public void onFailure(
                    Call<List<Models.Category>> call,
                    Throwable error
                ) {
                    showError();
                }
            }
        );
    }

    private boolean valid(Response<?> response) {
        return isAdded() && response.isSuccessful() && response.body() != null;
    }

    private void showSheet(
        List<Models.Category> categories,
        List<Models.Size> sizes,
        List<Models.Color> colors
    ) {
        BottomSheetProductFilterBinding sheet =
            BottomSheetProductFilterBinding.inflate(getLayoutInflater());
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(sheet.getRoot());
        for (var category : categories)
            addChip(
                sheet.categories,
                category.categoryName,
                category.categoryId,
                java.util.Objects.equals(vm.categoryId(), category.categoryId)
            );
        for (var size : sizes)
            addChip(
                sheet.sizes,
                size.sizeName,
                size.sizeId,
                java.util.Objects.equals(vm.sizeId(), size.sizeId)
            );
        for (var color : colors)
            addChip(
                sheet.colors,
                color.colorName,
                color.colorId,
                java.util.Objects.equals(vm.colorId(), color.colorId)
            );
        if ("PRICE_ASC".equals(vm.sort())) sheet.sortPriceAsc.setChecked(true);
        else if ("PRICE_DESC".equals(vm.sort())) sheet.sortPriceDesc.setChecked(
            true
        );
        else sheet.sortDefault.setChecked(true);
        sheet.reset.setOnClickListener(v -> {
            sheet.categories.clearCheck();
            sheet.sizes.clearCheck();
            sheet.colors.clearCheck();
            sheet.sortDefault.setChecked(true);
        });
        sheet.apply.setOnClickListener(v -> {
            Long category = checkedTag(sheet.categories),
                size = checkedTag(sheet.sizes),
                color = checkedTag(sheet.colors);
            String sort = "DEFAULT";
            if (sheet.sortPriceAsc.isChecked()) sort = "PRICE_ASC";
            else if (sheet.sortPriceDesc.isChecked()) sort = "PRICE_DESC";
            vm.query(
                b.search.getText().toString(),
                category,
                size,
                color,
                sort
            );
            dialog.dismiss();
        });
        dialog.show();
    }

    private void addChip(
        ChipGroup group,
        String label,
        long value,
        boolean checked
    ) {
        Chip chip = new Chip(requireContext());
        chip.setId(View.generateViewId());
        chip.setText(label);
        chip.setTag(value);
        chip.setCheckable(true);
        chip.setChecked(checked);
        chip.setMinHeight(dp(48));
        group.addView(chip);
    }

    private Long checkedTag(ChipGroup group) {
        int id = group.getCheckedChipId();
        if (id == View.NO_ID) return null;
        Object tag = group.findViewById(id).getTag();
        return tag instanceof Long ? (Long) tag : null;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void showError() {
        if (b != null) b.error.setText(R.string.network_error);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (firstResume) {
            firstResume = false;
            return;
        }
        if (vm != null) vm.refresh();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
