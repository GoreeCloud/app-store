export function matches(app, query = '', category = 'all', platform = 'all') {
  const needle = query.trim().toLocaleLowerCase();
  const corpus = [app.name, app.summary, app.category].join(' ').toLocaleLowerCase();
  return (!needle || corpus.includes(needle))
    && (category === 'all' || app.category === category)
    && (platform === 'all' || app.platforms.includes(platform));
}

export function selectApps(apps, query, category, platform) {
  return apps.filter(app => matches(app, query, category, platform))
    .sort((a,b) => a.name.localeCompare(b.name));
}
