package com.lauriel.store.repository;

import com.lauriel.store.entity.enums.ActiveStatus;
import java.math.BigDecimal;

public interface ProductSummaryView {
    Long getProductId();
    String getProductName();
    BigDecimal getPrice();
    String getImgUrl();
    ActiveStatus getStatus();
    CategoryView getCategory();

    interface CategoryView {
        Long getCategoryId();
        String getCategoryName();
    }
}
