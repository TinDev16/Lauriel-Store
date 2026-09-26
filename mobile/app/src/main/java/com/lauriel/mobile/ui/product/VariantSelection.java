package com.lauriel.mobile.ui.product;

import com.lauriel.mobile.data.model.Models;

final class VariantSelection {
    private final VariantAvailability availability;
    private Long sizeId;
    private Long colorId;

    VariantSelection(VariantAvailability availability) {
        this.availability = availability;
    }

    void selectSize(Long nextSizeId) {
        sizeId = nextSizeId;
        if (sizeId != null && colorId != null
                && availability.findInStock(sizeId, colorId) == null) {
            colorId = null;
        }
    }

    void selectColor(Long nextColorId) {
        colorId = nextColorId;
        if (sizeId != null && colorId != null
                && availability.findInStock(sizeId, colorId) == null) {
            sizeId = null;
        }
    }

    Long sizeId() {
        return sizeId;
    }

    Long colorId() {
        return colorId;
    }

    boolean isSizeEnabled(long candidateSizeId) {
        return availability.sizeAvailable(candidateSizeId, colorId);
    }

    boolean isColorEnabled(long candidateColorId) {
        return availability.colorAvailable(candidateColorId, sizeId);
    }

    Models.Variant selectedVariant() {
        return availability.findInStock(sizeId, colorId);
    }
}