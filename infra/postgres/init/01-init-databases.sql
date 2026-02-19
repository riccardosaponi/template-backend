-- ─────────────────────────────────────────────────────────────────────────────
-- 01-init-databases.sql
-- Executed once by the postgres:17 entrypoint on first container start.
-- Creates two isolated databases:
--   • template_db  → Spring Boot application
--   • keycloak_db  → Keycloak identity provider
-- ─────────────────────────────────────────────────────────────────────────────

-- ── Application database ──────────────────────────────────────────────────────
CREATE USER template_user WITH PASSWORD 'template_pass';
CREATE DATABASE template_db
    OWNER     template_user
    ENCODING  'UTF8'
    LC_COLLATE 'en_US.utf8'
    LC_CTYPE   'en_US.utf8'
    TEMPLATE  template0;
GRANT ALL PRIVILEGES ON DATABASE template_db TO template_user;

-- ── Keycloak database ─────────────────────────────────────────────────────────
CREATE USER keycloak_user WITH PASSWORD 'keycloak_pass';
CREATE DATABASE keycloak_db
    OWNER     keycloak_user
    ENCODING  'UTF8'
    LC_COLLATE 'en_US.utf8'
    LC_CTYPE   'en_US.utf8'
    TEMPLATE  template0;
GRANT ALL PRIVILEGES ON DATABASE keycloak_db TO keycloak_user;

