package com.lauriel.store.repository;

import com.lauriel.store.entity.ProductVariant;
import com.lauriel.store.entity.Promotion;

public interface VariantPromotionView {
    ProductVariant getVariant();
    Promotion getProductPromotion();
    Promotion getVariantPromotion();
}
