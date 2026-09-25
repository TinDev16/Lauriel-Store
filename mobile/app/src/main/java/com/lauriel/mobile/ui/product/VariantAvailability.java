package com.lauriel.mobile.ui.product;

import com.lauriel.mobile.data.model.Models;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class VariantAvailability {
    private final List<Models.Variant> variants;
    private final List<Models.Size> sizes;
    private final List<Models.Color> colors;

    VariantAvailability(List<Models.Variant> source) {
        variants = source == null ? List.of() : source.stream()
                .filter(this::isSelectableVariant)
                .collect(java.util.stream.Collectors.toList());

        Map<Long, Models.Size> uniqueSizes = new LinkedHashMap<>();
        Map<Long, Models.Color> uniqueColors = new LinkedHashMap<>();
        for (Models.Variant variant : variants) {
            uniqueSizes.putIfAbsent(variant.size.sizeId, variant.size);
            uniqueColors.putIfAbsent(variant.color.colorId, variant.color);
        }
        sizes = new ArrayList<>(uniqueSizes.values());
        colors = new ArrayList<>(uniqueColors.values());
    }

    List<Models.Size> sizes() {
        return sizes;
    }

    List<Models.Color> colors() {
        return colors;
    }

    boolean sizeAvailable(long sizeId, Long colorId) {
        return variants.stream().anyMatch(variant -> inStock(variant)
                && variant.size.sizeId == sizeId
                && (colorId == null || variant.color.colorId == colorId));
    }

    boolean colorAvailable(long colorId, Long sizeId) {
        return variants.stream().anyMatch(variant -> inStock(variant)
                && variant.color.colorId == colorId
                && (sizeId == null || variant.size.sizeId == sizeId));
    }

    Models.Variant findInStock(Long sizeId, Long colorId) {
        if (sizeId == null || colorId == null) {
            return null;
        }
        return variants.stream()
                .filter(this::inStock)
                .filter(variant -> variant.size.sizeId == sizeId
                        && variant.color.colorId == colorId)
                .findFirst()
                .orElse(null);
    }

    private boolean isSelectableVariant(Models.Variant variant) {
        return variant != null
                && "ACTIVE".equals(variant.status)
                && variant.size != null
                && variant.color != null;
    }

    private boolean inStock(Models.Variant variant) {
        return variant.quantity > 0;
    }
}