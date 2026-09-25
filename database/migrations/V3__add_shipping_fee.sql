-- Preserve existing orders while adding a server-owned shipping fee snapshot.
ALTER TABLE orders
    ADD COLUMN shipping_fee DECIMAL(14,2) NOT NULL DEFAULT 20000.00
    AFTER discount_amount;

ALTER TABLE orders
    ADD CONSTRAINT chk_orders_shipping_fee CHECK (shipping_fee >= 0);
