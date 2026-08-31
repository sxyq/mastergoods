-- Keep business numbers unique within an owner, matching the application scope.
ALTER TABLE sale_orders DROP CONSTRAINT IF EXISTS sale_orders_order_no_key;
ALTER TABLE purchase_orders DROP CONSTRAINT IF EXISTS purchase_orders_order_no_key;
ALTER TABLE pay_orders DROP CONSTRAINT IF EXISTS pay_orders_order_no_key;
ALTER TABLE finance_records DROP CONSTRAINT IF EXISTS finance_records_record_no_key;

CREATE UNIQUE INDEX IF NOT EXISTS uq_sale_orders_owner_order_no
    ON sale_orders(owner_user_id, order_no);
CREATE UNIQUE INDEX IF NOT EXISTS uq_purchase_orders_owner_order_no
    ON purchase_orders(owner_user_id, order_no);
CREATE UNIQUE INDEX IF NOT EXISTS uq_pay_orders_owner_order_no
    ON pay_orders(owner_user_id, order_no);
CREATE UNIQUE INDEX IF NOT EXISTS uq_finance_records_owner_record_no
    ON finance_records(owner_user_id, record_no);
