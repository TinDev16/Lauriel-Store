CREATE TABLE product_images (
    product_image_id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    image_url TEXT NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (product_image_id),
    KEY idx_product_images_product_order (product_id, display_order, product_image_id),
    CONSTRAINT fk_product_images_product FOREIGN KEY (product_id)
        REFERENCES products (product_id) ON DELETE CASCADE,
    CONSTRAINT chk_product_images_order CHECK (display_order >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO product_images (product_id, image_url, is_primary, display_order)
SELECT p.product_id, p.img_url, TRUE, 0
FROM products p
WHERE p.img_url IS NOT NULL
  AND TRIM(p.img_url) <> ''
  AND NOT EXISTS (
      SELECT 1 FROM product_images pi WHERE pi.product_id = p.product_id
  );
