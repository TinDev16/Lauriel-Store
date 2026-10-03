CREATE TABLE IF NOT EXISTS order_promotions (
  order_promotion_id BIGINT NOT NULL AUTO_INCREMENT,
  promotion_name VARCHAR(200) NOT NULL,
  description TEXT NULL,
  min_order_amount DECIMAL(14,2) NOT NULL,
  discount_percent DECIMAL(5,2) NOT NULL,
  max_discount_amount DECIMAL(14,2) NULL,
  start_date DATETIME NOT NULL,
  end_date DATETIME NOT NULL,
  status ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  PRIMARY KEY (order_promotion_id),
  KEY idx_order_promotions_eligibility (status,start_date,end_date,min_order_amount),
  CONSTRAINT chk_order_promotions_min CHECK (min_order_amount >= 0),
  CONSTRAINT chk_order_promotions_percent CHECK (discount_percent > 0 AND discount_percent <= 100),
  CONSTRAINT chk_order_promotions_cap CHECK (max_discount_amount IS NULL OR max_discount_amount > 0),
  CONSTRAINT chk_order_promotions_dates CHECK (start_date < end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE orders
  ADD COLUMN order_promotion_id BIGINT NULL AFTER voucher_id,
  ADD COLUMN order_promotion_name VARCHAR(200) NULL AFTER discount_amount,
  ADD COLUMN order_promotion_percent DECIMAL(5,2) NULL AFTER order_promotion_name,
  ADD COLUMN order_promotion_discount DECIMAL(14,2) NOT NULL DEFAULT 0.00 AFTER order_promotion_percent,
  ADD KEY idx_orders_order_promotion (order_promotion_id),
  ADD CONSTRAINT fk_orders_order_promotion FOREIGN KEY (order_promotion_id) REFERENCES order_promotions(order_promotion_id),
  ADD CONSTRAINT chk_orders_order_promotion_discount CHECK (order_promotion_discount >= 0);
