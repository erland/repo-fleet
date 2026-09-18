-- Run once as a PostgreSQL administrator on the shared Coolify PostgreSQL resource.
-- Replace CHANGE_ME before executing. Never commit the production password.

CREATE ROLE repofleet
    LOGIN
    PASSWORD 'CHANGE_ME';

CREATE DATABASE repofleet
    OWNER repofleet;
