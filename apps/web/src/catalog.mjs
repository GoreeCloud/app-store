const PAGE_SIZE = 100;
const MAX_APPS = 1000;
const allowedPlatforms = new Set(['web', 'linux', 'android']);

function validEntry(app) {
  return app && typeof app.slug === 'string' && /^[a-z][a-z0-9]*(?:-[a-z0-9]+)*$/.test(app.slug)
    && ['name', 'summary', 'category'].every(key => typeof app[key] === 'string' && app[key].length > 0)
    && Array.isArray(app.platforms) && app.platforms.length > 0
    && app.platforms.every(p => allowedPlatforms.has(p))
    && app.status === 'source-only' && Array.isArray(app.releases) && app.releases.length === 0
    && typeof app.source_url === 'string'
    && /^https:\/\/github\.com\/GoreeCloud\/[A-Za-z0-9_-][A-Za-z0-9_.-]*\/?$/.test(app.source_url);
}

export async function loadApps(fetchPage = fetch, signal) {
  const items = [];
  const seen = new Set();
  let total;
  do {
    const response = await fetchPage('/api/v1/apps?limit=' + PAGE_SIZE + '&offset=' + items.length,
      { signal, credentials: 'same-origin', redirect: 'error' });
    if (!response.ok) throw new Error('Catalog unavailable');
    const page = await response.json();
    if (!page || !Number.isSafeInteger(page.total) || page.total < 0 || page.total > MAX_APPS
        || (total !== undefined && page.total !== total)
        || page.offset !== items.length || page.limit !== PAGE_SIZE
        || !Array.isArray(page.items)
        || page.items.length !== Math.min(PAGE_SIZE, page.total - items.length)) {
      throw new Error('Invalid or changing catalog');
    }
    total = page.total;
    for (const entry of page.items) {
      if (!validEntry(entry) || seen.has(entry.slug)) throw new Error('Invalid catalog entry');
      seen.add(entry.slug);
      items.push(entry);
    }
  } while (items.length < total);
  return items;
}
