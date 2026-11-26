import React, { useEffect, useMemo, useState } from 'react';
import { createRoot } from 'react-dom/client';
import '@goreecloud/ui/tokens.css';
import '@goreecloud/ui/components.css';
import './store.css';
import { selectApps } from './filter.mjs';
import { loadApps } from './catalog.mjs';

const platforms = ['all', 'web', 'linux', 'android'];

function Store() {
  const [apps, setApps] = useState([]);
  const [query, setQuery] = useState('');
  const [platform, setPlatform] = useState('all');
  const [category, setCategory] = useState('all');
  const [state, setState] = useState('loading');
  const [error, setError] = useState('');

  async function refresh(signal) {
    setState('loading');
    setError('');
    try {
      const data = await loadApps(fetch, signal);
      if (signal?.aborted) return;
      setApps(data);
      setState('ready');
    } catch (e) {
      if (signal?.aborted || e.name === 'AbortError') return;
      setError('Unable to load the catalog. Verify that the API is running and try again.');
      setState('error');
    }
  }

  useEffect(() => {
    const controller = new AbortController();
    refresh(controller.signal);
    return () => controller.abort();
  }, []);

  const categories = useMemo(() => [...new Set(apps.map(a => a.category))].sort(), [apps]);
  const selected = useMemo(() => selectApps(apps, query, category, platform), [apps, query, category, platform]);

  return <div className="gc-app store-shell">
    <a href="#catalog" className="skip-link">Skip to applications</a>
    <header className="store-header">
      <div className="gc-container store-header-row">
        <div className="store-mark" aria-hidden="true">G</div>
        <div className="brand-lockup"><strong>GoreeCloud</strong><span className="gc-support">App Store</span></div>
        <span className="gc-badge">Developer preview</span>
        <a className="gc-button gc-button--outline store-github" href="https://github.com/GoreeCloud/app-store" target="_blank" rel="noopener noreferrer">Project source <span aria-hidden="true">↗</span></a>
      </div>
    </header>
    <main>
      <section className="gc-container store-hero" aria-labelledby="hero-title">
        <p className="gc-label eyebrow">A connected ecosystem · Source-first</p>
        <h1 className="gc-display" id="hero-title">Discover what <em>GoreeCloud</em> can do.</h1>
        <p className="hero-description">Explore privacy-conscious applications and services built around your own cloud. Browse development projects, check platform support, and visit their public source.</p>
        <div className="gc-alert store-notice" data-kind="info" role="note"><div><strong>Distribution not yet enabled.</strong> Listed projects have source links, not verified installers or automatic updates. Release trust and signing are still being developed.</div></div>
      </section>
      <section id="catalog" className="gc-container catalog-section" aria-label="Application catalog">
        <div className="section-heading"><div><p className="gc-label eyebrow">The collection</p><h2 className="gc-title">Explore applications</h2></div><p className="gc-support" aria-live="polite">{state === 'ready' ? selected.length + ' of ' + apps.length + ' projects' : state === 'loading' ? 'Loading catalog' : 'Catalog unavailable'}</p></div>
        <div className="catalog-filters gc-card gc-card--tonal">
          <div className="gc-field search-field"><label htmlFor="app-search">Search the catalog</label><input className="gc-input" id="app-search" placeholder="Find apps, features or categories…" value={query} onChange={event => setQuery(event.target.value)} type="search" autoComplete="off" /></div>
          <div className="gc-field filter-field"><label htmlFor="category-filter">Category</label><select className="gc-select" id="category-filter" value={category} onChange={event => setCategory(event.target.value)}><option value="all">All categories</option>{categories.map(c => <option key={c} value={c}>{c}</option>)}</select></div>
          <div className="gc-field filter-field"><label htmlFor="platform-filter">Platform</label><select className="gc-select" id="platform-filter" value={platform} onChange={event => setPlatform(event.target.value)}>{platforms.map(p => <option key={p} value={p}>{p === 'all' ? 'All platforms' : p.charAt(0).toUpperCase() + p.slice(1)}</option>)}</select></div>
        </div>
        {state === 'loading' && <p role="status" className="gc-support store-status">Loading applications…</p>}
        {state === 'error' && <div className="gc-alert store-status" data-kind="negative" role="alert"><span>{error}</span><button className="gc-button gc-button--outline" onClick={() => refresh()}>Try again</button></div>}
        {state === 'ready' && selected.length === 0 && <p role="status" className="gc-card store-status">No projects match your filters. <button className="gc-button gc-button--quiet" onClick={() => {setQuery('');setCategory('all');setPlatform('all');}}>Clear filters</button></p>}
        <div className="gc-grid store-grid">
          {state === 'ready' && selected.map((app, index) => <article className="gc-card gc-card--interactive store-item" key={app.slug}>
            <div className="card-header"><div className="app-monogram" aria-hidden="true">{app.name.replace("GoreeCloud ","").charAt(0)}</div><span className="gc-badge">{app.category}</span></div>
            <h3 className="gc-heading">{app.name}</h3>
            <p className="gc-support card-summary">{app.summary}</p>
            <p className="platform-line"><span className="sr-only">Supported platforms: </span>{app.platforms.map(p => <span key={p} className="platform-chip">{p}</span>)}</p>
            <div className="card-footer"><span className="status-dot"><span aria-hidden="true">●</span> Source only</span><a className="gc-button gc-button--secondary" target="_blank" rel="noopener noreferrer" href={app.source_url}>View source <span aria-hidden="true">↗</span></a></div>
          </article>)}
        </div>
      </section>
    </main>
    <footer className="store-footer"><div className="gc-container footer-row"><p>GoreeCloud App Store · Open-source development preview</p><p>Designed with <a href="https://github.com/GoreeCloud/gc-ui">GoreeCloud UI</a> · Privacy by design</p></div></footer>
  </div>;
}

createRoot(document.getElementById('root')).render(<React.StrictMode><Store/></React.StrictMode>);
