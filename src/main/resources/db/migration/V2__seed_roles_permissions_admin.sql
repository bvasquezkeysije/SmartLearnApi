INSERT INTO roles(name, guard_name)
VALUES ('admin', 'web'), ('user', 'web')
ON CONFLICT DO NOTHING;

INSERT INTO permissions(name, guard_name)
VALUES
    ('users.manage', 'web'),
    ('projects.view', 'web'),
    ('projects.create', 'web'),
    ('projects.update', 'web'),
    ('projects.delete', 'web'),
    ('tasks.view', 'web'),
    ('tasks.create', 'web'),
    ('tasks.update', 'web'),
    ('tasks.delete', 'web'),
    ('tasks.complete', 'web'),
    ('portal.home.view', 'web'),
    ('portal.ai.view', 'web'),
    ('portal.forms.view', 'web'),
    ('portal.rooms.view', 'web'),
    ('portal.schedules.view', 'web'),
    ('portal.stats.view', 'web'),
    ('portal.help.view', 'web')
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON 1 = 1
WHERE r.name = 'admin'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.name IN (
    'portal.home.view',
    'portal.ai.view',
    'portal.forms.view',
    'portal.rooms.view',
    'portal.schedules.view',
    'portal.stats.view',
    'portal.help.view'
)
WHERE r.name = 'user'
ON CONFLICT DO NOTHING;

INSERT INTO people(first_name, last_name)
SELECT 'Admin', 'A21K'
WHERE NOT EXISTS (
    SELECT 1 FROM people
    WHERE first_name = 'Admin' AND last_name = 'A21K'
);

INSERT INTO users(person_id, name, username, email, password, email_verified_at, status)
SELECT p.id, 'Admin A21K', 'admin', 'admin@a21k.com', '123', CURRENT_TIMESTAMP, 1
FROM people p
WHERE p.first_name = 'Admin'
  AND p.last_name = 'A21K'
  AND NOT EXISTS (
      SELECT 1 FROM users u WHERE u.email = 'admin@a21k.com'
  );

INSERT INTO user_roles(user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = 'admin'
WHERE u.email = 'admin@a21k.com'
ON CONFLICT DO NOTHING;
