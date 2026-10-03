CREATE TABLE customer_activity (
  user_id BIGINT NOT NULL,
  last_seen_at DATETIME NOT NULL,
  PRIMARY KEY (user_id),
  KEY idx_customer_activity_last_seen (last_seen_at),
  CONSTRAINT fk_customer_activity_user FOREIGN KEY (user_id)
    REFERENCES users (user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE customer_cart_items (
  user_id BIGINT NOT NULL,
  variant_id BIGINT NOT NULL,
  quantity INT NOT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (user_id, variant_id),
  KEY idx_customer_cart_variant (variant_id),
  CONSTRAINT fk_customer_cart_user FOREIGN KEY (user_id)
    REFERENCES users (user_id) ON DELETE CASCADE,
  CONSTRAINT fk_customer_cart_variant FOREIGN KEY (variant_id)
    REFERENCES product_variants (variant_id) ON DELETE CASCADE,
  CONSTRAINT chk_customer_cart_quantity CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
