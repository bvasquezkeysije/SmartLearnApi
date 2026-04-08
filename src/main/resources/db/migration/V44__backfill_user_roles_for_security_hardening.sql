-- Hardening: remove role inference by username/email and rely on persisted roles.
-- Backfill required roles so legacy users keep expected access.

-- Ensure canonical admin account(s) have admin role.
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON LOWER(r.name) = 'admin'
WHERE (
    LOWER(COALESCE(u.username, '')) = 'admin'
    OR LOWER(COALESCE(u.email, '')) = 'admin@a21k.com'
)
AND NOT EXISTS (
    SELECT 1
    FROM user_roles ur
    WHERE ur.user_id = u.id
      AND ur.role_id = r.id
);

-- Ensure every user has at least "user" role.
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON LOWER(r.name) = 'user'
WHERE NOT EXISTS (
    SELECT 1
    FROM user_roles ur
    WHERE ur.user_id = u.id
)
AND NOT EXISTS (
    SELECT 1
    FROM user_roles ur2
    WHERE ur2.user_id = u.id
      AND ur2.role_id = r.id
);
