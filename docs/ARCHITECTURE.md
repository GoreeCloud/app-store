# GoreeCloud App Store Architecture

## Design principle

The store is a **policy-mediated catalog and delivery client**, not the authority for every platform decision it displays.

```text
GoreeCloud Identity
      │ authenticated actor + approved claims
      ▼
App Store entitlement service / policy boundary
      │ personalized authorized catalog
      ▼
Native App Store client
      ├── Discover / Apps / Services / Search
      ├── Updates / Library
      └── Product details + evidence presentation
             │
             ├── application artifact request ──► delivery service ──► Wardveil verification ──► Android installer
             └── service launch request ───────► service authorization ──► approved endpoint

Privacy Shield governs permitted data use.
Everkeep governs protection/recovery evidence.
GoreeCloud Mesh coordinates capabilities/events.
Glaze UI governs presentation/interaction semantics.
```

## Authority matrix

| Decision | Authority | App Store responsibility |
| --- | --- | --- |
| Who is acting? | GoreeCloud Identity | Consume validated session/claims |
| What catalog items may this user see? | App Store domain policy using approved Identity inputs | Evaluate and conceal non-entitled items |
| May this artifact be downloaded? | Delivery backend + applicable Identity/App Store policy | Request with current authorization; never rely on UI state |
| Is this package trusted/safe enough for install? | Wardveil Security | Present and enforce authoritative result |
| May catalog/search/library data be collected or used? | Privacy Shield | Minimize data and honor policy/consent |
| Is library/history state recoverable? | Everkeep | Preserve required state and present evidence truthfully |
| How are lifecycle/capability events coordinated? | GoreeCloud Mesh | Publish/consume minimized approved events |
| How is state represented and interacted with? | Glaze UI | Implement current consumer contract |

## Entitlement evaluation

The current pure `EntitlementEngine` is deliberately small. It accepts an `IdentitySession` plus `AccessRule` and returns only visible catalog items. It contains no implicit administrator override.

In production, normalized policy inputs should come from a server-authoritative catalog/entitlement boundary. The client may repeat the filter for defense in depth and offline-safe rendering, but the client is not the sole access-control enforcement point.

## Production catalog direction

A production catalog service should provide an authenticated snapshot containing:

- catalog revision and generation time;
- item metadata and lifecycle state;
- entitlement result or opaque policy binding appropriate for the caller;
- compatible releases;
- immutable artifact identifiers and digests;
- signing/provenance metadata;
- evidence references for platform systems;
- cache expiry and rollback/revocation information.

For privacy and security, a user should preferably receive only entitled entries rather than downloading a complete secret catalog and hiding items locally.

## Development artifact identity and signing

Development/device-review APKs are intentionally isolated from the future production package lineage.

```text
Development CI artifact
  package: com.goreecloud.appstore.dev
  label:   GoreeCloud App Store Dev
  signer:  repository-managed development-only certificate

Future production artifact
  package: com.goreecloud.appstore
  signer:  separate controlled production identity (not yet established)
```

The stable development certificate prevents each CI runner from generating a mutually incompatible Android debug signature. Its private key is intentionally repository-managed non-production test material and creates no production signing authority.

CI verifies the `.dev` package identity, application label, version metadata, APK digest, and development certificate fingerprint before artifact publication. Production signing must later use distinct protected key custody, provenance, recovery, and acceptance processes.

The early bootstrap lineage that used `com.goreecloud.appstore` with ephemeral runner debug certificates is not the production lineage and is not expected to upgrade in place. Those old installations may be removed from test devices.

## Package-delivery boundary

No installer permission is requested in the bootstrap. Before Android installation is enabled, the implementation needs:

1. a protected artifact endpoint with backend re-authorization;
2. immutable artifact identity and SHA-256 digest;
3. application signing-certificate expectations/provenance;
4. Wardveil verification policy and fail-closed result handling;
5. download integrity checking;
6. explicit Android user consent for installation;
7. install/update result reconciliation;
8. recovery/rollback behavior and audit evidence;
9. package-install permission only when required by the accepted implementation.

## Service-launch boundary

A service catalog entry may expose an approved HTTPS/deep-link destination, but the service must enforce its own authentication and authorization. Redirects and external-app handoff must use an allowlisted policy rather than arbitrary catalog-provided URLs.

## Offline behavior

A future offline cache may show a previously authorized catalog only within a defined freshness window and must clearly distinguish cached state from current authority. Protected downloads, new service grants, or stale elevated access must not be inferred solely from cached authorization.

## Current development boundary

Implemented now: native Android application, development catalog loader, development Identity adapter, entitlement filtering, application/service views, dedicated development platform-status surface, canonical artwork derivatives, stable `.dev` installation identity and signing certificate, tests, lint, exact-source CI, and development APK evidence publication.

Not connected now: production Identity, production catalog service, package download/install, service launch, production updates, installed library, Wardveil runtime acceptance, Privacy Shield runtime acceptance, Everkeep runtime acceptance, Mesh runtime, production signing, or Stable Glaze UI conformance. `productionAcceptance` therefore remains `false`.
