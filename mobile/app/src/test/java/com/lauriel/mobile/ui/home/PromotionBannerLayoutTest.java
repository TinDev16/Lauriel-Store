package com.lauriel.mobile.ui.home;

import static org.junit.Assert.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.Test;

public class PromotionBannerLayoutTest {
    @Test public void homeUsesHorizontalRealPromotionCards() throws Exception {
        String home=new String(Files.readAllBytes(Paths.get("src/main/res/layout/fragment_home.xml")),StandardCharsets.UTF_8);
        String card=new String(Files.readAllBytes(Paths.get("src/main/res/layout/item_promotion_offer.xml")),StandardCharsets.UTF_8);
        String source=new String(Files.readAllBytes(Paths.get("src/main/java/com/lauriel/mobile/ui/home/HomeFragment.java")),StandardCharsets.UTF_8);
        assertTrue(home.contains("androidx.recyclerview.widget.RecyclerView android:id=\"@+id/promotions\""));
        assertTrue(card.contains("@+id/badge"));
        assertTrue(card.contains("@+id/benefit"));
        assertTrue(source.contains("activeOrderPromotions"));
        assertTrue(source.contains("activeVouchers"));
        assertTrue(source.contains("PagerSnapHelper"));
        assertTrue(source.contains("showPromotionDetail"));
        assertFalse(home.contains("@string/hero_title"));
        assertFalse(source.contains("fake"));
    }
}
