CREATE INDEX IF NOT EXISTS idx_sessions_user_id ON sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_sessions_expires_at ON sessions(expires_at);

CREATE INDEX IF NOT EXISTS idx_products_name ON products(name);
CREATE INDEX IF NOT EXISTS idx_products_code ON products(code);
CREATE INDEX IF NOT EXISTS idx_products_status ON products(status);

CREATE INDEX IF NOT EXISTS idx_customers_name ON customers(name);
CREATE INDEX IF NOT EXISTS idx_customers_phone ON customers(phone);
CREATE INDEX IF NOT EXISTS idx_customers_status ON customers(status);

CREATE INDEX IF NOT EXISTS idx_suppliers_name ON suppliers(name);
CREATE INDEX IF NOT EXISTS idx_suppliers_status ON suppliers(status);

CREATE INDEX IF NOT EXISTS idx_sale_orders_customer_id ON sale_orders(customer_id);
CREATE INDEX IF NOT EXISTS idx_sale_orders_status ON sale_orders(status);
CREATE INDEX IF NOT EXISTS idx_sale_orders_created_at ON sale_orders(created_at);

CREATE INDEX IF NOT EXISTS idx_purchase_orders_status ON purchase_orders(status);
CREATE INDEX IF NOT EXISTS idx_purchase_orders_created_at ON purchase_orders(created_at);

CREATE INDEX IF NOT EXISTS idx_pay_orders_supplier_id ON pay_orders(supplier_id);
CREATE INDEX IF NOT EXISTS idx_pay_orders_status ON pay_orders(status);
CREATE INDEX IF NOT EXISTS idx_pay_orders_created_at ON pay_orders(created_at);

CREATE INDEX IF NOT EXISTS idx_finance_records_type ON finance_records(type);
CREATE INDEX IF NOT EXISTS idx_finance_records_created_at ON finance_records(created_at);

CREATE INDEX IF NOT EXISTS idx_agent_notifications_task_id ON agent_notifications(task_id);
