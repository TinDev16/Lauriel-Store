package com.lauriel.mobile.ui.product;

import static org.junit.Assert.assertTrue;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.Test;

public class ProductPricingLayoutTest {
    @Test public void productCardsExposeOriginalEffectiveAndPromotionViews() throws Exception {
        assertPricingViews("src/main/res/layout/item_product.xml");
        assertPricingViews("src/main/res/layout/item_related_product.xml");
    }

    @Test public void variantPickerShowsExactVariantOriginalAndDiscountedPrices() throws Exception {
        String layout = read("src/main/res/layout/bottom_sheet_variant_selection.xml");
        assertTrue(layout.contains("@+id/price"));
        assertTrue(layout.contains("@+id/original_price"));
        assertTrue(layout.contains("@+id/promotion"));
    }
    @Test public void savedVoucherHasDedicatedVisualState() throws Exception {
        String layout = read("src/main/res/layout/item_voucher.xml");
        assertTrue(layout.contains("@+id/card"));
        assertTrue(layout.contains("@+id/saved_badge"));
        assertTrue(layout.contains("bg_saved_badge"));
    }

    private static void assertPricingViews(String path) throws Exception {
        String layout = read(path);
        assertTrue(layout.contains("@+id/effective_price"));
        assertTrue(layout.contains("@+id/price"));
        assertTrue(layout.contains("@+id/promotion"));
    }

    private static String read(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
