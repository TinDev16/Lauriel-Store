package com.lauriel.store.repository;

import com.lauriel.store.entity.ProductVariant;
import com.lauriel.store.entity.Promotion;

// Entity selections are internal; the fetched variant graph is mapped to a response DTO in the service.
public interface VariantPromotionView {
    ProductVariant getVariant();
    Promotion getPromotion();
}
