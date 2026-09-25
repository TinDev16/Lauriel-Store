-- Lauriel Store V2: add product categories without dropping or recreating product data.
-- Run once against the `lauriel` database, then keep ddl-auto=validate.

CREATE TABLE categories (
    category_id BIGINT NOT NULL AUTO_INCREMENT,
    category_name VARCHAR(100) NOT NULL,
    status ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (category_id),
    UNIQUE KEY uq_category_name (category_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE products ADD COLUMN category_id BIGINT NULL AFTER product_id;

INSERT INTO categories (category_name, status) VALUES
    ('Áo', 'ACTIVE'),
    ('Quần', 'ACTIVE'),
    ('Giày dép', 'ACTIVE'),
    ('Mũ', 'ACTIVE'),
    ('Phụ kiện', 'ACTIVE'),
    ('Áo khoác', 'ACTIVE'),
    ('Váy', 'ACTIVE'),
    ('Chưa phân loại', 'ACTIVE');

SET @fallback_category_id = (
    SELECT category_id FROM categories WHERE category_name = 'Chưa phân loại' LIMIT 1
);

UPDATE products
SET category_id = @fallback_category_id
WHERE category_id IS NULL;

ALTER TABLE products MODIFY COLUMN category_id BIGINT NOT NULL;
ALTER TABLE products ADD KEY idx_products_category (category_id);
ALTER TABLE products ADD CONSTRAINT fk_products_category
    FOREIGN KEY (category_id) REFERENCES categories (category_id);
