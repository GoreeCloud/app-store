# GoreeCloud App Store — Feature Roadmap

**Status:** Active roadmap control  
**As of:** 2026-09-16  
**Authoritative project record:** Project Specification — App Store  
**Canonical repository:** GoreeCloud/goreecloud-app-store  
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
