ALTER TABLE users ADD COLUMN IF NOT EXISTS admin_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE stores ADD COLUMN IF NOT EXISTS admin_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE store_memberships ADD COLUMN IF NOT EXISTS admin_version BIGINT NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_users_admin_version ON users(admin_version, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_stores_admin_version ON stores(admin_version, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_store_memberships_admin_version ON store_memberships(admin_version, updated_at DESC);
