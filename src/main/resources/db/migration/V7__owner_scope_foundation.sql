INSERT INTO users (phone, password_hash, nickname, status, created_at, updated_at)
VALUES (
    'SYSTEM-LEGACY-OWNER',
    '$2a$10$zvM4H9oAJn5LQJfUel4Ch.79v42PFsKnz03Wx/ju0RduCMsZ/HXXi',
    'Legacy Owner',
    0,
    1717353600000,
    1717353600000
)
ON CONFLICT (phone) DO NOTHING;

ALTER TABLE products ADD COLUMN IF NOT EXISTS owner_user_id BIGINT;
ALTER TABLE customers ADD COLUMN IF NOT EXISTS owner_user_id BIGINT;
ALTER TABLE suppliers ADD COLUMN IF NOT EXISTS owner_user_id BIGINT;
ALTER TABLE sale_orders ADD COLUMN IF NOT EXISTS owner_user_id BIGINT;
ALTER TABLE sale_order_items ADD COLUMN IF NOT EXISTS owner_user_id BIGINT;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS owner_user_id BIGINT;
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS owner_user_id BIGINT;
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS supplier_id BIGINT;
ALTER TABLE purchase_order_items ADD COLUMN IF NOT EXISTS owner_user_id BIGINT;
ALTER TABLE pay_orders ADD COLUMN IF NOT EXISTS owner_user_id BIGINT;
ALTER TABLE finance_records ADD COLUMN IF NOT EXISTS owner_user_id BIGINT;
ALTER TABLE inventory_adjustments ADD COLUMN IF NOT EXISTS owner_user_id BIGINT;
ALTER TABLE agent_tasks ADD COLUMN IF NOT EXISTS owner_user_id BIGINT;
ALTER TABLE agent_notifications ADD COLUMN IF NOT EXISTS owner_user_id BIGINT;
ALTER TABLE sync_cursors ADD COLUMN IF NOT EXISTS owner_user_id BIGINT;

UPDATE products
SET owner_user_id = (SELECT id FROM users WHERE phone = 'SYSTEM-LEGACY-OWNER')
WHERE owner_user_id IS NULL;
UPDATE customers
SET owner_user_id = (SELECT id FROM users WHERE phone = 'SYSTEM-LEGACY-OWNER')
WHERE owner_user_id IS NULL;
UPDATE suppliers
SET owner_user_id = (SELECT id FROM users WHERE phone = 'SYSTEM-LEGACY-OWNER')
WHERE owner_user_id IS NULL;
UPDATE sale_orders
SET owner_user_id = (SELECT id FROM users WHERE phone = 'SYSTEM-LEGACY-OWNER')
WHERE owner_user_id IS NULL;
UPDATE sale_order_items
SET owner_user_id = (SELECT id FROM users WHERE phone = 'SYSTEM-LEGACY-OWNER')
WHERE owner_user_id IS NULL;
UPDATE payments
SET owner_user_id = (SELECT id FROM users WHERE phone = 'SYSTEM-LEGACY-OWNER')
WHERE owner_user_id IS NULL;
UPDATE purchase_orders
SET owner_user_id = (SELECT id FROM users WHERE phone = 'SYSTEM-LEGACY-OWNER')
WHERE owner_user_id IS NULL;
UPDATE purchase_order_items
SET owner_user_id = (SELECT id FROM users WHERE phone = 'SYSTEM-LEGACY-OWNER')
WHERE owner_user_id IS NULL;
UPDATE pay_orders
SET owner_user_id = (SELECT id FROM users WHERE phone = 'SYSTEM-LEGACY-OWNER')
WHERE owner_user_id IS NULL;
UPDATE finance_records
SET owner_user_id = (SELECT id FROM users WHERE phone = 'SYSTEM-LEGACY-OWNER')
WHERE owner_user_id IS NULL;
UPDATE inventory_adjustments
SET owner_user_id = (SELECT id FROM users WHERE phone = 'SYSTEM-LEGACY-OWNER')
WHERE owner_user_id IS NULL;
UPDATE agent_tasks
SET owner_user_id = (SELECT id FROM users WHERE phone = 'SYSTEM-LEGACY-OWNER')
WHERE owner_user_id IS NULL;
UPDATE agent_notifications
SET owner_user_id = (SELECT id FROM users WHERE phone = 'SYSTEM-LEGACY-OWNER')
WHERE owner_user_id IS NULL;
UPDATE sync_cursors
SET owner_user_id = (SELECT id FROM users WHERE phone = 'SYSTEM-LEGACY-OWNER')
WHERE owner_user_id IS NULL;

ALTER TABLE products ALTER COLUMN owner_user_id SET NOT NULL;
ALTER TABLE customers ALTER COLUMN owner_user_id SET NOT NULL;
ALTER TABLE suppliers ALTER COLUMN owner_user_id SET NOT NULL;
ALTER TABLE sale_orders ALTER COLUMN owner_user_id SET NOT NULL;
ALTER TABLE sale_order_items ALTER COLUMN owner_user_id SET NOT NULL;
ALTER TABLE payments ALTER COLUMN owner_user_id SET NOT NULL;
ALTER TABLE purchase_orders ALTER COLUMN owner_user_id SET NOT NULL;
ALTER TABLE purchase_order_items ALTER COLUMN owner_user_id SET NOT NULL;
ALTER TABLE pay_orders ALTER COLUMN owner_user_id SET NOT NULL;
ALTER TABLE finance_records ALTER COLUMN owner_user_id SET NOT NULL;
ALTER TABLE inventory_adjustments ALTER COLUMN owner_user_id SET NOT NULL;
ALTER TABLE agent_tasks ALTER COLUMN owner_user_id SET NOT NULL;
ALTER TABLE agent_notifications ALTER COLUMN owner_user_id SET NOT NULL;
ALTER TABLE sync_cursors ALTER COLUMN owner_user_id SET NOT NULL;

ALTER TABLE products DROP CONSTRAINT IF EXISTS products_code_key;
ALTER TABLE customers DROP CONSTRAINT IF EXISTS customers_phone_key;
ALTER TABLE suppliers DROP CONSTRAINT IF EXISTS suppliers_phone_key;

CREATE UNIQUE INDEX IF NOT EXISTS uq_products_owner_code ON products(owner_user_id, code);
CREATE UNIQUE INDEX IF NOT EXISTS uq_customers_owner_phone ON customers(owner_user_id, phone);
CREATE UNIQUE INDEX IF NOT EXISTS uq_suppliers_owner_phone ON suppliers(owner_user_id, phone);

CREATE INDEX IF NOT EXISTS idx_products_owner_user_id ON products(owner_user_id);
CREATE INDEX IF NOT EXISTS idx_customers_owner_user_id ON customers(owner_user_id);
CREATE INDEX IF NOT EXISTS idx_suppliers_owner_user_id ON suppliers(owner_user_id);
CREATE INDEX IF NOT EXISTS idx_sale_orders_owner_user_id ON sale_orders(owner_user_id);
CREATE INDEX IF NOT EXISTS idx_sale_order_items_owner_user_id ON sale_order_items(owner_user_id);
CREATE INDEX IF NOT EXISTS idx_payments_owner_user_id ON payments(owner_user_id);
CREATE INDEX IF NOT EXISTS idx_purchase_orders_owner_user_id ON purchase_orders(owner_user_id);
CREATE INDEX IF NOT EXISTS idx_purchase_order_items_owner_user_id ON purchase_order_items(owner_user_id);
CREATE INDEX IF NOT EXISTS idx_pay_orders_owner_user_id ON pay_orders(owner_user_id);
CREATE INDEX IF NOT EXISTS idx_finance_records_owner_user_id ON finance_records(owner_user_id);
CREATE INDEX IF NOT EXISTS idx_inventory_adjustments_owner_user_id ON inventory_adjustments(owner_user_id);
CREATE INDEX IF NOT EXISTS idx_agent_tasks_owner_user_id ON agent_tasks(owner_user_id);
CREATE INDEX IF NOT EXISTS idx_agent_notifications_owner_user_id ON agent_notifications(owner_user_id);

ALTER TABLE sync_cursors DROP CONSTRAINT IF EXISTS sync_cursors_pkey;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'sync_cursors_owner_client_pk'
    ) THEN
        ALTER TABLE sync_cursors
            ADD CONSTRAINT sync_cursors_owner_client_pk PRIMARY KEY (owner_user_id, client_id);
    END IF;
END $$;
