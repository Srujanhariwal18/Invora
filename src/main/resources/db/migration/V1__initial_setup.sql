-- V1__initial_setup.sql
-- Invora: Initial Flyway migration
-- Purpose: Verify Flyway is connected and the schema baseline is established.
-- No business tables are created in this phase.

-- Ensure the public schema exists (it does by default on Supabase/PostgreSQL)
-- and set the search path for this session.
SET search_path TO public;

-- Metadata: record the application name and migration baseline version.
COMMENT ON SCHEMA public IS 'Invora Warehouse Inventory Management System';
