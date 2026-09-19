package com.lauriel.store.mapper;

import com.lauriel.store.dto.response.*;
import com.lauriel.store.entity.Color;
import com.lauriel.store.entity.Product;
import com.lauriel.store.entity.ProductVariant;
import com.lauriel.store.entity.Size;

public final class CatalogMapper {
    private CatalogMapper() {
    }

    public static ProductResponse product(Product product) {
        return new ProductResponse(product.getProductId(), product.getProductName(),
                product.getDescription(), product.getPrice(), product.getImgUrl(), product.getStatus());
    }

    public static SizeResponse size(Size size) {
        return new SizeResponse(size.getSizeId(), size.getSizeName());
    }

    public static ColorResponse color(Color color) {
        return new ColorResponse(color.getColorId(), color.getColorName());
    }

    public static ProductVariantResponse variant(ProductVariant variant) {
        return new ProductVariantResponse(variant.getVariantId(), variant.getProduct().getProductId(),
                variant.getSku(), size(variant.getSize()), color(variant.getColor()),
                variant.getPrice(), variant.getQuantity(), variant.getStatus());
    }
}
