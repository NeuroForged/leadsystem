# ADR-001: DDL auto-update instead of a migration tool

**Status:** Accepted  
**Date:** 2026-04-26

## Context

The system needs a way to keep the PostgreSQL schema in sync with JPA entity definitions as the codebase evolves.

Options considered:
- Flyway or Liquibase (explicit versioned migration scripts)
- Hibernate `ddl-auto: update` (schema inferred from entities at startup)

## Decision

Use `ddl-auto: update` for now.

## Consequences

**Positive:**
- Zero migration boilerplate during rapid early development
- New columns and tables appear automatically on deploy
- No risk of migration version conflicts

**Negative / watch-outs:**
- `ddl-auto: update` will NOT drop columns or rename them — only add
- Renaming a field in Java silently adds a new column and orphans the old one
- Adding a `NOT NULL` column to a table with existing rows will cause startup failure unless the field is nullable in Java or has a `columnDefinition` DB default
- Not suitable long-term at scale — switch to Flyway before first production data migration is needed

**Rule:** When adding a `NOT NULL` column, either make it nullable in the entity or add `@Column(columnDefinition = "... DEFAULT ...")`.
