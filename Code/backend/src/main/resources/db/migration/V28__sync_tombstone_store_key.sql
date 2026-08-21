-- V28: make deletion propagation unique per owner, store and entity.
-- V27 added store_id but the V26 primary key still allowed only one tombstone
-- for an entity across all stores. Historical rows are safe to backfill only
-- when each affected owner has exactly one active store. Abort otherwise;
-- never guess a store scope during a migration.
ALTER TABLE sync_tombstones
    ADD COLUMN IF NOT EXISTS store_id BIGINT;

DO $$
BEGIN
    IF EXISTS (
        SELECT t.owner_user_id
        FROM sync_tombstones t
        LEFT JOIN stores s
          ON s.owner_user_id = t.owner_user_id
         AND (s.status IS NULL OR s.status = 1)
        WHERE t.store_id IS NULL
        GROUP BY t.owner_user_id
        HAVING COUNT(s.id) <> 1
    ) THEN
        RAISE EXCEPTION
            'sync_tombstones rows have an ambiguous or missing active store scope';
    END IF;
END $$;

UPDATE sync_tombstones t
SET store_id = stores.id
FROM (
    SELECT DISTINCT ON (owner_user_id) id, owner_user_id
    FROM stores
    WHERE status IS NULL OR status = 1
    ORDER BY owner_user_id, id
) stores
WHERE t.owner_user_id = stores.owner_user_id
  AND t.store_id IS NULL;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM sync_tombstones WHERE store_id IS NULL) THEN
        RAISE EXCEPTION 'sync_tombstones rows without a resolvable store_id';
    END IF;
END $$;

ALTER TABLE sync_tombstones
    ALTER COLUMN store_id SET NOT NULL;

ALTER TABLE sync_tombstones
    DROP CONSTRAINT IF EXISTS pk_sync_tombstones;

ALTER TABLE sync_tombstones
    ADD CONSTRAINT pk_sync_tombstones_store
    PRIMARY KEY (owner_user_id, store_id, entity_type, entity_id);

CREATE INDEX IF NOT EXISTS idx_sync_tombstones_owner_store_deleted
    ON sync_tombstones (owner_user_id, store_id, deleted_at);
