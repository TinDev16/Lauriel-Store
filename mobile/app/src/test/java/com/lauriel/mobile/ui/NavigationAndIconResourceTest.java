package com.lauriel.mobile.ui;

import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

public class NavigationAndIconResourceTest {
    @Test public void everyBackButtonUsesTheSharedStyle() throws Exception {
        try (var layouts = Files.list(Paths.get("src/main/res/layout"))) {
            layouts.filter(path -> path.toString().endsWith(".xml")).forEach(path -> {
                try {
                    String xml = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                    if (xml.contains("@+id/back")) {
                        assertTrue(path + " phải dùng BackButton", xml.contains("style=\"@style/BackButton\""));
                    }
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            });
        }
    }

    @Test public void notificationAppliesSystemBarsAndManifestUsesAvatarIcon() throws Exception {
        String notification = new String(Files.readAllBytes(Paths.get(
                "src/main/java/com/lauriel/mobile/ui/notification/NotificationActivity.java")), StandardCharsets.UTF_8);
        String manifest = new String(Files.readAllBytes(Paths.get("src/main/AndroidManifest.xml")), StandardCharsets.UTF_8);
        Path icon = Paths.get("src/main/res/drawable-nodpi/lauriel_app_icon.png");
        assertTrue(notification.contains("SystemBars.apply(binding.getRoot())"));
        assertTrue(manifest.contains("android:icon=\"@drawable/lauriel_app_icon\""));
        assertTrue(manifest.contains("android:roundIcon=\"@drawable/lauriel_app_icon\""));
        assertTrue(Files.exists(icon));
        assertTrue(Files.size(icon) > 0);
    }
}
