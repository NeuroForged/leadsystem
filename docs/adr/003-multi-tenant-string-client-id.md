# ADR-003: Multi-tenant with string clientId on Lead

**Status:** Accepted  
**Date:** 2026-04-26

## Context

The system is multi-tenant — each `Client` (agency) has their own chatbot that submits leads. Leads need to be scoped to their originating client.

`Client.id` is a `Long` (DB-generated). Chatbots submit leads via a simple POST body and need to identify which client they belong to.

## Decision

`Lead.clientId` is stored as a `String`, not a foreign key to `Client`.

## Consequences

**Positive:**
- Chatbots can use any stable string identifier (slug, UUID, whatever is issued to them) without needing to know internal DB IDs
- No join required when filtering leads by client

**Negative / watch-outs:**
- No referential integrity — a lead can have a `clientId` that references no existing `Client`
- `Lead.clientId` (String) and `Client.id` (Long) are related conceptually but not enforced by the DB
- Duplicate check (`existsByEmail`) was global — fixed in KAN-19 to be `existsByEmailAndClientId`
- Per-client API keys (KAN-7) will require mapping key → clientId at auth time

**Rule:** When querying leads for a specific client, always filter by `clientId`. Never use `getAllLeads()` in a multi-client context.
