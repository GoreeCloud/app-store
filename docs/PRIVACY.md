# GoreeCloud App Store Privacy

## Status

GoreeCloud App Store is in Development and is not production accepted. This record describes the verified privacy boundary of the current candidate; it does not claim accepted Privacy Shield runtime integration or production privacy certification.

## Current data handling

- Catalog search is limited to the already-entitled Development catalog presented to the active Development identity.
- **Save for later** is device-local state. Saved item identifiers are stored in Android SharedPreferences under a SHA-256-derived namespace for the exact opaque Development identity subject; the raw subject is not embedded in preference-key metadata.
- Saved state does not represent purchase, ownership, installation history, account-wide history, synchronization, backup, or Everkeep recovery.
- **Clear saved for later** removes only the active Development identity's local saved-item state.
- Development package-state observation is restricted to the exact candidate package and is performed only when the delivery policy has no earlier non-device blocker. The application does not request broad package inventory authority.
- Production analytics, advertising, behavioral profiling, recommendation telemetry, and background query transmission are not implemented in this candidate.

## Identity and authorization

The current account switcher uses Development fixtures. It is not production GoreeCloud Identity authentication, and client-visible entitlement state is not treated as server-side authorization for package or service delivery.

## Network and delivery boundary

Production catalog transport, package download/install, service launch, update delivery, Wardveil runtime verification, Privacy Shield runtime policy, Everkeep recovery, Mesh coordination, and production Identity are not accepted. The UI must not imply that those authorities exist.

## Sensitive-data rules

Do not log or persist credentials, reusable tokens, signing secrets, protected catalog metadata, or unrelated identity data. Future telemetry, history, recommendations, synchronization, or recovery features require explicit Privacy Shield and lifecycle acceptance before they may be represented as active.

## User control

The current candidate provides per-identity removal of device-local saved items. Broader export, deletion, portability, synchronization, retention, and recovery controls remain planned and must be defined before production acceptance.
