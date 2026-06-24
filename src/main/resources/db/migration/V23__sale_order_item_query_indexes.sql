CREATE INDEX IF NOT EXISTS idx_sale_order_items_owner_created_at
    ON sale_order_items (owner_user_id, created_at);

CREATE INDEX IF NOT EXISTS idx_sale_order_items_owner_order_id
    ON sale_order_items (owner_user_id, order_id);

CREATE INDEX IF NOT EXISTS idx_sale_orders_owner_created_at
    ON sale_orders (owner_user_id, created_at);
