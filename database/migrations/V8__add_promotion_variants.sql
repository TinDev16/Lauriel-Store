CREATE TABLE IF NOT EXISTS promotion_variants (
  promotion_id BIGINT NOT NULL,
  variant_id BIGINT NOT NULL,
  PRIMARY KEY (promotion_id, variant_id),
  KEY idx_promotion_variants_variant (variant_id),
  CONSTRAINT fk_promotion_variants_promotion FOREIGN KEY (promotion_id)
    REFERENCES promotions (promotion_id) ON DELETE CASCADE,
  CONSTRAINT fk_promotion_variants_variant FOREIGN KEY (variant_id)
    REFERENCES product_variants (variant_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
