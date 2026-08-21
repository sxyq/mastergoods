CREATE INDEX IF NOT EXISTS idx_bill_fund_links_owner_updated_id
    ON bill_fund_links (owner_user_id, updated_at, id);

CREATE INDEX IF NOT EXISTS idx_bill_fund_links_owner_bill_created
    ON bill_fund_links (owner_user_id, bill_type, bill_id, created_at);

CREATE INDEX IF NOT EXISTS idx_bill_fund_links_owner_account_created
    ON bill_fund_links (owner_user_id, account_id, created_at);

CREATE INDEX IF NOT EXISTS idx_partner_contacts_owner_type_updated_id
    ON partner_contacts (owner_user_id, partner_type, updated_at, id);

CREATE INDEX IF NOT EXISTS idx_partner_contacts_owner_type_partner_primary_created
    ON partner_contacts (owner_user_id, partner_type, partner_id, is_primary, created_at);

CREATE INDEX IF NOT EXISTS idx_product_supplier_relations_owner_updated_id
    ON product_supplier_relations (owner_user_id, updated_at, id);

CREATE INDEX IF NOT EXISTS idx_product_supplier_relations_owner_product_default_priority_created
    ON product_supplier_relations (owner_user_id, product_id, is_default, purchase_priority, created_at);

CREATE INDEX IF NOT EXISTS idx_inventory_adjustments_owner_created
    ON inventory_adjustments (owner_user_id, created_at);
