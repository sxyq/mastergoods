CREATE INDEX idx_inventory_ledger_owner_source_created
    ON inventory_ledger (owner_user_id, source_type, source_id, created_at);
