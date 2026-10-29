"""Constrained GitHub release discovery and checksum-verified downloads.

Only curated GoreeCloud repositories may be queried. Downloaded packages are
not executed automatically. A GitHub asset digest is mandatory.
"""
from __future__ import annotations

import hashlib
import json
import re
import tempfile
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import dataclass
from pathlib import Path

API_ROOT = "https://api.github.com/repos/GoreeCloud/"
MAX_JSON = 2 * 1024 * 1024
MAX_ASSET = 512 * 1024 * 1024
ALLOWED_HOSTS = {"github.com", "release-assets.githubusercontent.com", "objects.githubusercontent.com", "api.github.com"}
REPO_PATTERN = re.compile(r"^[A-Za-z0-9_.-]+$")
DIGEST_PATTERN = re.compile(r"^sha256:([0-9a-fA-F]{64})$")
FORMATS = {"android": (".apk",), "linux": (".flatpak", ".flatpakref", ".appimage")}


@dataclass(frozen=True)
class ReleaseAsset:
    name: str
    url: str
    size: int
    digest: str


@dataclass(frozen=True)
class Release:
    version: str
    notes: str
    assets: tuple[ReleaseAsset, ...]


def safe_url(url: str) -> str:
    parts = urllib.parse.urlsplit(url)
    if parts.scheme != "https" or parts.hostname not in ALLOWED_HOSTS or parts.username or parts.password or parts.port not in (None, 443):
        raise ValueError("untrusted release URL")
    return url


class RestrictedRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        checked = safe_url(urllib.parse.urljoin(req.full_url, newurl))
        return super().redirect_request(req, fp, code, msg, headers, checked)


OPENER = urllib.request.build_opener(RestrictedRedirect)


def fetch_latest(repo: str, platform: str) -> Release | None:
    if not REPO_PATTERN.fullmatch(repo) or repo in (".", "..") or platform not in FORMATS:
        raise ValueError("invalid repo or platform")
    url = API_ROOT + urllib.parse.quote(repo) + "/releases/latest"
    request = urllib.request.Request(url, headers={"Accept": "application/vnd.github+json", "User-Agent": "GoreeCloud-App-Store/0.1"})
    try:
        with OPENER.open(request, timeout=12) as response:
            data = response.read(MAX_JSON + 1)
    except urllib.error.HTTPError as exc:
        if exc.code == 404:
            return None
        raise
    if len(data) > MAX_JSON:
        raise ValueError("oversized release metadata")
    payload = json.loads(data)
    if payload.get("draft") or payload.get("prerelease") or not isinstance(payload.get("tag_name"), str):
        return None
    assets = []
    for entry in payload.get("assets", []):
        if not isinstance(entry, dict):
            continue
        name, url, digest, size = (entry.get(k) for k in ("name", "browser_download_url", "digest", "size"))
        if not isinstance(name, str) or not name.lower().endswith(FORMATS[platform]):
            continue
        if not isinstance(url, str) or not isinstance(digest, str) or not isinstance(size, int):
            continue
        if size < 1 or size > MAX_ASSET or not DIGEST_PATTERN.fullmatch(digest):
            continue
        try:
            safe_url(url)
        except ValueError:
            continue
        if not url.startswith(f"https://github.com/GoreeCloud/{repo}/releases/download/"):
            continue
        assets.append(ReleaseAsset(name, url, size, digest.lower()))
    return Release(payload["tag_name"], str(payload.get("body") or "")[:4000], tuple(assets))


def download_verified(asset: ReleaseAsset, folder: Path) -> Path:
    safe_url(asset.url)
    match = DIGEST_PATTERN.fullmatch(asset.digest)
    if not match or asset.size < 1 or asset.size > MAX_ASSET:
        raise ValueError("asset lacks a valid SHA-256 digest or size")
    filename = Path(asset.name).name
    if filename != asset.name or filename in ("", ".", "..") or not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._-]{0,180}", filename):
        raise ValueError("unsafe asset filename")
    folder.mkdir(mode=0o700, parents=True, exist_ok=True)
    temp_path = None
    try:
        with tempfile.NamedTemporaryFile(dir=folder, prefix=".download-", delete=False) as output:
            temp_path = Path(output.name)
            request = urllib.request.Request(asset.url, headers={"User-Agent": "GoreeCloud-App-Store/0.1"})
            size = 0
            sha = hashlib.sha256()
            with OPENER.open(request, timeout=30) as response:
                safe_url(response.geturl())
                while chunk := response.read(1024 * 1024):
                    size += len(chunk)
                    if size > MAX_ASSET or size > asset.size:
                        raise ValueError("download exceeds expected size")
                    output.write(chunk)
                    sha.update(chunk)
        if size != asset.size or sha.hexdigest() != match.group(1).lower():
            raise ValueError("release asset checksum or size mismatch")
        final = folder / filename
        temp_path.replace(final)
        return final
    finally:
        if temp_path and temp_path.exists():
            temp_path.unlink()


def load_catalog(path: Path):
    from sys import path as sys_path
    sys_path.insert(0, str(path.resolve().parents[1]))
    # The independently tested validator prevents malformed catalog content.
    import scripts.validate_catalog as validator
    payload = json.loads(path.read_text(encoding="utf-8"))
    validator.validate(payload)
    return payload["products"]
