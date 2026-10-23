package com.lauriel.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lauriel.store.support.TestSchema;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import org.junit.jupiter.api.Test;

class CategoryMigrationTest {

    @Test
    void migrationUsesSafeBackfillOrderAndNeverDropsProductData()
        throws Exception {
        String sql = Files.readString(
            Path.of("database/migrations/V2__add_product_categories.sql")
        );
        int createCategory = sql.indexOf("CREATE TABLE categories");
        int nullableColumn = sql.indexOf("ADD COLUMN category_id BIGINT NULL");
        int fallback = sql.indexOf("'Chưa phân loại'");
        int backfill = sql.indexOf("UPDATE products");
        int notNull = sql.indexOf("MODIFY COLUMN category_id BIGINT NOT NULL");
        int foreignKey = sql.indexOf("FOREIGN KEY (category_id)");
        assertThat(createCategory).isGreaterThanOrEqualTo(0);
        assertThat(nullableColumn).isGreaterThan(createCategory);
        assertThat(fallback).isGreaterThan(nullableColumn);
        assertThat(backfill).isGreaterThan(fallback);
        assertThat(notNull).isGreaterThan(backfill);
        assertThat(foreignKey).isGreaterThan(notNull);
        assertThat(sql.toUpperCase()).doesNotContain(
            "DROP TABLE",
            "DROP DATABASE",
            "TRUNCATE"
        );
    }

    @Test
    void migratedSchemaEnforcesCategoryForeignKeyAndPreservesVariants()
        throws Exception {
        String url = TestSchema.createIsolatedDatabase();
        try (
            var connection = DriverManager.getConnection(url, "sa", "");
            var statement = connection.createStatement()
        ) {
            statement.executeUpdate(
                "INSERT INTO categories(category_id,category_name,status) VALUES(1,'Chưa phân loại','ACTIVE')"
            );
            statement.executeUpdate(
                "INSERT INTO products(product_id,category_id,product_name,price,status) VALUES(1,1,'Legacy',10,'ACTIVE')"
            );
            statement.executeUpdate(
                "INSERT INTO sizes(size_id,size_name) VALUES(1,'M')"
            );
            statement.executeUpdate(
                "INSERT INTO colors(color_id,color_name) VALUES(1,'Đen')"
            );
            statement.executeUpdate(
                "INSERT INTO product_variants(variant_id,product_id,size_id,color_id,sku,price,quantity,status) VALUES(1,1,1,1,'LEGACY-M',10,3,'ACTIVE')"
            );
            assertThat(
                statement.executeQuery("SELECT COUNT(*) FROM products").next()
            ).isTrue();
            try (
                var products = statement.executeQuery(
                    "SELECT COUNT(*) FROM products"
                )
            ) {
                products.next();
                assertThat(products.getLong(1)).isEqualTo(1);
            }
            try (
                var variants = statement.executeQuery(
                    "SELECT COUNT(*) FROM product_variants"
                )
            ) {
                variants.next();
                assertThat(variants.getLong(1)).isEqualTo(1);
            }
            assertThatThrownBy(() ->
                statement.executeUpdate(
                    "INSERT INTO products(product_id,category_id,product_name,price,status) VALUES(2,999,'Bad',10,'ACTIVE')"
                )
            ).isInstanceOf(java.sql.SQLException.class);
            assertThatThrownBy(() ->
                statement.executeUpdate(
                    "INSERT INTO products(product_id,category_id,product_name,price,status) VALUES(3,NULL,'Bad',10,'ACTIVE')"
                )
            ).isInstanceOf(java.sql.SQLException.class);
        }
    }
}
