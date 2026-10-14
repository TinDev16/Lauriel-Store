package com.lauriel.store.dto.store;

import java.math.BigDecimal;
import java.util.List;

public record StoreCartView(List<StoreCartItemView> items, int totalQuantity, BigDecimal subtotal) {
    public boolean empty() { return items.isEmpty(); }
}
