CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    phone VARCHAR(32) NOT NULL UNIQUE,
    password_hash VARCHAR(128) NOT NULL,
    nickname VARCHAR(64) NOT NULL,
    status INT NOT NULL DEFAULT 1,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS sessions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(128) NOT NULL UNIQUE,
    refresh_token VARCHAR(128) NOT NULL UNIQUE,
    expires_at BIGINT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS products (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    category VARCHAR(64) NOT NULL,
    unit VARCHAR(32) NOT NULL,
    sale_price DOUBLE PRECISION NOT NULL DEFAULT 0,
    purchase_price DOUBLE PRECISION NOT NULL DEFAULT 0,
    stock DOUBLE PRECISION NOT NULL DEFAULT 0,
    safe_stock DOUBLE PRECISION NOT NULL DEFAULT 0,
    status INT NOT NULL DEFAULT 1,
    sync_status INT NOT NULL DEFAULT 0,
    sync_version BIGINT NOT NULL DEFAULT 0,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS customers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    phone VARCHAR(32) NOT NULL UNIQUE,
    level_value INT NOT NULL DEFAULT 0,
    address VARCHAR(255),
    notes VARCHAR(255),
    balance DOUBLE PRECISION NOT NULL DEFAULT 0,
    status INT NOT NULL DEFAULT 1,
    sync_status INT NOT NULL DEFAULT 0,
    sync_version BIGINT NOT NULL DEFAULT 0,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS sale_orders (
    id BIGINT PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL UNIQUE,
    customer_id BIGINT,
    customer_name VARCHAR(128),
    subtotal_amount DOUBLE PRECISION NOT NULL DEFAULT 0,
    discount_amount DOUBLE PRECISION NOT NULL DEFAULT 0,
    total_amount DOUBLE PRECISION NOT NULL DEFAULT 0,
    paid_amount DOUBLE PRECISION NOT NULL DEFAULT 0,
    notes VARCHAR(255),
    status INT NOT NULL DEFAULT 0,
    sync_status INT NOT NULL DEFAULT 0,
    sync_version BIGINT NOT NULL DEFAULT 0,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_sale_orders_created_at ON sale_orders(created_at);
CREATE INDEX IF NOT EXISTS idx_sale_orders_customer_id ON sale_orders(customer_id);

CREATE TABLE IF NOT EXISTS sale_order_items (
    id BIGINT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_code VARCHAR(64) NOT NULL,
    product_name VARCHAR(128) NOT NULL,
    customer_id BIGINT,
    customer_name VARCHAR(128),
    quantity DOUBLE PRECISION NOT NULL,
    unit_price DOUBLE PRECISION NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_sale_order_items_order_id ON sale_order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_sale_order_items_product_id ON sale_order_items(product_id);
CREATE INDEX IF NOT EXISTS idx_sale_order_items_created_at ON sale_order_items(created_at);

CREATE TABLE IF NOT EXISTS payments (
    id BIGINT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    method INT NOT NULL,
    reference_no VARCHAR(128),
    type INT NOT NULL DEFAULT 1,
    created_at BIGINT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_payments_order_id ON payments(order_id);
CREATE INDEX IF NOT EXISTS idx_payments_created_at ON payments(created_at);

CREATE TABLE IF NOT EXISTS purchase_orders (
    id BIGINT PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL UNIQUE,
    supplier_name VARCHAR(128) NOT NULL,
    total_amount DOUBLE PRECISION NOT NULL DEFAULT 0,
    notes VARCHAR(255),
    status INT NOT NULL DEFAULT 0,
    sync_status INT NOT NULL DEFAULT 0,
    sync_version BIGINT NOT NULL DEFAULT 0,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_purchase_orders_created_at ON purchase_orders(created_at);

CREATE TABLE IF NOT EXISTS purchase_order_items (
    id BIGINT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_code VARCHAR(64) NOT NULL,
    product_name VARCHAR(128) NOT NULL,
    quantity DOUBLE PRECISION NOT NULL,
    unit_cost DOUBLE PRECISION NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    created_at BIGINT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_purchase_order_items_order_id ON purchase_order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_purchase_order_items_product_id ON purchase_order_items(product_id);

CREATE TABLE IF NOT EXISTS inventory_adjustments (
    id BIGINT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    product_code VARCHAR(64) NOT NULL,
    product_name VARCHAR(128) NOT NULL,
    quantity DOUBLE PRECISION NOT NULL,
    flow_type INT NOT NULL,
    reason VARCHAR(255),
    operator_name VARCHAR(128),
    created_at BIGINT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_inventory_adjustments_product_id ON inventory_adjustments(product_id);
CREATE INDEX IF NOT EXISTS idx_inventory_adjustments_created_at ON inventory_adjustments(created_at);

CREATE TABLE IF NOT EXISTS sync_cursors (
    client_id VARCHAR(128) PRIMARY KEY,
    last_cursor VARCHAR(128),
    updated_at BIGINT NOT NULL
);

