"""No telemetry, mutation, remote URL fetches, install side effects or implicit CORS."""
from __future__ import annotations

from fastapi import FastAPI, HTTPException, Query
from fastapi.responses import JSONResponse
from pydantic import BaseModel

from .catalog import StoreApp, current_catalog

app = FastAPI(
    title="GoreeCloud App Store API",
    version="0.1.0",
    description="Read-only, source-verified catalog; no installer or update service.",
)


class AppPage(BaseModel):
    items: list[StoreApp]
    total: int
    offset: int
    limit: int


@app.get("/healthz")
def health():
    return {"status": "ok"}


@app.get("/readyz")
def ready():
    try:
        current_catalog()
    except (OSError, ValueError):
        return JSONResponse(status_code=503, content={"status": "unavailable"})
    return {"status": "ready"}


@app.get("/api/v1/meta")
def meta():
    catalog = current_catalog()
    return {
        "catalog_version": catalog.catalog_version,
        "total_apps": len(catalog.apps),
        "package_downloads_enabled": False,
    }


@app.get("/api/v1/categories")
def categories():
    catalog = current_catalog()
    result: dict[str, int] = {}
    for item in catalog.apps:
        result[item.category] = result.get(item.category, 0) + 1
    return [{"name": key, "count": result[key]} for key in sorted(result)]


@app.get("/api/v1/apps", response_model=AppPage)
def list_apps(
    q: str = Query(default="", max_length=100),
    category: str | None = Query(default=None, max_length=50),
    platform: str | None = Query(default=None, pattern=r"^(web|linux|android)$"),
    offset: int = Query(default=0, ge=0),
    limit: int = Query(default=24, ge=1, le=100),
):
    query = q.strip().casefold()
    items = [
        a for a in current_catalog().apps
        if (not query or query in f"{a.name} {a.summary} {a.category}".casefold())
        and (not category or a.category == category)
        and (not platform or platform in a.platforms)
    ]
    items.sort(key=lambda a: a.name.casefold())
    return AppPage(items=items[offset:offset + limit], total=len(items), offset=offset, limit=limit)


@app.get("/api/v1/apps/{slug}", response_model=StoreApp)
def get_app(slug: str):
    for item in current_catalog().apps:
        if item.slug == slug:
            return item
    raise HTTPException(status_code=404, detail="Application not found")
