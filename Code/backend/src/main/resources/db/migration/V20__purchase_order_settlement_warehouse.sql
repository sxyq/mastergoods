ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS settlement_method INTEGER;
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS warehouse_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_purchase_orders_warehouse ON purchase_orders(warehouse_id);
