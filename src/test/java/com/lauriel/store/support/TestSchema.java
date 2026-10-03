package com.lauriel.store.support;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

public final class TestSchema {
    private TestSchema() {
    }

    public static Map<String, String> migratedTableDefinitions() throws Exception {
        String dump = Files.readString(Path.of("database/lauriel.sql"));
        Map<String, String> definitions = new LinkedHashMap<>();
        var tables = Pattern.compile("CREATE TABLE `(\\w+)` \\((.*?)\\) ENGINE=", Pattern.DOTALL).matcher(dump);
        while (tables.find()) definitions.put(tables.group(1), tables.group(2));

        definitions.put("categories", """
                  `category_id` bigint NOT NULL AUTO_INCREMENT,
                  `category_name` varchar(100) NOT NULL,
                  `status` enum('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
                  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  PRIMARY KEY (`category_id`),
                  UNIQUE KEY `uq_category_name` (`category_name`)
                """);
        String products = definitions.get("products");
        products = products.replace("  `product_id` bigint NOT NULL AUTO_INCREMENT,",
                "  `product_id` bigint NOT NULL AUTO_INCREMENT,\n  `category_id` bigint NOT NULL,");
        products = products.replace("  PRIMARY KEY (`product_id`),",
                "  PRIMARY KEY (`product_id`),\n  KEY `idx_products_category` (`category_id`),\n"
                        + "  CONSTRAINT `fk_products_category` FOREIGN KEY (`category_id`) REFERENCES `categories` (`category_id`),");
        definitions.put("products", products);

        String orders = definitions.get("orders");
        if (!orders.contains("`shipping_fee`")) {
            orders = orders.replace("  `discount_amount` decimal(14,2) NOT NULL DEFAULT '0.00',",
                    "  `discount_amount` decimal(14,2) NOT NULL DEFAULT '0.00',\n"
                            + "  `shipping_fee` decimal(14,2) NOT NULL DEFAULT '20000.00',");
        }
        definitions.put("orders", orders);

        definitions.put("product_images", """
                  `product_image_id` bigint NOT NULL AUTO_INCREMENT,
                  `product_id` bigint NOT NULL,
                  `color_id` bigint NULL,
                  `image_url` text NOT NULL,
                  `is_primary` boolean NOT NULL DEFAULT false,
                  `display_order` int NOT NULL DEFAULT 0,
                  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  PRIMARY KEY (`product_image_id`),
                  KEY `idx_product_images_product_order` (`product_id`,`display_order`,`product_image_id`),
                  KEY `idx_product_images_color` (`color_id`),
                  CONSTRAINT `fk_product_images_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`product_id`) ON DELETE CASCADE,
                  CONSTRAINT `fk_product_images_color` FOREIGN KEY (`color_id`) REFERENCES `colors` (`color_id`) ON DELETE SET NULL,
                  CONSTRAINT `chk_product_images_order` CHECK ((`display_order` >= 0))
                """);        return definitions;
    }

    public static String createIsolatedDatabase() throws Exception {
        String url = "jdbc:h2:mem:auth_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        Map<String, String> definitions = new LinkedHashMap<>(migratedTableDefinitions());
        try (var connection = DriverManager.getConnection(url, "sa", "");
             var statement = connection.createStatement()) {
            Set<String> created = new HashSet<>();
            while (!definitions.isEmpty()) {
                boolean progressed = false;
                for (String name : new ArrayList<>(definitions.keySet())) {
                    String definition = definitions.get(name);
                    Set<String> references = new HashSet<>();
                    var foreignKeys = Pattern.compile("REFERENCES `(\\w+)`").matcher(definition);
                    while (foreignKeys.find()) references.add(foreignKeys.group(1));
                    if (created.containsAll(references)) {
                        statement.execute("CREATE TABLE `" + name + "` (" + definition + ")");
                        created.add(name);
                        definitions.remove(name);
                        progressed = true;
                    }
                }
                if (!progressed) throw new IllegalStateException("Cannot order test schema foreign keys");
            }
        }
        return url;
    }
}
