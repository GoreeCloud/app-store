"""Strict local catalog schema; all entries are owner-reviewed, never fetched from untrusted URLs."""
from __future__ import annotations

import json
import re
from functools import lru_cache
from pathlib import Path
from typing import Literal
from urllib.parse import urlparse

from pydantic import BaseModel, ConfigDict, Field, HttpUrl, field_validator, model_validator

CATALOG_PATH = Path(__file__).resolve().parents[2] / "catalog" / "apps.json"
# Reviewed GitHub inventory; update only after owner/source verification.
ACTIVE_SOURCE_REPOS = frozenset(".github AI android-defaults android-toolkit app-store brand-assets containers database firefox-add-ons gc-ui GIF GoreeCloud goreeshield index mesh metrics static-websites wardveil".lower().split())
SLUG_RE = re.compile(r"^[a-z][a-z0-9]*(?:-[a-z0-9]+)*$")


class StrictModel(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)


class Release(StrictModel):
    version: str = Field(min_length=1, max_length=64)
    platform: Literal["linux", "android"]
    kind: Literal["flatpakref", "flatpak", "appimage", "apk"]
    download_url: HttpUrl
    sha256: str = Field(pattern=r"^[a-f0-9]{64}$")
    size_bytes: int = Field(gt=0)
    # A hash is necessary but not sufficient to establish publisher trust.
    published_at: str = Field(pattern=r"^20\d\d-\d\d-\d\d$")

    @model_validator(mode="after")
    def verify_distribution_host_and_platform(self):
        u = urlparse(str(self.download_url))
        if u.hostname != "github.com" or u.username or u.password or u.port:
            raise ValueError("initial release URLs must use trusted github.com HTTPS assets")
        if not u.path.startswith("/GoreeCloud/") or "/releases/download/" not in u.path:
            raise ValueError("release URL must be a GoreeCloud GitHub release asset")
        if (self.platform == "android") != (self.kind == "apk"):
            raise ValueError("Android artifacts must be APKs; Linux artifacts must not be APKs")
        return self


class StoreApp(StrictModel):
    slug: str = Field(min_length=2, max_length=64)
    name: str = Field(min_length=2, max_length=100)
    summary: str = Field(min_length=10, max_length=240)
    category: str = Field(min_length=2, max_length=50)
    platforms: list[Literal["web", "linux", "android"]] = Field(min_length=1)
    source_url: HttpUrl
    status: Literal["source-only", "packages-available"] = "source-only"
    releases: list[Release] = Field(default_factory=list)

    @field_validator("slug")
    @classmethod
    def validate_slug(cls, value):
        if not SLUG_RE.fullmatch(value):
            raise ValueError("invalid application slug")
        return value

    @field_validator("source_url")
    @classmethod
    def validate_source(cls, value):
        parsed = urlparse(str(value))
        if parsed.hostname != "github.com" or parsed.username or parsed.password or parsed.port:
            raise ValueError("source URL must be HTTPS github.com")
        if parsed.scheme != "https" or parsed.query or parsed.fragment or not re.fullmatch(r"/GoreeCloud/[A-Za-z0-9_.-]+/?", parsed.path):
            raise ValueError("source URL must identify a GoreeCloud repository")
        if parsed.path.rstrip("/").split("/")[-1].lower() not in ACTIVE_SOURCE_REPOS:
            raise ValueError("source repository is not in the reviewed active inventory")
        return value

    @model_validator(mode="after")
    def validate_releases(self):
        if len(set(self.platforms)) != len(self.platforms):
            raise ValueError("platforms must be unique")
        if self.status == "source-only" and self.releases:
            raise ValueError("source-only apps cannot advertise releases")
        if self.status == "packages-available" and not self.releases:
            raise ValueError("package availability requires releases")
        if any(r.platform not in self.platforms for r in self.releases):
            raise ValueError("release platform must be supported by app")
        return self


class Catalog(StrictModel):
    catalog_version: Literal[1]
    apps: list[StoreApp]

    @model_validator(mode="after")
    def unique_names(self):
        slugs = [a.slug for a in self.apps]
        if len(slugs) != len(set(slugs)):
            raise ValueError("duplicate app slug")
        return self


def load_catalog(path: Path = CATALOG_PATH) -> Catalog:
    # Explicit upper bound protects against accidentally mounting huge/untrusted catalogs.
    if path.stat().st_size > 2_000_000:
        raise ValueError("catalog file exceeds 2 MB")
    return Catalog.model_validate(json.loads(path.read_text(encoding="utf-8")))


@lru_cache(maxsize=1)
def current_catalog() -> Catalog:
    return load_catalog()
