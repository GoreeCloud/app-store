package com.goreecloud.appstore

import android.content.Context
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.MessageDigest
import javax.net.ssl.HttpsURLConnection

data class StoreEntry(
    val id: String,
    val name: String,
    val repo: String,
    val summary: String,
    val packageId: String?
)
data class ApkAsset(val name: String, val url: String, val bytes: Long, val digest: String)
data class PublishedRelease(val tag: String, val asset: ApkAsset?)

class StoreRepository(private val context: Context) {
    private val repoPattern = Regex("[A-Za-z0-9_.-]+")
    private val digestPattern = Regex("sha256:[0-9a-fA-F]{64}")
    private val maxDownload = 512L * 1024L * 1024L
    private val githubPrefix = "https://github.com/GoreeCloud/"
    private val redirectHosts = setOf(
        "github.com", "release-assets.githubusercontent.com", "objects.githubusercontent.com"
    )

    val products: List<StoreEntry> = context.assets.open("catalog.json").bufferedReader().use {
        val catalog = JSONObject(it.readText())
        require(catalog.getInt("schema_version") == 1 && catalog.getString("owner") == "GoreeCloud")
        val products = catalog.getJSONArray("products")
        buildList {
            val seen = mutableSetOf<String>()
            for (i in 0 until products.length()) {
                val p = products.getJSONObject(i)
                val platformArray = p.getJSONArray("platforms")
                if ((0 until platformArray.length()).none { index -> platformArray.getString(index) == "android" }) continue
                val id = p.getString("id")
                val repo = p.getString("repo")
                require(id.matches(repoPattern) && repo.matches(repoPattern) && seen.add(id))
                val packageId = p.optString("android_package").ifBlank { null }
                if (packageId != null) require(packageId.matches(Regex("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)+")))
                add(StoreEntry(id, p.getString("name"), repo, p.getString("summary"), packageId))
            }
        }
    }

    fun latest(product: StoreEntry): PublishedRelease? {
        require(products.contains(product))
        val url = URL("https://api.github.com/repos/GoreeCloud/${product.repo}/releases/latest")
        val conn = url.openConnection() as HttpsURLConnection
        conn.connectTimeout = 12000
        conn.readTimeout = 12000
        conn.instanceFollowRedirects = false
        conn.setRequestProperty("User-Agent", "GoreeCloud-App-Store/0.1")
        conn.setRequestProperty("Accept", "application/vnd.github+json")
        try {
            if (conn.responseCode == 404) return null
            check(conn.responseCode == 200) { "GitHub returned HTTP ${conn.responseCode}" }
            val data = conn.inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    check(output.size() + n <= 2 * 1024 * 1024) { "Oversized metadata" }
                    output.write(buffer, 0, n)
                }
                output.toString("UTF-8")
            }
            val obj = JSONObject(data)
            if (obj.optBoolean("draft") || obj.optBoolean("prerelease")) return null
            val assets = obj.getJSONArray("assets")
            var trusted: ApkAsset? = null
            for (i in 0 until assets.length()) {
                val item = assets.getJSONObject(i)
                val name = item.getString("name")
                val link = item.getString("browser_download_url")
                val size = item.getLong("size")
                val digest = item.optString("digest")
                val prefix = "${githubPrefix}${product.repo}/releases/download/"
                if (name.endsWith(".apk", ignoreCase = true) &&
                    name.matches(Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,180}")) &&
                    link.startsWith(prefix) && size in 1..maxDownload &&
                    digest.matches(digestPattern)) {
                    trusted = ApkAsset(name, link, size, digest.lowercase())
                    break
                }
            }
            return PublishedRelease(obj.getString("tag_name"), trusted)
        } finally {
            conn.disconnect()
        }
    }

    private fun verifiedConnection(url: String): HttpsURLConnection {
        var next = URL(url)
        repeat(6) {
            val uri = next.toURI()
            require(uri.scheme == "https" && uri.host in redirectHosts &&
                    uri.userInfo == null && (uri.port == -1 || uri.port == 443))
            val connection = next.openConnection() as HttpsURLConnection
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.setRequestProperty("User-Agent", "GoreeCloud-App-Store/0.1")
            val code = connection.responseCode
            if (code == 200) return connection
            if (code in listOf(301, 302, 303, 307, 308)) {
                val location = connection.getHeaderField("Location") ?: error("Redirect missing Location")
                next = URI(next.toString()).resolve(location).toURL()
                connection.disconnect()
            } else {
                connection.disconnect()
                error("Download failed: HTTP $code")
            }
        }
        error("Too many redirects")
    }

    fun download(product: StoreEntry, asset: ApkAsset): File {
        require(products.contains(product))
        require(asset.name.endsWith(".apk", ignoreCase = true))
        require(asset.name.matches(Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,180}")))
        require(asset.url.startsWith("${githubPrefix}${product.repo}/releases/download/"))
        require(asset.digest.matches(digestPattern) && asset.bytes in 1..maxDownload)
        val folder = File(context.cacheDir, "verified").apply { mkdirs() }
        val temp = File.createTempFile("goreecloud-", ".part", folder)
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            var total = 0L
            val conn = verifiedConnection(asset.url)
            try {
                conn.inputStream.use { input ->
                    temp.outputStream().use { output ->
                        val buffer = ByteArray(1024 * 1024)
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            total += read
                            check(total <= asset.bytes && total <= maxDownload) { "Unexpected package length" }
                            digest.update(buffer, 0, read)
                            output.write(buffer, 0, read)
                        }
                    }
                }
            } finally {
                conn.disconnect()
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            check(total == asset.bytes && "sha256:$actual" == asset.digest) { "Checksum or size mismatch" }
            val dest = File(folder, asset.name)
            check(temp.renameTo(dest)) { "Could not finalize download" }
            saveToDownloads(dest)
            return dest
        } finally {
            if (temp.exists()) temp.delete()
        }
    }

    private fun saveToDownloads(file: File) {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, file.name)
            put(MediaStore.Downloads.MIME_TYPE, "application/vnd.android.package-archive")
            put(MediaStore.Downloads.RELATIVE_PATH, "Download/GoreeCloud")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("Downloads storage unavailable")
        try {
            resolver.openOutputStream(uri)?.use { output -> file.inputStream().use { it.copyTo(output) } }
                ?: error("Could not open Downloads destination")
            resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
        } catch (t: Throwable) {
            resolver.delete(uri, null, null)
            throw t
        }
    }
}
