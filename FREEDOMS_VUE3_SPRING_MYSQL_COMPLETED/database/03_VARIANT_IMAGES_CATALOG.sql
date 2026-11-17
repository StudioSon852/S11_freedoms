-- FREEDOMS: variant gallery support for databases created before this feature.
-- Run after 02_FEATURES_VUE_POS_SHIPPING_REFUNDS.sql and 03_REFUND_PARTIAL_TWO_REQUESTS.sql.
-- This is safe to re-run; existing image rows are preserved.
CREATE TABLE IF NOT EXISTS product_variant_images (
  image_id BIGINT NOT NULL AUTO_INCREMENT,
  variant_id BIGINT NOT NULL,
  image_url VARCHAR(600) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (image_id),
  KEY idx_variant_images_variant (variant_id, sort_order, image_id),
  CONSTRAINT fk_variant_images_variant
    FOREIGN KEY (variant_id) REFERENCES product_variants (variant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
