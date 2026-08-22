-- V30: owner-scoped request idempotency for V2 pay-order creation.
ALTER TABLE pay_orders ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(128);

CREATE UNIQUE INDEX IF NOT EXISTS uq_pay_orders_owner_idempotency
    ON pay_orders (owner_user_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;
