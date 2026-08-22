-- Execute only against the production PostgreSQL schema. Do not substitute H2.
EXPLAIN (ANALYZE, BUFFERS)
SELECT p.* FROM products p
WHERE p.owner_user_id = :owner_user_id
  AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER('%' || :keyword || '%')
       OR LOWER(p.code) LIKE LOWER('%' || :keyword || '%'))
ORDER BY p.updated_at DESC, p.id DESC
LIMIT :size OFFSET (:page * :size);

EXPLAIN (ANALYZE, BUFFERS)
SELECT c.* FROM customers c
WHERE c.owner_user_id = :owner_user_id
  AND (:keyword IS NULL OR LOWER(c.name) LIKE LOWER('%' || :keyword || '%')
       OR LOWER(c.phone) LIKE LOWER('%' || :keyword || '%'))
ORDER BY c.updated_at DESC, c.id DESC
LIMIT :size OFFSET (:page * :size);

EXPLAIN (ANALYZE, BUFFERS)
SELECT s.* FROM suppliers s
WHERE s.owner_user_id = :owner_user_id
  AND (:keyword IS NULL OR LOWER(s.name) LIKE LOWER('%' || :keyword || '%')
       OR LOWER(s.phone) LIKE LOWER('%' || :keyword || '%'))
ORDER BY s.updated_at DESC, s.id DESC
LIMIT :size OFFSET (:page * :size);

EXPLAIN (ANALYZE, BUFFERS)
SELECT f.* FROM finance_records f
WHERE f.owner_user_id = :owner_user_id
ORDER BY f.created_at DESC, f.id DESC
LIMIT :size OFFSET (:page * :size);

EXPLAIN (ANALYZE, BUFFERS)
SELECT o.* FROM purchase_orders o
WHERE o.owner_user_id = :owner_user_id
ORDER BY o.created_at DESC, o.id DESC
LIMIT :size OFFSET (:page * :size);
