# Security model (early native foundation)

Trust boundary: a bundled, curated catalog restricts sources to `GoreeCloud` repositories.
Network discovery uses GitHub's public release metadata over HTTPS. Linux downloads
enforce an allowlist for redirect hosts, a 512 MiB size cap, exact byte size, filename
restrictions, and GitHub's SHA-256 asset digest. Unverified assets are not saved.

**Important limitation:** A checksum obtained from the same service as the downloaded
file does not protect against compromise of that service or its release-edit permissions.
A signed, delegated, rollback-aware metadata system and package signing verification
are required before independent automatic update claims. Never auto-run downloaded assets.

Android will use platform installation consent and APK signing checks; Linux should
use native Flatpak trust anchors. Store configuration should hold no GitHub tokens
or host credentials. The repository carries no production signing keys.

Report suspected vulnerabilities privately to the GoreeCloud maintainers; do not
include secrets, tokens or personal data in public issues.
