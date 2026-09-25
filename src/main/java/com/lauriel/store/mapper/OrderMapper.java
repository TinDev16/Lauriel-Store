package com.lauriel.store.mapper;

import com.lauriel.store.dto.response.*;
import com.lauriel.store.entity.Order;
import com.lauriel.store.entity.OrderDetail;
import java.util.List;

public final class OrderMapper {
    private OrderMapper() {}

    public static OrderSummaryResponse summary(Order order) {
        return new OrderSummaryResponse(order.getOrderId(), order.getCustomer().getCustomerId(), order.getStatus(),
                order.getCreatedAt(), order.getTotalAmount(), order.getDiscountAmount(), order.getShippingFee(), order.getFinalAmount());
    }

    public static OrderResponse order(Order order, List<OrderDetail> items) {
        return new OrderResponse(order.getOrderId(), order.getCustomer().getCustomerId(), order.getStatus(),
                order.getCreatedAt(), order.getUpdatedAt(), order.getShippingAddress(), order.getTotalAmount(),
                order.getDiscountAmount(), order.getShippingFee(), order.getFinalAmount(),
                items.stream().map(OrderMapper::item).toList(),
                order.getVoucher() == null ? null : order.getVoucher().getCode());
    }

    private static OrderDetailResponse item(OrderDetail item) {
        var variant = item.getProductVariant();
        return new OrderDetailResponse(variant.getVariantId(), variant.getSku(), variant.getProduct().getProductName(),
                CatalogMapper.size(variant.getSize()), CatalogMapper.color(variant.getColor()), item.getQuantity(),
                item.getUnitPrice(), item.getDiscountAmount(), item.getSubtotal(), variant.getProduct().getImgUrl());
    }
}
