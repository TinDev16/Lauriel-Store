package com.lauriel.mobile.util;

import android.graphics.Paint;
import android.view.View;
import android.widget.TextView;
import com.lauriel.mobile.data.model.Models;
import java.math.BigDecimal;

public final class ProductPriceView {
    private ProductPriceView() {}

    public static void bind(Models.Product product, TextView effectiveView,
            TextView originalView, TextView promotionView) {
        BigDecimal original = product.price == null ? product.basePrice : product.price;
        BigDecimal effective = product.effectivePrice == null ? original : product.effectivePrice;
        boolean discounted = original != null && effective != null && effective.compareTo(original) < 0;

        effectiveView.setText(Formatters.money(effective));
        originalView.setVisibility(discounted ? View.VISIBLE : View.GONE);
        promotionView.setVisibility(discounted ? View.VISIBLE : View.GONE);
        if (!discounted) return;

        originalView.setText(Formatters.money(original));
        originalView.setPaintFlags(originalView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        if (product.promotion != null && product.promotion.discountPercent != null) {
            promotionView.setText("-" + product.promotion.discountPercent.stripTrailingZeros().toPlainString() + "%");
        } else {
            promotionView.setText("Giá ưu đãi");
        }
    }
}
