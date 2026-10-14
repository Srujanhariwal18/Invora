# Invora Database Management

## Overview
This directory is designated for standalone database administration assets, database provisioning scripts, standalone schemas, and backup procedures for **Invora**.

## Database Stack
- **Engine:** PostgreSQL 15+ (Hosted on Supabase)
- **Pooling:** Supavisor / HikariCP
- **Connection Details:** See connection configuration variables in [Backend/.env.example](../Backend/.env.example).

## Migration Scripts Location
In accordance with project architecture and separation of concerns:
- Flyway database migration scripts (`V1__initial_setup.sql`, `V2__create_invora_schema.sql`, `V3__seed_development_data.sql`) are maintained inside:
  [`Backend/src/main/resources/db/migration/`](../Backend/src/main/resources/db/migration/)
- This ensures the Spring Boot backend can automatically discover and execute migrations upon startup without duplicate files or storage overhead.

Development seed data is maintained in
[`Backend/src/main/resources/db/seed/`](../Backend/src/main/resources/db/seed/).
