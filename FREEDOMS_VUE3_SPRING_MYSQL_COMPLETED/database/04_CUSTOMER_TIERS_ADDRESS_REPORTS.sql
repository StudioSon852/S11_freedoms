-- FREEDOMS migration 05: customer default address + member tier thresholds.
-- Run against the existing application database. This script does not delete customer/order/payment data.

-- Add the customer's saved default address only when the column is missing.
SET @has_default_address := (
  SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'default_address'
);
SET @ddl := IF(@has_default_address = 0,
  'ALTER TABLE users ADD COLUMN default_address VARCHAR(500) NULL AFTER phone',
  'SELECT ''users.default_address already exists'' AS migration_note');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- For existing customers without a saved default, initialize it from the most recent non-empty online shipping address.
-- The customer can subsequently edit this value in Account Profile.
UPDATE users AS u
SET default_address = (
    SELECT o.shipping_address
    FROM orders AS o
    WHERE o.customer_id = u.user_id
      AND o.shipping_address IS NOT NULL
      AND TRIM(o.shipping_address) <> ''
    ORDER BY o.created_at DESC, o.order_id DESC
    LIMIT 1
)
WHERE u.user_id > 0
  AND (
      u.default_address IS NULL
      OR TRIM(u.default_address) = ''
  )
  AND EXISTS (
      SELECT 1
      FROM orders AS o
      WHERE o.customer_id = u.user_id
        AND o.shipping_address IS NOT NULL
        AND TRIM(o.shipping_address) <> ''
  );

-- Exact membership levels requested by the shop.
INSERT INTO member_tiers(tier_code, tier_name, min_spending, discount_percent, sort_order)
VALUES
  ('NEW', 'Mới', 0, 0, 1),
  ('SILVER', 'Bạc', 100000, 2, 2),
  ('GOLD', 'Vàng', 250000, 3, 3),
  ('PLATINUM', 'Bạch kim', 500000, 4, 4),
  ('DIAMOND', 'Kim cương', 1000000, 5, 5)
ON DUPLICATE KEY UPDATE
  tier_name = VALUES(tier_name),
  min_spending = VALUES(min_spending),
  discount_percent = VALUES(discount_percent),
  sort_order = VALUES(sort_order);

-- Recalculate existing CUSTOMER tiers from their already recorded cumulative spending.
UPDATE users u
JOIN roles r ON r.role_id = u.role_id AND r.role_code = 'CUSTOMER'
JOIN member_tiers t ON t.min_spending = (
  SELECT MAX(t2.min_spending)
  FROM member_tiers t2
  WHERE t2.min_spending <= COALESCE(u.total_spent, 0)
)
SET u.tier_id = t.tier_id;

SELECT tier_code, tier_name, min_spending, discount_percent, sort_order
FROM member_tiers
ORDER BY min_spending ASC;
