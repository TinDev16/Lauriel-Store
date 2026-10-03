package com.lauriel.mobile.ui.checkout;

import static org.junit.Assert.*;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import org.junit.Test;

public class CheckoutLayoutTest {
    @Test public void productListAndSinglePriceSummaryAreSeparate() throws Exception {
        String checkout=new String(Files.readAllBytes(Paths.get("src/main/res/layout/activity_checkout.xml")), StandardCharsets.UTF_8);
        String item=new String(Files.readAllBytes(Paths.get("src/main/res/layout/item_checkout_product.xml")), StandardCharsets.UTF_8);
        int list=checkout.indexOf("@+id/products");
        int subtotal=checkout.indexOf("@+id/subtotal");
        assertTrue(list>=0&&subtotal>list);
        assertEquals(1,count(checkout,"@+id/subtotal"));
        assertFalse(item.contains("@+id/subtotal"));
        assertFalse(item.contains("@+id/shipping"));
        assertTrue(item.contains("@+id/image"));
        assertTrue(checkout.contains("@+id/payment_methods"));
        assertTrue(checkout.contains("@+id/payment_cod"));
        assertTrue(checkout.contains("@string/payment_method_required"));
        assertTrue(checkout.contains("@+id/choose_saved_voucher"));
        assertTrue(checkout.contains("@string/choose_saved_voucher"));
        assertTrue(checkout.contains("@+id/voucher_entry_card"));
        assertTrue(checkout.contains("@+id/preview_card"));
        assertTrue(checkout.contains("@+id/remove_voucher"));
        assertTrue(checkout.contains("@+id/product_promotion_discount"));
        assertTrue(item.contains("android:clickable=\"true\""));
    }
    private int count(String source,String token){int result=0,from=0;while((from=source.indexOf(token,from))>=0){result++;from+=token.length();}return result;}
}
