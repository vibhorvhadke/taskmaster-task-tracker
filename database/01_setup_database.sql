-- =====================================================================
-- TaskMaster - Database setup
--
-- Creates the application's database user and database.
-- Run as a PostgreSQL administrator (e.g. the "postgres" user) in pgAdmin
-- or psql, connected to the default "postgres" database.
--
-- IMPORTANT:
--   1. Replace 'change-me' with your own password, and use the same value
--      for the DB_PASSWORD environment variable.
--   2. Run each statement SEPARATELY (select it, then press F5 in pgAdmin).
--      CREATE DATABASE cannot run together with other statements.
--
-- Tables are NOT created here. Flyway creates them automatically from
-- src/main/resources/db/migration/V1__create_tables.sql on the first start.
-- =====================================================================

-- 1. Application user
CREATE USER taskmaster_user WITH PASSWORD 'change-me';

-- 2. Database owned by that user
CREATE DATABASE taskmaster_db OWNER taskmaster_user;

-- 3. Full privileges on the database
GRANT ALL PRIVILEGES ON DATABASE taskmaster_db TO taskmaster_user;

-- ---------------------------------------------------------------------
-- Optional: start from a clean database (DELETES ALL DATA).
-- Stop the application first, then run each statement separately.
-- ---------------------------------------------------------------------
-- DROP DATABASE IF EXISTS taskmaster_db;
-- CREATE DATABASE taskmaster_db OWNER taskmaster_user;
