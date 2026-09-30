# GoreeCloud App Store Security

## Security status

This repository is Development-only. Source contains fail-closed entitlement and package-delivery boundaries, but production Wardveil Security integration, package distribution, production signing, deployment, and Stable acceptance are not established.

## Reporting vulnerabilities

Do not publish reusable credentials, private keys, signing material, exploit payloads containing sensitive data, or other secrets in public issues. Use GitHub private vulnerability reporting when available for this repository; otherwise contact the GoreeCloud maintainers through an approved private channel and keep sensitive reproduction material private.

## Current security boundaries

- Catalog entitlement filtering does not replace backend authorization.
- Package delivery remains unavailable unless authoritative catalog, artifact digest/signature, release evidence, revocation, package-state, and rollback evidence satisfy the fail-closed policy.
- Installed-state observation is exact-package only and does not use broad package enumeration.
- The Development signing certificate is non-production test material and must never be treated as production signing authority.
- No client-side administrator bypass is permitted.
- Canonical branding and package identity are validated in CI to reduce spoofing and accidental identity drift.

## Secrets and credentials

Production credentials, private keys, reusable tokens, protected signing material, and secret-bearing environment files must not be committed. Development-only test credentials or signing material must remain clearly non-production and must not be reused for production artifacts.

## Release boundary

A green build, test, emulator run, or Development artifact is not production security acceptance. Production package verification, protected signing/key custody, rollback, recovery, deployment, and Wardveil acceptance remain separate gates.
