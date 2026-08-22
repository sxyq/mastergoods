-- Preserve the request identity so a reused key with a different payload is rejected.
ALTER TABLE pay_orders ADD COLUMN IF NOT EXISTS idempotency_payload_hash VARCHAR(64);
