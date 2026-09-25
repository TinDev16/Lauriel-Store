package com.lauriel.mobile.ui.product;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.lauriel.mobile.data.model.Models;
import java.util.List;
import org.junit.Test;

public class VariantAvailabilityTest {
    @Test
    public void showsAllOptionsAndFiltersByTheSelectedDimension() {
        Models.Variant smallBlack = variant(1, 10, "S", 20, "Black", 3);
        Models.Variant smallWhite = variant(2, 10, "S", 21, "White", 0);
        Models.Variant largeWhite = variant(3, 11, "L", 21, "White", 5);

        VariantAvailability availability = new VariantAvailability(
                List.of(smallBlack, smallWhite, largeWhite));

        assertEquals(2, availability.sizes().size());
        assertEquals(2, availability.colors().size());
        assertTrue(availability.colorAvailable(20, 10L));
        assertFalse(availability.colorAvailable(21, 10L));
        assertFalse(availability.sizeAvailable(10, 21L));
        assertTrue(availability.sizeAvailable(11, 21L));
        assertNotNull(availability.findInStock(10L, 20L));
        assertNull(availability.findInStock(10L, 21L));
    }

    @Test
    public void keepsOutOfStockOptionsVisibleButDisabled() {
        VariantAvailability availability = new VariantAvailability(
                List.of(variant(1, 10, "S", 20, "Black", 0)));

        assertEquals(1, availability.sizes().size());
        assertEquals(1, availability.colors().size());
        assertFalse(availability.sizeAvailable(10, null));
        assertFalse(availability.colorAvailable(20, null));
    }

    private Models.Variant variant(long variantId, long sizeId, String sizeName,
                                   long colorId, String colorName, int quantity) {
        Models.Size size = new Models.Size();
        size.sizeId = sizeId;
        size.sizeName = sizeName;
        Models.Color color = new Models.Color();
        color.colorId = colorId;
        color.colorName = colorName;
        Models.Variant variant = new Models.Variant();
        variant.variantId = variantId;
        variant.status = "ACTIVE";
        variant.size = size;
        variant.color = color;
        variant.quantity = quantity;
        return variant;
    }
}