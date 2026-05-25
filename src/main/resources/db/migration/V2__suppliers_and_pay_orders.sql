CREATE TABLE IF NOT EXISTS suppliers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    phone VARCHAR(32) NOT NULL UNIQUE,
    address VARCHAR(255),
    notes VARCHAR(255),
    balance DOUBLE PRECISION NOT NULL DEFAULT 0,
    status INT NOT NULL DEFAULT 1,
    sync_status INT NOT NULL DEFAULT 0,
    sync_version BIGINT NOT NULL DEFAULT 0,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_suppliers_name ON suppliers(name);
CREATE INDEX IF NOT EXISTS idx_suppliers_phone ON suppliers(phone);
CREATE INDEX IF NOT EXISTS idx_suppliers_updated_at ON suppliers(updated_at);

CREATE TABLE IF NOT EXISTS pay_orders (
    id BIGINT PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL UNIQUE,
    supplier_id BIGINT,
    supplier_name VARCHAR(128) NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    method INT NOT NULL,
    reference_no VARCHAR(128),
    notes VARCHAR(255),
    status INT NOT NULL DEFAULT 0,
    sync_status INT NOT NULL DEFAULT 0,
    sync_version BIGINT NOT NULL DEFAULT 0,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_pay_orders_created_at ON pay_orders(created_at);
CREATE INDEX IF NOT EXISTS idx_pay_orders_supplier_id ON pay_orders(supplier_id);
CREATE INDEX IF NOT EXISTS idx_pay_orders_status ON pay_orders(status);
