-- =====================================================================
-- TaskMaster - Data verification queries
--
-- Read-only queries for checking the data after calling the API.
-- Run in pgAdmin's Query Tool (or psql) connected to taskmaster_db.
-- Select one query and press F5 to run just that query.
--
-- Nothing here changes data: every statement is a SELECT.
-- =====================================================================


-- ---------------------------------------------------------------------
-- 0. Overview: row count per table
-- ---------------------------------------------------------------------
SELECT 'users'            AS table_name, count(*) AS row_count FROM users
UNION ALL SELECT 'teams',            count(*) FROM teams
UNION ALL SELECT 'team_members',     count(*) FROM team_members
UNION ALL SELECT 'team_invitations', count(*) FROM team_invitations
UNION ALL SELECT 'tasks',            count(*) FROM tasks
UNION ALL SELECT 'comments',         count(*) FROM comments
UNION ALL SELECT 'attachments',      count(*) FROM attachments
UNION ALL SELECT 'revoked_tokens',   count(*) FROM revoked_tokens;


-- ---------------------------------------------------------------------
-- 1. Users  (after register / profile update / password change)
-- Passwords must start with $2a$ or $2b$ (BCrypt hash), never plain text.
-- ---------------------------------------------------------------------
SELECT id,
       full_name,
       email,
       left(password, 7) || '...' AS password_hash_prefix,
       created_at,
       updated_at
FROM users
ORDER BY id;


-- ---------------------------------------------------------------------
-- 2. Teams with owner and member count  (after create team / accept invitation)
-- ---------------------------------------------------------------------
SELECT t.id,
       t.name,
       t.description,
       u.full_name  AS created_by,
       count(tm.id) AS member_count,
       t.created_at
FROM teams t
JOIN users u              ON u.id = t.created_by
LEFT JOIN team_members tm ON tm.team_id = t.id
GROUP BY t.id, u.full_name
ORDER BY t.id;


-- ---------------------------------------------------------------------
-- 3. Team members and roles  (OWNER / MEMBER)
-- ---------------------------------------------------------------------
SELECT t.name      AS team,
       u.id        AS user_id,
       u.full_name AS member,
       u.email,
       tm.role,
       tm.joined_at
FROM team_members tm
JOIN teams t ON t.id = tm.team_id
JOIN users u ON u.id = tm.user_id
ORDER BY t.id, tm.role DESC, u.full_name;


-- ---------------------------------------------------------------------
-- 4. Invitations  (after invite / accept / decline)
-- ---------------------------------------------------------------------
SELECT i.id,
       t.name         AS team,
       invited.email  AS invited_user,
       inviter.email  AS invited_by,
       i.status,
       i.created_at
FROM team_invitations i
JOIN teams t       ON t.id = i.team_id
JOIN users invited ON invited.id = i.invited_user_id
JOIN users inviter ON inviter.id = i.invited_by
ORDER BY i.id;


-- ---------------------------------------------------------------------
-- 5. Tasks with team, creator and assignee
--    (after create / update / status change / assign / delete)
-- team = NULL means a personal task; assignee = NULL means unassigned.
-- ---------------------------------------------------------------------
SELECT tk.id,
       tk.title,
       tk.status,
       tk.priority,
       tk.due_date,
       tm.name        AS team,
       creator.email  AS created_by,
       assignee.email AS assignee,
       tk.created_at,
       tk.updated_at
FROM tasks tk
LEFT JOIN teams tm       ON tm.id = tk.team_id
JOIN users creator       ON creator.id = tk.created_by
LEFT JOIN users assignee ON assignee.id = tk.assignee_id
ORDER BY tk.id;


-- ---------------------------------------------------------------------
-- 6. Task count by status  (compare with GET /api/tasks?status=...)
-- ---------------------------------------------------------------------
SELECT status, count(*) AS task_count
FROM tasks
GROUP BY status
ORDER BY status;


-- ---------------------------------------------------------------------
-- 7. Tasks assigned to one user  (compare with GET /api/tasks?scope=ASSIGNED_TO_ME)
-- Change the email to the user you are checking.
-- ---------------------------------------------------------------------
SELECT tk.id, tk.title, tk.status, tk.priority, tk.due_date
FROM tasks tk
JOIN users u ON u.id = tk.assignee_id
WHERE u.email = 'amit@example.com'
ORDER BY tk.due_date NULLS LAST, tk.id;


-- ---------------------------------------------------------------------
-- 8. Search check  (compare with GET /api/tasks?search=...)
-- Change the search text; ILIKE is case-insensitive, like the API.
-- ---------------------------------------------------------------------
SELECT id, title, description
FROM tasks
WHERE title ILIKE '%home%' OR description ILIKE '%home%'
ORDER BY id;


-- ---------------------------------------------------------------------
-- 9. Comments with task and author  (after add / delete comment)
-- ---------------------------------------------------------------------
SELECT c.id,
       c.task_id,
       tk.title  AS task,
       u.email   AS author,
       c.content,
       c.created_at
FROM comments c
JOIN tasks tk ON tk.id = c.task_id
JOIN users u  ON u.id = c.author_id
ORDER BY c.task_id, c.created_at;


-- ---------------------------------------------------------------------
-- 10. Attachments with task and uploader  (after upload / delete)
-- storage_path is relative to the uploads folder, e.g. task-4/<uuid>.jpg
-- ---------------------------------------------------------------------
SELECT a.id,
       a.task_id,
       tk.title AS task,
       a.file_name,
       a.content_type,
       a.file_size,
       u.email  AS uploaded_by,
       a.storage_path,
       a.uploaded_at
FROM attachments a
JOIN tasks tk ON tk.id = a.task_id
JOIN users u  ON u.id = a.uploaded_by
ORDER BY a.task_id, a.uploaded_at;


-- ---------------------------------------------------------------------
-- 11. Revoked tokens  (after logout)
-- Expired rows are removed by the hourly cleanup job.
-- ---------------------------------------------------------------------
SELECT id,
       token_id,
       expires_at,
       expires_at < now() AS already_expired
FROM revoked_tokens
ORDER BY expires_at DESC;


-- ---------------------------------------------------------------------
-- 12. Flyway migration history
-- ---------------------------------------------------------------------
SELECT installed_rank, version, description, success, installed_on
FROM flyway_schema_history
ORDER BY installed_rank;
