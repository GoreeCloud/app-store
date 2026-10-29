"""Native Libadwaita app catalog and verified release downloader."""
from __future__ import annotations

import os
import subprocess
import threading
from pathlib import Path

import gi
gi.require_version("Gtk", "4.0")
gi.require_version("Adw", "1")
from gi.repository import Adw, Gio, GLib, Gtk

from .releases import download_verified, fetch_latest, load_catalog

ROOT = Path(__file__).resolve().parents[2]
CATALOG = ROOT / "shared" / "catalog.json"


class StoreWindow(Adw.ApplicationWindow):
    def __init__(self, application):
        super().__init__(application=application, title="GoreeCloud App Store")
        self.set_default_size(920, 720)
        self.rows = {}
        self.releases = {}

        self.overlay = Adw.ToastOverlay()
        view = Adw.ToolbarView()
        header = Adw.HeaderBar()
        header.set_title_widget(Adw.WindowTitle.new("GoreeCloud App Store", "Native Linux preview"))
        view.add_top_bar(header)
        refresh = Gtk.Button.new_from_icon_name("view-refresh-symbolic")
        refresh.set_tooltip_text("Check published releases")
        refresh.connect("clicked", lambda *_: self.refresh_all())
        header.pack_end(refresh)

        scroll = Gtk.ScrolledWindow()
        scroll.set_policy(Gtk.PolicyType.NEVER, Gtk.PolicyType.AUTOMATIC)
        content = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=24)
        content.set_margin_top(30)
        content.set_margin_bottom(36)
        content.set_margin_start(26)
        content.set_margin_end(26)
        scroll.set_child(content)
        view.set_content(scroll)
        self.overlay.set_child(view)
        self.set_content(self.overlay)

        hero = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=8)
        name = Gtk.Label(label="Your GoreeCloud ecosystem")
        name.add_css_class("title-1")
        name.set_xalign(0)
        hero.append(name)
        intro = Gtk.Label(label="Discover first-party apps and services. Downloads appear only for verified, published assets.")
        intro.set_wrap(True)
        intro.set_xalign(0)
        intro.add_css_class("dim-label")
        hero.append(intro)
        content.append(hero)

        products = [p for p in load_catalog(CATALOG) if "linux" in p["platforms"]]
        for kind, title in (("application", "Applications"), ("service", "Services")):
            group = Adw.PreferencesGroup(title=title)
            group.set_description("Published releases are checked from the verified GoreeCloud GitHub owner.")
            for app in products:
                if app["kind"] != kind:
                    continue
                row = Adw.ActionRow(title=app["name"], subtitle=app["summary"])
                row.set_activatable(False)
                check = Gtk.Button(label="Check release")
                check.set_valign(Gtk.Align.CENTER)
                check.connect("clicked", lambda _button, item=app: self.action(item))
                row.add_suffix(check)
                group.add(row)
                self.rows[app["id"]] = (row, check)
            content.append(group)

        footer = Gtk.Label(label="Early development foundation · No silent installs · Only GoreeCloud repositories")
        footer.add_css_class("dim-label")
        footer.set_xalign(0)
        content.append(footer)

    def toast(self, message):
        self.overlay.add_toast(Adw.Toast.new(str(message)))

    def action(self, app):
        if app["id"] in self.releases:
            self.download(app)
        else:
            self.check_app(app)

    def check_app(self, app):
        self.releases.pop(app["id"], None)
        row, button = self.rows[app["id"]]
        button.set_sensitive(False)
        button.set_label("Checking…")

        def work():
            try:
                release = fetch_latest(app["repo"], "linux")
                GLib.idle_add(self.release_loaded, app, release, None)
            except Exception as exc:
                GLib.idle_add(self.release_loaded, app, None, str(exc))

        threading.Thread(target=work, daemon=True).start()

    def release_loaded(self, app, release, error):
        row, button = self.rows[app["id"]]
        button.set_sensitive(True)
        if error:
            row.set_subtitle(f"Release check failed: {error[:130]}")
            button.set_label("Retry")
        elif release is None:
            row.set_subtitle("No published stable GitHub release yet")
            button.set_label("Check again")
        elif not release.assets:
            row.set_subtitle(f"{release.version} · No checksum-verified Linux package asset")
            button.set_label("Check again")
        else:
            asset = release.assets[0]
            self.releases[app["id"]] = asset
            row.set_subtitle(f"{release.version} · {asset.name} · SHA-256 verified before saving")
            button.set_label("Download")
        return False

    def download(self, app):
        asset = self.releases.get(app["id"])
        if not asset:
            return
        row, button = self.rows[app["id"]]
        button.set_sensitive(False)
        button.set_label("Downloading…")
        folder = Path.home() / "Downloads" / "GoreeCloud"

        def work():
            try:
                path = download_verified(asset, folder)
                GLib.idle_add(self.download_finished, app, path, None)
            except Exception as exc:
                GLib.idle_add(self.download_finished, app, None, str(exc))

        threading.Thread(target=work, daemon=True).start()

    def download_finished(self, app, path, error):
        row, button = self.rows[app["id"]]
        button.set_sensitive(True)
        button.set_label("Download again")
        if error:
            self.toast(f"Download refused: {error[:180]}")
        else:
            row.set_subtitle(f"Saved verified release to {path}")
            self.toast("Verified download complete. Use your system software installer to install it.")
        return False

    def refresh_all(self):
        for app in load_catalog(CATALOG):
            if app["id"] in self.rows:
                self.check_app(app)


class StoreApp(Adw.Application):
    def __init__(self):
        super().__init__(application_id="com.goreecloud.AppStore", flags=Gio.ApplicationFlags.DEFAULT_FLAGS)

    def do_activate(self):
        window = self.props.active_window or StoreWindow(self)
        window.present()


if __name__ == "__main__":
    raise SystemExit(StoreApp().run([]))
