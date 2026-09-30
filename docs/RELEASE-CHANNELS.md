# GoreeCloud App Store Release Channels

## Status

Development implementation contract. This file does not claim production GoreeCloud Identity integration or production artifact delivery.

## Channels

The App Store recognizes five release channels:

- `STABLE` — production-approved releases.
- `RC` — release candidates awaiting final promotion.
- `BETA` — broader pre-release testing builds.
- `DEVELOPMENT` — active development builds.
- `DEBUG` — diagnostic/debug builds intended only for explicitly authorized development identities.

## Authorization model

Release-channel visibility is independent from broad application roles. A session must carry the explicit channel claim required by the requested channel:

- `channel:stable`
- `channel:rc`
- `channel:beta`
- `channel:development`
- `channel:debug`

Administrator status does not implicitly grant pre-release access. This prevents a general administrative role from silently becoming a debug-artifact entitlement.

The current claims in `DevelopmentIdentityGateway` are fixtures used to test this behavior. Production claims and policy must come from approved GoreeCloud Identity integration.

## Development fixture profiles

- Standard demo: Stable only.
- Preview tester demo: Stable, Beta, and RC.
- Administrator demo: Stable only unless explicit pre-release claims are added.
- Developer demo: Stable, RC, Beta, Development, and Debug.
- Signed out: no release-channel access.

## Delivery boundary

Channel visibility does not itself authorize artifact download, installation, update delivery, service launch, signature trust, or Stable promotion. Artifact delivery must re-authorize the request and apply Wardveil Security, Privacy Shield, Identity, lifecycle, signing/provenance, and other applicable GoreeCloud platform controls.
