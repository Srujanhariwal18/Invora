-- ==============================================================================
-- V4__allow_user_role.sql
-- Allow 'USER' role for standard normal users in addition to ADMIN and legacy roles
-- ==============================================================================

ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_users_role;
ALTER TABLE users ADD CONSTRAINT chk_users_role CHECK (role IN ('ADMIN', 'USER', 'WAREHOUSE_MANAGER', 'STAFF', 'VIEWER'));
