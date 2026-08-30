-- Anonymous administrator-endpoint denials have no authenticated user ID.
-- Keep the event in the same audit stream without inventing an administrator.
ALTER TABLE admin_audit_events
    ALTER COLUMN admin_user_id DROP NOT NULL;

-- Only an explicitly anonymous security denial may omit the administrator FK.
ALTER TABLE admin_audit_events
    ADD CONSTRAINT ck_admin_audit_actor_identity
    CHECK (
        admin_user_id IS NOT NULL
        OR (role_code = 'ANONYMOUS' AND action = 'admin.access.denied' AND result = 'DENIED')
    );
