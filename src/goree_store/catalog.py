"""Strict local catalog schema; all entries are owner-reviewed, never fetched from untrusted URLs."""
from __future__ import annotations

import json
import re
from datetime import date
from functools import lru_cache
from pathlib import Path
from typing import Literal
from urllib.parse import urlparse

from pydantic import BaseModel, ConfigDict, Field, HttpUrl, field_validator, model_validator

CATALOG_PATH = Path(__file__).resolve().parents[2] / "catalog" / "apps.json"
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
        if u.scheme != "https" or u.netloc != "github.com" or u.query or u.fragment:
            raise ValueError("initial release URLs must use trusted github.com HTTPS assets")
        if not re.fullmatch(r"/GoreeCloud/[A-Za-z0-9_-][A-Za-z0-9_.-]*/releases/download/[A-Za-z0-9_-][A-Za-z0-9_.-]*/[A-Za-z0-9_-][A-Za-z0-9_.-]*", u.path):
            raise ValueError("release URL must be a GoreeCloud GitHub release asset")
        date.fromisoformat(self.published_at)
        if not u.path.endswith("." + self.kind):
            raise ValueError("release filename must match its artifact type")
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
        if parsed.scheme != "https" or parsed.netloc != "github.com" or parsed.query or parsed.fragment:
            raise ValueError("source URL must be HTTPS github.com")
        if not re.fullmatch(r"/GoreeCloud/[A-Za-z0-9_-][A-Za-z0-9_.-]*/?", parsed.path):
            raise ValueError("source URL must identify a GoreeCloud repository")
        return value

    @model_validator(mode="after")
    def validate_releases(self):
        if len(set(self.platforms)) != len(self.platforms):
            raise ValueError("platforms must be unique")
        if self.status == "source-only" and self.releases:
            raise ValueError("source-only apps cannot advertise releases")
        if self.status == "packages-available" and not self.releases:
            raise ValueError("package availability requires releases")
        if any(urlparse(str(r.download_url)).path.split("/")[2] != urlparse(str(self.source_url)).path.split("/")[2] for r in self.releases):
            raise ValueError("release repository must match its source repository")
        if len({(r.platform, r.kind, r.version) for r in self.releases}) != len(self.releases):
            raise ValueError("duplicate release version/platform/type")
        if any(r.platform not in self.platforms for r in self.releases):
            raise ValueError("release platform must be supported by app")
        return self


class Catalog(StrictModel):
    catalog_version: Literal[1]
    apps: list[StoreApp] = Field(max_length=1000)

    @model_validator(mode="after")
    def unique_names(self):
        slugs = [a.slug for a in self.apps]
        if len(slugs) != len(set(slugs)):
            raise ValueError("duplicate app slug")
        if any(a.status != "source-only" or a.releases for a in self.apps):
            raise ValueError("distribution disabled until signed metadata admission is implemented")
        return self


def load_catalog(path: Path = CATALOG_PATH) -> Catalog:
    # Explicit upper bound protects against accidentally mounting huge/untrusted catalogs.
    with path.open("rb") as stream:
        raw = stream.read(2_000_001)
    if len(raw) > 2_000_000:
        raise ValueError("catalog file exceeds 2 MB")
    def unique_keys(pairs):
        result = {}
        for key, value in pairs:
            if key in result:
                raise ValueError("duplicate JSON object key")
            result[key] = value
        return result
    return Catalog.model_validate(json.loads(raw.decode("utf-8"), object_pairs_hook=unique_keys))


@lru_cache(maxsize=1)
def current_catalog() -> Catalog:
    return load_catalog()
