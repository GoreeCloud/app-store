# Architecture and contracts (development)

## Current services and clients

```
                     canonical catalog/apps.json (reviewed source)
                              │
               ┌──────────────┼──────────────────┐
               │              │                  │
           FastAPI API    Qt Quick bundle   Android assets bundle
               │              │                  │
           React web       Linux GUI         Compose Android GUI
```

The API is **read-only** and has no writable catalog, account system, database, user profiling, telemetry, payment or package execution. It exposes `GET /healthz`, `GET /api/v1/meta`, `GET /api/v1/apps?q=&category=&platform=&offset=&limit=`, `GET /api/v1/apps/{slug}`, and `GET /api/v1/categories`. OpenAPI is available at `/openapi.json` and `/docs`.

Native apps bundle byte-identical offline snapshots of `catalog/apps.json`, checked by `scripts/sync_catalog.py --check`. Their content freshness is controlled by client release cycles. The browser uses an API proxy in development; in production require same-origin HTTPS through the existing GoreeCloud Gateway rather than enabling permissive CORS.

## Catalog model

The manifest includes version 1, unique slugs, names, short summaries, category, supported clients, repository URL, status, and releases. Known project repository URLs are manually curated and must not be conflated with runnable packages.

An optional release record has platform, artifact type, release URL, date, byte size and SHA-256. Validation restricts initial release download URLs to the GoreeCloud GitHub release namespace. This is only a **metadata input guard**, not an artifact integrity or authenticity verification. No current entries have releases. Never use these fields to automatically install or authorize an update before signed distribution controls are implemented.

## MIPS integration status

- **GoreeCloud UI:** web directly imports pinned `@goreecloud/ui` source; Android and Qt clients use a small source-pinned semantic token bridge. Published native SDK adoption, visual approval, accessibility and Stable compliance are not established.
- **GoreeCloud Mesh:** API versioning supports future discovery integration; no Mesh registration, synchronization or event flow exists.
- **GoreeCloud Identity:** public read-only catalog needs no user identity. Future publisher administration and private channels must use scoped Identity auth instead of ad hoc static API keys.
- **GoreeShield (privacy):** default no account, tracking, analytics or personal catalog storage. Private entitlements and retention/consent policies are future work.
- **Wardveil (cybersecurity):** catalog input validation and least privilege are implemented; malware scanning, signed provenance, publisher review, update trust and incident response are future gates.

## Operations

Run behind a trusted HTTPS proxy. Do not publish the Uvicorn dev server directly. Native applications use no auto-update privileges. Availability of repository source does not establish its package signing or operational status.
