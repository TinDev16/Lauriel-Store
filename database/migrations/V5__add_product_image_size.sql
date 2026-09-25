-- Optional image-to-size association. Images without size_id remain shared product images.
ALTER TABLE product_images
    ADD COLUMN size_id BIGINT NULL AFTER product_id,
    ADD KEY idx_product_images_size (size_id),
    ADD CONSTRAINT fk_product_images_size
        FOREIGN KEY (size_id) REFERENCES sizes (size_id)
        ON DELETE SET NULL;