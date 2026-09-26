-- Replace the optional image-to-size association with image-to-color.
-- Existing size assignments are reset because a size cannot be mapped to a color safely.
UPDATE product_images SET size_id = NULL WHERE size_id IS NOT NULL;

ALTER TABLE product_images
    DROP FOREIGN KEY fk_product_images_size,
    DROP INDEX idx_product_images_size,
    CHANGE COLUMN size_id color_id BIGINT NULL,
    ADD KEY idx_product_images_color (color_id),
    ADD CONSTRAINT fk_product_images_color
        FOREIGN KEY (color_id) REFERENCES colors (color_id)
        ON DELETE SET NULL;