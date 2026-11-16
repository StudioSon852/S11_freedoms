-- FREEDOMS refund workflow upgrade: multiple partial refunds per payment.
-- Safe additive migration for an existing freedoms_shop database. Does not delete refund records.
USE freedoms_shop;

-- Create a non-unique replacement index before dropping the legacy UNIQUE(order_id),
-- so the foreign key on refunds.order_id always retains a supporting index.
SET @has_refund_order_time_index = (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'refunds'
    AND INDEX_NAME = 'idx_refunds_order_time'
);
SET @ddl_refund_order_time_index = IF(
  @has_refund_order_time_index = 0,
  'ALTER TABLE refunds ADD INDEX idx_refunds_order_time (order_id, requested_at)',
  'SELECT "idx_refunds_order_time already exists"'
);
PREPARE stmt_refund_order_time_index FROM @ddl_refund_order_time_index;
EXECUTE stmt_refund_order_time_index;
DEALLOCATE PREPARE stmt_refund_order_time_index;

SET @has_refund_payment_status_index = (
  SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'refunds'
    AND INDEX_NAME = 'idx_refunds_payment_status'
);
SET @ddl_refund_payment_status_index = IF(
  @has_refund_payment_status_index = 0,
  'ALTER TABLE refunds ADD INDEX idx_refunds_payment_status (payment_id, status)',
  'SELECT "idx_refunds_payment_status already exists"'
);
PREPARE stmt_refund_payment_status_index FROM @ddl_refund_payment_status_index;
EXECUTE stmt_refund_payment_status_index;
DEALLOCATE PREPARE stmt_refund_payment_status_index;

-- Older schemas made order_id UNIQUE, which limited each order to one refund request.
-- Drop only a single-column UNIQUE index on order_id; keep the foreign key and all refund rows.
SET @refund_order_unique_indexes = (
  SELECT GROUP_CONCAT(CONCAT('DROP INDEX `', INDEX_NAME, '`') SEPARATOR ', ')
  FROM (
    SELECT INDEX_NAME
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'refunds'
      AND NON_UNIQUE = 0
      AND INDEX_NAME <> 'PRIMARY'
    GROUP BY INDEX_NAME
    HAVING COUNT(*) = 1 AND SUM(COLUMN_NAME = 'order_id') = 1
  ) AS unique_order_indexes
);
SET @ddl_drop_refund_order_unique = IF(
  @refund_order_unique_indexes IS NULL,
  'SELECT "No legacy order_id unique index found"',
  CONCAT('ALTER TABLE refunds ', @refund_order_unique_indexes)
);
PREPARE stmt_drop_refund_order_unique FROM @ddl_drop_refund_order_unique;
EXECUTE stmt_drop_refund_order_unique;
DEALLOCATE PREPARE stmt_drop_refund_order_unique;

-- Verification: no UNIQUE index should consist only of order_id.
SELECT INDEX_NAME, NON_UNIQUE, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS indexed_columns
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'refunds'
GROUP BY INDEX_NAME, NON_UNIQUE
ORDER BY INDEX_NAME;
