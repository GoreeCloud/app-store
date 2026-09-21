# GoreeCloud App Store — Feature Roadmap

**Status:** Active roadmap control  
**As of:** 2026-09-21  
**Authoritative project record:** Project Specification — App Store  
**Canonical repository:** GoreeCloud/app-store  
**Drive control:** `GoreeCloud/Feature Roadmap/GoreeCloud App Store/FEATURE-ROADMAP.md`  
**Detailed planned capability record:** `GoreeCloud/Feature Roadmap/GoreeCloud App Store/goreecloud-app-store-update.md`

## Purpose

This file is the repository-side feature roadmap control for GoreeCloud App Store. It records current planned and recommended feature work without replacing the authoritative project record, implementation evidence, release gates, or GoreeCloud Tasks Management.

The detailed App Store update is maintained as a planned feature-and-capability record in the canonical GoreeCloud Feature Roadmap location. Its contents describe intended product direction and do not establish implementation, Production Acceptance, Release Candidate status, Stable status, deployment, or runtime acceptance.

## Roadmap

| ID | Feature / obligation | Priority | Current state |
| --- | --- | --- | --- |
| FR-001 | Reconcile and maintain every current planned or recommended GoreeCloud App Store feature from the authoritative project record and verified repository evidence in this roadmap. | High | Ongoing control |
| FR-002 | Move actionable feature obligations into GoreeCloud Tasks Management when required, preserving priority, dependency, and lifecycle disposition. | High | Ongoing control |
| FR-003 | Do not mark features implemented, complete, cancelled, or superseded without authoritative evidence and synchronized repository/Drive roadmap updates. | High | Ongoing control |
| FR-004 | Develop the GoreeCloud App Store as the first-party-only GoreeCloud software discovery, distribution, update, management, security, privacy-intelligence, device-aware, cross-platform, and lifecycle control center defined by `goreecloud-app-store-update.md`, while preserving the boundary that third-party software and repositories are outside the dedicated GoreeCloud App Store catalog. | High | In Progress |

## Current implementation increment

Development implementation began on September 16, 2026 with stacked Draft PR #19 (`feature/release-evidence-gate`) at exact head `af774127e637eeaeb2d9c4e923d1a5ec922f5e18`, based directly on Draft PR #15 exact head `c6bf04c74d0fb13828fec1ce53b388005ef66f9c`.

The first bounded increment adds a fail-closed pre-handoff release-evidence gate to the Android package-delivery policy. Handoff eligibility now requires explicit accepted build-provenance evidence, SBOM evidence, release approval, and revocation status. Missing or rejected evidence blocks handoff with a distinct blocker. The increment also adds regression tests, a repository source validator, CI evidence retention, and an authority-boundary document.

This does not create authoritative release-evidence producers, enable package download or installation, grant Android package-install authority, or establish Production Acceptance, Release Candidate, Stable, deployment, or runtime acceptance. The broader FR-004 scope remains active and incomplete.

Exact-head application validation passed on `af774127e637eeaeb2d9c4e923d1a5ec922f5e18`: Android Development run `35076844576` (artifact `10438567256`, digest `sha256:81cedaefca9dedbf6022177b0c28de2c42b86b86e4b6c7e183b41b0ec76b1001`), Android rendered acceptance run `35076844654` (artifact `10438826007`, digest `sha256:1bd55dfcb1850a56fa5c4d57c6351c3d026902eff85b2c7622a3bc7b413b4873`), Linux Development run `35076844673` (artifact `10438078480`, digest `sha256:210f2af64656a32859250bbb3d13c086cd23665ee5c3c2159bb83b172f7584cb`), Web Development run `35076844652` (artifact `10438358884`, digest `sha256:f65698e6e4243159a0a85303f8601b537b74dd8b8fed14a0661ab76c717944bd`), and Web rendered acceptance run `35076844723` (artifact `10438562247`, digest `sha256:f1350c4c44d8345d9ad6dc7a27fe66c70fed5e1e9bef2da71de005e7176184d2`). These are Development validation results for the stacked candidate only; PR #19 remains Draft and unmerged.

### Second validated Development increment

Draft PR #21 (`security/release-evidence-artifact-binding`) is stacked directly on PR #19 exact head `af774127e637eeaeb2d9c4e923d1a5ec922f5e18`. Exact head `8b24d12c643fc8f9bb74a99cb59e6a566a8fde47` adds a canonical SHA-256 content identity to package candidates and binds accepted release evidence to the exact candidate digest. Malformed candidate identities, missing evidence bindings, and mismatched or non-canonical evidence bindings fail closed through distinct blockers, preventing otherwise accepted provenance/SBOM/release/revocation evidence from being reused for a different binary identity.

Exact-head application validation passed on `8b24d12c643fc8f9bb74a99cb59e6a566a8fde47`: Android Development run `35085116206` (artifact `10441313691`, digest `sha256:749409bfbc4c4452ffc7b12e9231f2a87f172ba45cda3442fff3ed9de4143fff`), Android rendered acceptance run `35085116126` (artifact `10442077480`, digest `sha256:aa922bc819d3e684e0d91a6e6d57957ab8f97f374b5b445144bddb0332ecd511`), Linux Development run `35085116175` (artifact `10441876504`, digest `sha256:c56d52be0738fd38ee32b79ec6138903e650f57db9f22fa0555ba1bba2e44a3b`), Web Development run `35085116162` (artifact `10441658249`, digest `sha256:c5a012d7c50e516979277a3260747c3b375915c36c25a05196989e5b83033879`), and Web rendered acceptance run `35085116123` (artifact `10442465122`, digest `sha256:b49c88c04905008ec5c67cc8b9d1cc9f0fad1deff8154f9758c14cf4f992f8dd`). No separate Platform Contract run was observed for this candidate path, so none is claimed.

The policy compares exact identities only. It does not hash or download package bytes, prove that a caller-supplied digest matches a binary, create authoritative digest/provenance/SBOM/revocation evidence, enable package installation, or establish Production Acceptance, Release Candidate, Stable, deployment, or runtime acceptance. PR #21 remains Draft and unmerged; the broader FR-004 scope remains active and incomplete.

### Third validated Development increment

Draft PR #23 (`security/release-evidence-envelope`) is stacked directly on PR #21 exact head `8b24d12c643fc8f9bb74a99cb59e6a566a8fde47`. Exact head `203b6f3625f978fa5146c0f425e00f4a9e9adc8b` replaces bare release-evidence facts with four independent typed release-evidence envelopes so package-delivery handoff requires preserved producer/system attribution, authority-domain attribution, accepted producer-authority state supplied by a future governed integration, exact package and artifact scope, contract/version metadata, source-reference metadata, and explicit creation/expiry freshness metadata. The pure policy now also requires a caller-supplied evaluation timestamp and fails closed for missing, unaccepted, malformed, mismatched, future-dated, expired, or otherwise non-current required release evidence.

The Development source removes the production-domain `acceptedFor(...)` convenience constructor so the App Store cannot mint accepted release evidence locally. The App Store remains an evidence consumer only: it does not authenticate producers, establish that authority-domain labels are truthful, obtain trusted time, validate source references, generate/validate provenance or SBOMs, approve releases, query revocation infrastructure, calculate artifact digests, verify package bytes, download packages, request Android install-source authority, invoke `PackageInstaller`, or install/update/rollback/uninstall software.

Exact-head application validation passed on `203b6f3625f978fa5146c0f425e00f4a9e9adc8b`: Android Development run `35088543828` (artifact `10443391136`, digest `sha256:d22d3d713b1a78cc049459f2b77a99f6d927135c1b331eab693f2accc1794ac6`), Android rendered acceptance run `35088543846` (artifact `10443741452`, digest `sha256:6f4c36206b70fc684853b7fc23e0b5b3003a33b95b85e66b7958c548ed0bc5d2`), Linux Development run `35088543848` (artifact `10443391373`, digest `sha256:dd338fb66a94981854cb1a036e9758cbfc713908d1e5cce9c34a116bee5a04ee`), Web Development run `35088543851` (artifact `10442968749`, digest `sha256:40f2f784ffc99947b0ea1161f16f80a809b3ed70f5dbc88267c4882c9a355ad4`), and Web rendered acceptance run `35088543856` (artifact `10443366878`, digest `sha256:3bca8559c212e0a0e727a351e63a3035f98764e7d9c5fe379dd2f2bfaa0c22d8`). No separate Platform Contract workflow was observed for this candidate path, so none is claimed.

This bounded control implements a consumer-side subset of the current GoreeCloud Platform Evidence Plane requirements for provenance, producer authority, scope, contract/version, source context, and freshness. It does not establish authenticated Evidence Plane transport, Identity verification of producers, trusted-time integration, authoritative evidence production, full Platform-System conformance, package delivery authority, Production Acceptance, Release Candidate, Stable, deployment, or runtime acceptance. PR #23 remains Draft and unmerged; the broader FR-004 scope remains active and incomplete.

### Fourth validated Development increment and Contract 0.4 reconciliation

Draft PR #25 (`security/release-evidence-set-correlation`) remains stacked directly on PR #23 exact validated head `203b6f3625f978fa5146c0f425e00f4a9e9adc8b`. Current exact candidate head `e05ae21dc20127cdff47ed5d5af4e2c2ce32c415` retains the fourth bounded increment and adds an opaque non-empty `evidenceSetId` to all four required release-evidence records and requires build-provenance, SBOM, release-approval, and revocation-status evidence to carry the same evidence-set identity. Missing correlation and mismatched evidence-set identities fail closed through distinct blockers, preventing individually valid records from separate evidence decisions from being combined into one future package-delivery handoff decision.

The App Store still does not authenticate or mint evidence-set identities, prove atomic evidence issuance, authenticate evidence producers, obtain trusted time, calculate or verify package-byte digests, download packages, request Android install-source authority, invoke `PackageInstaller`, or install/update/rollback/uninstall software. This increment remains a bounded consumer-side policy contract, not package-delivery authority.

The same exact candidate performs the current governance/conformance migration: `goreecloud.platform.yaml` uses Platform Contract `0.4`, evaluates exactly nine Integral Platform Systems, adds GoreeCloud Policy and GoreeCloud Observability as applicable-blocked, keeps GoreeCloud Sync separately governed, and pins the reusable validator to accepted central Contract 0.4 revision `6cb150d512647a0401b4da9e4741d7591693dee0`. The implemented Glaze source mapping remains V1.4 / `1.4.0`; current mandatory Stable target V1.6 / `1.6.0` at accepted release source `a7180679ea851389e0f3004515f9a25f420e716d` is represented as migration-required rather than falsely accepted.

Fresh exact-head validation passed on current head `e05ae21dc20127cdff47ed5d5af4e2c2ce32c415`: Platform Contract run `35630231278`, Android Development run `35630230744`, Linux Development run `35630230499`, Web Development run `35630230651`, Android rendered acceptance run `35630230696`, and Web rendered acceptance run `35630230742`. The Contract lane validated the exact caller revision against the accepted central Contract 0.4 implementation; Android, Linux, and Web Development/rendered lanes requalified the candidate after the governance migration.

PR #25 remains Draft, unmerged, Development-only, and overall nonconformant. Current-Stable Glaze UI 1.6.0 implementation and application acceptance; production Identity/catalog authorization; Wardveil package verification; Privacy Shield; Everkeep; Manager; Mesh; Policy; Observability; authoritative evidence production/transport/trusted time; package byte verification and installation/update/rollback authority; representative accessibility/target review; protected production signing; supported publication/hosting; release approval; Release Candidate qualification where applicable; deployment; and Stable qualification remain outstanding. Authoritative `main` remains separate from this stacked candidate, so this checkpoint does not establish integration into `main` or production acceptance. FR-004 remains active and incomplete.

## Planned capability scope

The September 16, 2026 App Store update establishes planned direction for:

- a GoreeCloud-only catalog and GoreeCloud Verified authenticity model;
- premium Glaze UI discovery, product pages, Today/editorial surfaces, search, collections, and ecosystem relationships;
- application, service, system-component, developer-tool, firmware, optional-feature, beta, preview, and self-hosted GoreeCloud distribution;
- platform-aware and device-aware delivery across supported mobile, desktop, server, networking, TV, web, container, and embedded targets;
- privacy and security information, Privacy Shield and Wardveil integration, cryptographic authenticity, and fail-secure installation;
- advanced update management, change analysis, dependency-aware Safe Update Mode, rollback, staged rollout, and official release channels;
- Library, favorites, history, Everkeep-backed restoration, multi-device views, and Identity-authorized remote installation;
- GoreeCloud Manager administration, self-hosted mirrors, offline operation, download management, security alerts, lifecycle status, roadmaps, documentation, support, accessibility, and feedback;
- centralized GoreeCloud release infrastructure, automated quality validation, canonical software-catalog metadata, one-product/multiple-platform identity, GoreeCloud Mesh distribution assistance, and ecosystem-graph relationships;
- software supply-chain transparency, release transparency and revocation, content-addressed package identity, signed delta updates, transactional installation, compatibility preflight, data-migration contracts, post-install health verification, and explainable install/update decisions;
- maintenance-window policy, emergency security response, signed offline catalogs, air-gapped package transfer, trusted local caching, bandwidth/power/storage-aware delivery, package retention, release-evidence inspection, catalog-schema governance, dependency impact analysis, recovery/rescue integration, an internal GoreeCloud release console, and Store self-diagnostics.

Detailed requirements remain in the canonical planned capability record rather than being duplicated here.

## Maintenance and synchronization

This roadmap and the corresponding Drive `FEATURE-ROADMAP.md` must remain materially synchronized with one another and with the authoritative project or service record. Update both copies whenever feature scope, priority, dependency, implementation status, cancellation, supersession, recommendation, or verification state materially changes.

The detailed planned capability record must remain clearly separated from verified implementation state. No feature may be represented as complete or Stable solely because it appears in the roadmap or capability document. Completion and lifecycle claims require the applicable authoritative implementation, validation, review, release, and production evidence.

## Reconciliation rule

At each material feature change, reconcile this roadmap against the current authoritative project record, repository implementation state, applicable platform-system requirements, the detailed planned capability record, and GoreeCloud Tasks Management. Missing obligations, stale status, duplicated work, roadmap drift, or undocumented disposition changes are defects to correct.
