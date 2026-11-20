# Security model and distribution gates

**Current security posture: preview, no production install/update capability.**

Threats include poisoned publisher metadata, compromised GitHub releases, mirror tampering, SHA substitution, rollback/freeze attacks, malicious APKs, sideloading abuse, untrusted Flatpak refs, downloader SSRF, compromised package registry dependencies, and spoofed publisher identity.

Implemented:
- Strict versioned Pydantic manifest schema with unique IDs, bounded inputs, restricted source and artifact URL hosts, HTTPS-only release paths and SHA-256 format checks.
- No dynamic artifact ingestion, downloads, execution, install privileges, remote manifest fetches, signing keys or credentials.
- Native offline catalog copies; Android requests no Internet or installer permissions.
- Public API input bounds; no write routes or wildcard CORS.
- Source-only status shown truthfully in clients.

**Not yet implemented (release-blocking):**
1. Review actual publishing rights, license compatibility and original build reproducibility for each app.
2. Define independent publisher identities via GoreeCloud Identity; require hardware-backed/CI-protected signing keys and rotation/revocation.
3. Adopt a maintained TUF client + publisher role/threshold metadata and authenticated, consistent snapshots with expiry/rollback protection. Do not invent a compatible JSON format.
4. Verify artifact digest, download byte bounds, redirect destinations, APK signing certificate lineage, SBOM/provenance, malware assessments, and approved Linux Flatpak remote GPG keys.
5. Explicit, informed per-install consent and audit trails; Android installer user confirmation where required, Linux privilege separation and polkit user action.
6. Staged download/quarantine, upgrade test matrix, rollback/restore and compromised-release emergency disable.
7. Device and accessibility acceptance, dependency scanning, release workflow with evidence and approved launch.

**Disclosure:** do not add actual secrets to the public repository or issue logs. Report privately using the repository security policy once configured. This source is not suitable for delivering trusted software updates yet.

## External specification references
- TUF: https://theupdateframework.github.io/specification/latest/
- F-Droid APIs and index: https://f-droid.org/docs/All_our_APIs/
- Flatpak repository management: https://docs.flatpak.org/en/latest/repositories.html
- Android package user action: https://developer.android.com/reference/android/content/pm/PackageInstaller.SessionParams
