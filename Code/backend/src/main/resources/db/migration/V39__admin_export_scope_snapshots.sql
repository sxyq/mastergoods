-- Persist the exact authorization scope and time range captured at export creation.
-- Workers must never re-resolve a request against a later, broader administrator scope.
ALTER TABLE admin_export_jobs ADD COLUMN IF NOT EXISTS scope_owner_user_ids_json TEXT;
ALTER TABLE admin_export_jobs ADD COLUMN IF NOT EXISTS scope_store_ids_json TEXT;
ALTER TABLE admin_export_jobs ADD COLUMN IF NOT EXISTS scope_all_owners BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE admin_export_jobs ADD COLUMN IF NOT EXISTS scope_all_stores BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE admin_export_jobs ADD COLUMN IF NOT EXISTS requested_from_at BIGINT;
ALTER TABLE admin_export_jobs ADD COLUMN IF NOT EXISTS requested_to_at BIGINT;

UPDATE admin_export_jobs
   SET scope_owner_user_ids_json = COALESCE(scope_owner_user_ids_json, '[]'),
       scope_store_ids_json = COALESCE(scope_store_ids_json, '[]')
 WHERE scope_owner_user_ids_json IS NULL OR scope_store_ids_json IS NULL;

CREATE INDEX IF NOT EXISTS idx_admin_export_status_created
    ON admin_export_jobs(status, created_at, id);
