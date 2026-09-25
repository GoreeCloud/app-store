# GoreeCloud App Store — Project Record

**Repository:** `GoreeCloud/app-store`  
**Former repository identity:** `GoreeCloud/goreecloud-app-store`  
**Lifecycle:** Active Development / non-Production / non-Stable  
**Migration baseline:** `e8ed0f0d9be4a6f5d2358e621d927a134045db9e`  
**Canonical authority:** This file is the repository-local project record once accepted on the default branch.

## Product foundation

GoreeCloud App Store is the first-party GoreeCloud surface for discovering and managing GoreeCloud applications and services available to an authorized identity. It is not intended to be a general third-party marketplace.

Its authority model separates GoreeCloud Identity, App Store catalog entitlement, and backend artifact/service authorization. Client-side filtering is not the sole authorization boundary.

## Android Development identity

The Development Android package is `com.goreecloud.appstore.dev`. The production package identity `com.goreecloud.appstore` is reserved for a future separately accepted production application.

Development signing remains non-production evidence and must not be treated as production signing authority.

## Repository rename

The Drive project specification uses `GoreeCloud/goreecloud-app-store`. The current authoritative repository is `GoreeCloud/app-store`.

The former name remains historical provenance only.

## Drive project-specification history

The Drive source contains 18 top-level sections covering product role, entitlement, catalog authority, Android architecture, UX, platform-system boundaries, package delivery, updates/library behavior, source checkpoints, restrictions, release gates, long-term direction, and Development-successor candidate history.

The source also describes Linux and Web clients as Development directions. At this migration baseline, authoritative `main` accepts Android as the user-facing Development client; Linux/Web work remains Draft/candidate-only.

## Draft release-evidence stack

Open Draft pull requests develop package-delivery and release-evidence controls, current-main reconciliation, and later platform/design-system migrations.

Exact-head CI on those branches is evidence for those candidates only. It does not establish baseline-main implementation, production package delivery, Release Candidate status, Production Acceptance, or Stable qualification.

## Glaze UI authority correction

The migration found conflicting claims:
- the Drive source names V1.5.1;
- baseline App Store documentation/metadata names 2.0.0;
- newer Draft work references V1.6.

Live verification of `GoreeCloud/glaze-ui` establishes V1.6 / `1.6.0` as current Official Stable and V1.7 as Development-only.

The App Store migration therefore reconciles its target to V1.6 / `1.6.0` while keeping application conformance unaccepted. This does not certify App Store rendering, accessibility, runtime, release, or production acceptance.

## Project-governance migration

This migration:
- adds root `PROJECT-SPECIFICATIONS.md`;
- adds root `PROJECT-RECORD.md`;
- consolidates the former `SPECIFICATIONS.md`;
- reconciles the repository rename;
- separates accepted Android state from Draft Linux/Web work;
- corrects stale Glaze authority;
- removes active Google Drive project-record/roadmap authority from repository documentation; and
- keeps Drive source retirement blocked until accepted default-branch readback and migration verification succeed.

**Drive source:** Project Specification — App Store  
**Drive source ID:** `1jQt4ZIpCXkryS57StZ8ZXQEuNRCfH4kB`

## Separate feature/changelog migration

The mandatory repository-native `IMPLEMENTED-FEATURES.md`, `PLANNED-FEATURES.md`, and `CHANGELOGS.md` set is not yet established here.

`FEATURE-ROADMAP.md` and former Drive feature/changelog material therefore remain migration inputs until that separate governance migration is accepted and verified.

## Ongoing maintenance

Record significant product-scope, client-platform, architecture, entitlement/security, signing, platform-system, repository, production/recovery, lifecycle, or retirement events here. Routine feature/fix chronology belongs in `CHANGELOGS.md` once that mandatory record exists.
