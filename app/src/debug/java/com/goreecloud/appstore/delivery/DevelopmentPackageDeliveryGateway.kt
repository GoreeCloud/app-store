package com.goreecloud.appstore.delivery

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import com.goreecloud.appstore.BuildConfig
import com.goreecloud.appstore.domain.IdentitySession
import com.goreecloud.appstore.domain.StoreItem
import com.goreecloud.appstore.domain.StoreItemType
import com.goreecloud.appstore.install.DevelopmentInstallResultReceiver
import org.json.JSONObject
import java.io.File
import java.net.URL
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.Executors
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

internal class DevelopmentPackageDeliveryGateway(
    private val context: Context,
) : PackageDeliveryGateway {
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val baseUrl = BuildConfig.DEVELOPMENT_DELIVERY_BASE_URL.trim().trimEnd('/')
    private val bearerToken = BuildConfig.DEVELOPMENT_DELIVERY_TOKEN.trim()
    private val tlsCertificateSha256 =
        BuildConfig.DEVELOPMENT_DELIVERY_TLS_CERT_SHA256.trim().lowercase()

    override val isAvailable: Boolean =
        baseUrl.startsWith("https://") &&
            bearerToken.length >= 32 &&
            CANONICAL_SHA256.matches(tlsCertificateSha256)

    override fun probe(
        session: IdentitySession,
        item: StoreItem,
        callback: (PackageDeliveryState) -> Unit,
    ) {
        if (!isAvailable) {
            callbackOnMain(
                callback,
                PackageDeliveryState.Unavailable(
                    "Development package delivery is not configured in this build.",
                ),
            )
            return
        }
        if (!session.isAuthenticated) {
            callbackOnMain(
                callback,
                PackageDeliveryState.Unavailable("Sign in to a Development identity first."),
            )
            return
        }
        if (item.type != StoreItemType.APPLICATION) {
            callbackOnMain(
                callback,
                PackageDeliveryState.Unavailable("This catalog entry is not an Android application."),
            )
            return
        }

        callbackOnMain(callback, PackageDeliveryState.Checking)
        executor.execute {
            val state = runCatching {
                val release = fetchRelease(session, item)
                val installed = installedVersionCode(release.packageName)
                if (installed != null && installed >= release.versionCode) {
                    PackageDeliveryState.Installed(release, installed)
                } else {
                    PackageDeliveryState.Ready(release)
                }
            }.getOrElse { error ->
                PackageDeliveryState.Failed(
                    reason = sanitizedFailure(error, "Development release verification failed."),
                )
            }
            callbackOnMain(callback, state)
        }
    }

    override fun downloadAndInstall(
        session: IdentitySession,
        item: StoreItem,
        release: DevelopmentDeliveryRelease,
        callback: (PackageDeliveryState) -> Unit,
    ) {
        if (!isAvailable || !session.isAuthenticated) {
            callbackOnMain(
                callback,
                PackageDeliveryState.Failed("Development package delivery is unavailable."),
            )
            return
        }

        callbackOnMain(callback, PackageDeliveryState.Downloading(release))
        executor.execute {
            val state = runCatching {
                validateReleaseBinding(item, release)
                val stagedApk = stageVerifiedApk(session, release)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    !context.packageManager.canRequestPackageInstalls()
                ) {
                    val settingsIntent = Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:\${context.packageName}"),
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(settingsIntent)
                    PackageDeliveryState.InstallPermissionRequired(release)
                } else {
                    commitPackageInstall(stagedApk, release)
                    PackageDeliveryState.AwaitingAndroid(release)
                }
            }.getOrElse { error ->
                PackageDeliveryState.Failed(
                    reason = sanitizedFailure(error, "Development package installation was blocked."),
                )
            }
            callbackOnMain(callback, state)
        }
    }

    private fun fetchRelease(
        session: IdentitySession,
        item: StoreItem,
    ): DevelopmentDeliveryRelease {
        val encodedItemId = java.net.URLEncoder.encode(item.id, Charsets.UTF_8.name())
        val response = getJson("/v1/releases/$encodedItemId", session.subjectId)
        if (response.optString("environment") != "development") {
            error("release_environment_not_development")
        }
        val releaseJson = response.getJSONObject("release")
        val wardveil = releaseJson.getJSONObject("wardveil")
        val release = DevelopmentDeliveryRelease(
            storeItemId = releaseJson.getString("storeItemId"),
            artifactId = releaseJson.getString("artifactId"),
            packageName = releaseJson.getString("packageName"),
            versionName = releaseJson.getString("versionName"),
            versionCode = releaseJson.getLong("versionCode"),
            releaseChannel = releaseJson.getString("releaseChannel"),
            minSdk = releaseJson.getInt("minSdk"),
            sha256 = releaseJson.getString("sha256").lowercase(),
            certificateSha256 = releaseJson.getString("certificateSha256").lowercase(),
            sizeBytes = releaseJson.getLong("sizeBytes"),
            downloadPath = releaseJson.getString("downloadPath"),
            wardveilEvidenceRef = wardveil.getString("evidenceRef"),
            wardveilValidUntilEpochSeconds = wardveil.getLong("validUntilEpochSeconds"),
        )
        if (wardveil.optString("result") != "clean") {
            error("wardveil_development_scan_not_clean")
        }
        if (wardveil.optBoolean("protectionClaimAuthority", true)) {
            error("unexpected_broad_wardveil_claim")
        }
        if (release.wardveilValidUntilEpochSeconds <= System.currentTimeMillis() / 1000L) {
            error("wardveil_development_scan_expired")
        }
        if (!release.wardveilEvidenceRef.contains(release.sha256)) {
            error("wardveil_evidence_digest_mismatch")
        }
        validateReleaseBinding(item, release)
        return release
    }

    private fun validateReleaseBinding(
        item: StoreItem,
        release: DevelopmentDeliveryRelease,
    ) {
        if (release.storeItemId != item.id) error("release_item_mismatch")
        if (item.packageName != release.packageName) error("release_package_mismatch")
        if (item.version != release.versionName) error("release_version_mismatch")
        if (item.releaseChannel.name.lowercase() != release.releaseChannel.lowercase()) {
            error("release_channel_mismatch")
        }
        if (!CANONICAL_SHA256.matches(release.sha256)) error("release_digest_invalid")
        if (!CANONICAL_SHA256.matches(release.certificateSha256)) {
            error("release_certificate_digest_invalid")
        }
        if (release.versionCode <= 0L || release.minSdk <= 0) error("release_version_metadata_invalid")
        if (release.sizeBytes <= 0L || release.sizeBytes > MAX_APK_BYTES) {
            error("release_size_invalid")
        }
        if (!release.downloadPath.startsWith("/v1/artifacts/") ||
            release.downloadPath.contains("..")
        ) {
            error("release_download_path_invalid")
        }
        if (Build.VERSION.SDK_INT < release.minSdk) error("device_sdk_incompatible")
    }

    private fun stageVerifiedApk(
        session: IdentitySession,
        release: DevelopmentDeliveryRelease,
    ): File {
        val directory = File(context.cacheDir, "development-delivery").apply { mkdirs() }
        val target = File(directory, "\${release.sha256}.apk")
        if (!target.isFile || target.length() != release.sizeBytes ||
            sha256(target) != release.sha256
        ) {
            target.delete()
            downloadArtifact(session, release, target)
        }
        verifyApk(target, release)
        return target
    }

    private fun downloadArtifact(
        session: IdentitySession,
        release: DevelopmentDeliveryRelease,
        target: File,
    ) {
        val connection = openConnection(release.downloadPath, session.subjectId)
        try {
            val status = connection.responseCode
            if (status != HttpsURLConnection.HTTP_OK) {
                error("artifact_http_$status")
            }
            val length = connection.contentLengthLong
            if (length != release.sizeBytes) error("artifact_content_length_mismatch")
            target.outputStream().buffered().use { output ->
                connection.inputStream.use { input ->
                    val buffer = ByteArray(64 * 1024)
                    var total = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        if (total > release.sizeBytes || total > MAX_APK_BYTES) {
                            error("artifact_size_exceeded")
                        }
                        output.write(buffer, 0, read)
                    }
                    if (total != release.sizeBytes) error("artifact_size_mismatch")
                }
            }
        } catch (error: Throwable) {
            target.delete()
            throw error
        } finally {
            connection.disconnect()
        }
        if (sha256(target) != release.sha256) {
            target.delete()
            error("artifact_digest_mismatch")
        }
    }

    private fun verifyApk(
        apk: File,
        release: DevelopmentDeliveryRelease,
    ) {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }
        @Suppress("DEPRECATION")
        val info = context.packageManager.getPackageArchiveInfo(apk.absolutePath, flags)
            ?: error("apk_metadata_unreadable")
        if (info.packageName != release.packageName) error("apk_package_mismatch")
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
        if (versionCode != release.versionCode) error("apk_version_code_mismatch")
        if (info.versionName != release.versionName) error("apk_version_name_mismatch")

        val certificateDigests = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = info.signingInfo ?: error("apk_signing_info_missing")
            signingInfo.apkContentsSigners.map { signature ->
                sha256(signature.toByteArray())
            }
        } else {
            @Suppress("DEPRECATION")
            info.signatures.orEmpty().map { signature ->
                sha256(signature.toByteArray())
            }
        }
        if (release.certificateSha256 !in certificateDigests) {
            error("apk_signing_certificate_mismatch")
        }
    }

    private fun commitPackageInstall(
        apk: File,
        release: DevelopmentDeliveryRelease,
    ) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            .apply {
                setAppPackageName(release.packageName)
                setSize(release.sizeBytes)
                setInstallReason(PackageManager.INSTALL_REASON_USER)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_REQUIRED)
                }
            }
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { installSession ->
            apk.inputStream().use { input ->
                installSession.openWrite("base.apk", 0L, release.sizeBytes).use { output ->
                    input.copyTo(output, 64 * 1024)
                    installSession.fsync(output)
                }
            }
            val resultIntent = Intent(context, DevelopmentInstallResultReceiver::class.java)
                .setAction(DevelopmentInstallResultReceiver.ACTION_INSTALL_RESULT)
                .putExtra(DevelopmentInstallResultReceiver.EXTRA_PACKAGE_NAME, release.packageName)
                .putExtra(DevelopmentInstallResultReceiver.EXTRA_VERSION_CODE, release.versionCode)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                sessionId,
                resultIntent,
                flags,
            )
            installSession.commit(pendingIntent.intentSender)
        }
    }

    private fun installedVersionCode(packageName: String): Long? = try {
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(0),
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(packageName, 0)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

    private fun getJson(path: String, subjectId: String): JSONObject {
        val connection = openConnection(path, subjectId)
        try {
            val status = connection.responseCode
            if (status != HttpsURLConnection.HTTP_OK) {
                error("release_http_$status")
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            if (body.length > MAX_JSON_CHARS) error("release_response_too_large")
            return JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun openConnection(path: String, subjectId: String): HttpsURLConnection {
        if (!path.startsWith("/")) error("backend_path_invalid")
        val connection = URL(baseUrl + path).openConnection() as? HttpsURLConnection
            ?: error("backend_not_https")
        connection.connectTimeout = 10_000
        connection.readTimeout = 45_000
        connection.instanceFollowRedirects = false
        connection.requestMethod = "GET"
        connection.setRequestProperty("Authorization", "Bearer $bearerToken")
        connection.setRequestProperty("X-GoreeCloud-Dev-Subject", subjectId)
        connection.setRequestProperty(
            "Accept",
            "application/json, application/vnd.android.package-archive",
        )
        connection.setRequestProperty(
            "User-Agent",
            "GoreeCloud-App-Store-Development/\${BuildConfig.VERSION_NAME}",
        )
        connection.sslSocketFactory = pinnedSslContext().socketFactory
        return connection
    }

    private fun pinnedSslContext(): SSLContext {
        val expected = tlsCertificateSha256
        val trustManager = object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()

            override fun checkClientTrusted(
                chain: Array<out X509Certificate>?,
                authType: String?,
            ) {
                throw CertificateException("client_certificate_auth_not_supported")
            }

            override fun checkServerTrusted(
                chain: Array<out X509Certificate>?,
                authType: String?,
            ) {
                val leaf = chain?.firstOrNull() ?: throw CertificateException("empty_server_chain")
                val actual = sha256(leaf.encoded)
                if (!MessageDigest.isEqual(
                        actual.toByteArray(Charsets.US_ASCII),
                        expected.toByteArray(Charsets.US_ASCII),
                    )
                ) {
                    throw CertificateException("development_backend_certificate_mismatch")
                }
            }
        }
        return SSLContext.getInstance("TLS").apply {
            init(null, arrayOf(trustManager), SecureRandom())
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().toHex()
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).toHex()

    private fun ByteArray.toHex(): String = joinToString("") { byte -> "%02x".format(byte) }

    private fun sanitizedFailure(error: Throwable, fallback: String): String {
        val token = error.message
            ?.takeIf { SAFE_FAILURE.matches(it) }
            ?.replace('_', ' ')
            ?.take(120)
        return token ?: fallback
    }

    private fun callbackOnMain(
        callback: (PackageDeliveryState) -> Unit,
        state: PackageDeliveryState,
    ) {
        mainHandler.post { callback(state) }
    }

    companion object {
        private val CANONICAL_SHA256 = Regex("^[0-9a-f]{64}$")
        private val SAFE_FAILURE = Regex("^[A-Za-z0-9_.:-]{1,160}$")
        private const val MAX_APK_BYTES = 64L * 1024L * 1024L
        private const val MAX_JSON_CHARS = 256 * 1024
    }
}
