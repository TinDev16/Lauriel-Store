package com.lauriel.store;

import static org.assertj.core.api.Assertions.assertThat;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CommerceMigrationTest {
    @Test
    void shippingMigrationIsAdditiveAndBackwardsCompatible() throws Exception {
        String sql=Files.readString(Path.of("database/migrations/V3__add_shipping_fee.sql"));
        assertThat(sql).contains("ADD COLUMN shipping_fee", "NOT NULL DEFAULT 20000.00",
                "chk_orders_shipping_fee");
        assertThat(sql.toUpperCase()).doesNotContain("DROP TABLE", "TRUNCATE");
    }
    @Test
    void productImageMigrationCreatesGalleryAndBackfillsLegacyPrimaryImage() throws Exception {
        String sql=Files.readString(Path.of("database/migrations/V4__add_product_images.sql"));
        assertThat(sql).contains("CREATE TABLE product_images", "ON DELETE CASCADE",
                "INSERT INTO product_images", "p.img_url", "NOT EXISTS");
        assertThat(sql.toUpperCase()).doesNotContain("DROP TABLE", "TRUNCATE", "DROP COLUMN");
    }
}
