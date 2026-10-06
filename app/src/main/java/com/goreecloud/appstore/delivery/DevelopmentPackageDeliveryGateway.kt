package com.goreecloud.appstore.delivery

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.goreecloud.appstore.BuildConfig
import com.goreecloud.appstore.domain.IdentitySession
import com.goreecloud.appstore.domain.StoreItem
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.MessageDigest
import java.util.Locale

private const val MAX_DEVELOPMENT_APK_BYTES = 250L * 1024L * 1024L

data class DevelopmentRelease(
    val itemId: String,
    val artifactId: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val minSdk: Int,
    val releaseChannel: String,
    val sha256: String,
    val signerSha256: String,
    val sizeBytes: Long,
    val artifactPath: String,
)

sealed interface DevelopmentReleaseResolution {
    data class Available(val release: DevelopmentRelease) : DevelopmentReleaseResolution
    data object NotAvailable : DevelopmentReleaseResolution
    data class Failed(val message: String) : DevelopmentReleaseResolution
}

sealed interface DevelopmentInstallAttempt {
    data object Submitted : DevelopmentInstallAttempt
    data object PermissionRequired : DevelopmentInstallAttempt
    data class Failed(val message: String) : DevelopmentInstallAttempt
}

class DevelopmentPackageDeliveryGateway private constructor(
    private val context: Context,
    private val backend: DevelopmentBackendClient,
) {
    fun resolve(
        session: IdentitySession,
        item: StoreItem,
    ): DevelopmentReleaseResolution = backend.resolve(session, item.id)

    fun downloadVerifyAndInstall(
        session: IdentitySession,
        item: StoreItem,
        release: DevelopmentRelease,
    ): DevelopmentInstallAttempt {
        if (item.id != release.itemId) {
            return DevelopmentInstallAttempt.Failed(
                "Backend release identity does not match the selected item.",
            )
        }
        if (release.releaseChannel !in setOf("development", "debug")) {
            return DevelopmentInstallAttempt.Failed(
                "The Development backend returned a non-Development release.",
            )
        }
        if (Build.VERSION.SDK_INT < release.minSdk) {
            return DevelopmentInstallAttempt.Failed(
                "This device does not meet the package minimum Android version.",
            )
        }

        val directory = File(context.cacheDir, "development-delivery").apply { mkdirs() }
        val apk = File(directory, release.artifactId + ".apk")
        return try {
            backend.download(session, release, apk)
            val verification = ApkArchiveVerifier.verify(context, apk, release)
            if (verification != null) {
                DevelopmentInstallAttempt.Failed(verification)
            } else {
                AndroidDevelopmentPackageInstaller.install(context, apk, release)
            }
        } catch (error: Exception) {
            DevelopmentInstallAttempt.Failed(
                error.message?.takeIf { it.isNotBlank() }
                    ?: "Development package delivery failed.",
            )
        } finally {
            apk.delete()
        }
    }

    companion object {
        fun createOrNull(context: Context): DevelopmentPackageDeliveryGateway? {
            if (!BuildConfig.DEBUG) return null
            val url = BuildConfig.DEVELOPMENT_BACKEND_URL.trim()
            val credential = BuildConfig.DEVELOPMENT_BACKEND_TOKEN
            if (url.isBlank() || credential.length < 32) return null
            return runCatching {
                DevelopmentPackageDeliveryGateway(
                    context.applicationContext,
                    DevelopmentBackendClient(url, credential),
                )
            }.getOrNull()
        }
    }
}

private class DevelopmentBackendClient(
    baseUrl: String,
    private val credential: String,
) {
    private val baseUrl = validateLoopbackBaseUrl(baseUrl)

    fun resolve(
        session: IdentitySession,
        itemId: String,
    ): DevelopmentReleaseResolution {
        val connection = open("/v1/items/${itemId.requireCatalogId()}", session)
        return try {
            when (val status = connection.responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    val payload = connection.inputStream.bufferedReader().use { it.readText() }
                    DevelopmentReleaseResolution.Available(
                        parseRelease(JSONObject(payload), itemId),
                    )
                }

                HttpURLConnection.HTTP_NOT_FOUND -> DevelopmentReleaseResolution.NotAvailable
                HttpURLConnection.HTTP_UNAUTHORIZED,
                HttpURLConnection.HTTP_FORBIDDEN,
                -> DevelopmentReleaseResolution.Failed(
                    "Development backend authorization was rejected.",
                )

                else -> DevelopmentReleaseResolution.Failed(
                    "Development backend returned HTTP $status.",
                )
            }
        } catch (error: Exception) {
            DevelopmentReleaseResolution.Failed(
                error.message?.takeIf { it.isNotBlank() }
                    ?: "Development backend is unavailable.",
            )
        } finally {
            connection.disconnect()
        }
    }

    fun download(
        session: IdentitySession,
        release: DevelopmentRelease,
        destination: File,
    ) {
        require(release.sizeBytes in 1..MAX_DEVELOPMENT_APK_BYTES) {
            "Development artifact size is outside the accepted range."
        }
        val connection = open(release.artifactPath, session)
        try {
            require(connection.responseCode == HttpURLConnection.HTTP_OK) {
                "Development artifact download was rejected."
            }
            require(connection.contentLengthLong == release.sizeBytes) {
                "Development artifact length does not match release metadata."
            }

            val digest = MessageDigest.getInstance("SHA-256")
            var copied = 0L
            destination.parentFile?.mkdirs()
            connection.inputStream.use { input ->
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(128 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        copied += count
                        require(copied <= release.sizeBytes) {
                            "Development artifact exceeded its declared size."
                        }
                        digest.update(buffer, 0, count)
                        output.write(buffer, 0, count)
                    }
                    output.fd.sync()
                }
            }
            require(copied == release.sizeBytes) {
                "Development artifact download ended before the declared size."
            }
            require(digest.digest().toHex() == release.sha256) {
                "Development artifact SHA-256 verification failed."
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun open(
        path: String,
        session: IdentitySession,
    ): HttpURLConnection {
        require(path.startsWith("/") && !path.startsWith("//")) {
            "Invalid Development backend path."
        }
        val connection = URL(baseUrl + path).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 7_500
        connection.readTimeout = 30_000
        connection.instanceFollowRedirects = false
        connection.useCaches = false
        connection.setRequestProperty("Authorization", "Bearer $credential")
        connection.setRequestProperty(
            "X-GoreeCloud-Development-Subject",
            session.subjectId,
        )
        connection.setRequestProperty(
            "X-GoreeCloud-Development-Audiences",
            session.audiences.sorted().joinToString(","),
        )
        connection.setRequestProperty(
            "Accept",
            "application/json, application/vnd.android.package-archive",
        )
        return connection
    }

    private fun parseRelease(
        payload: JSONObject,
        expectedItemId: String,
    ): DevelopmentRelease {
        require(payload.getInt("schemaVersion") == 1)
        require(payload.getString("environment") == "development-local")
        require(payload.getString("authority") == "development-local-only")

        val itemId = payload.getString("itemId")
        val artifactId = payload.getString("artifactId")
        val packageName = payload.getString("packageName")
        val versionName = payload.getString("versionName")
        val releaseChannel = payload.getString("releaseChannel").lowercase(Locale.US)
        val sha256 = payload.getString("sha256").lowercase(Locale.US)
        val signerSha256 = payload.getString("signerSha256").lowercase(Locale.US)
        val artifactPath = payload.getString("artifactPath")

        require(itemId == expectedItemId)
        require(artifactId.matches(Regex("[a-z0-9][a-z0-9._-]{1,159}")))
        require(
            packageName.matches(
                Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+"),
            ),
        )
        require(versionName.isNotBlank())
        require(releaseChannel in setOf("development", "debug"))
        require(sha256.matches(Regex("[0-9a-f]{64}")))
        require(signerSha256.matches(Regex("[0-9a-f]{64}")))
        require(artifactPath == "/v1/artifacts/$artifactId")

        return DevelopmentRelease(
            itemId = itemId,
            artifactId = artifactId,
            packageName = packageName,
            versionName = versionName,
            versionCode = payload.getLong("versionCode").also { require(it > 0) },
            minSdk = payload.getInt("minSdk").also { require(it > 0) },
            releaseChannel = releaseChannel,
            sha256 = sha256,
            signerSha256 = signerSha256,
            sizeBytes = payload.getLong("sizeBytes").also {
                require(it in 1..MAX_DEVELOPMENT_APK_BYTES)
            },
            artifactPath = artifactPath,
        )
    }

    private fun validateLoopbackBaseUrl(value: String): String {
        val normalized = value.trim().trimEnd('/')
        val uri = URI(normalized)
        require(uri.scheme == "http") {
            "Development backend must use the local HTTP transport."
        }
        require(uri.host in setOf("127.0.0.1", "localhost")) {
            "Development backend must be loopback-only."
        }
        require(uri.rawUserInfo == null && uri.rawQuery == null && uri.rawFragment == null)
        require(uri.path.isNullOrEmpty() || uri.path == "/")
        return normalized
    }
}

private fun String.requireCatalogId(): String {
    require(matches(Regex("[a-z0-9][a-z0-9._-]{1,127}")))
    return this
}

private object ApkArchiveVerifier {
    fun verify(
        context: Context,
        apk: File,
        release: DevelopmentRelease,
    ): String? {
        val packageManager = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }
        val info = packageManager.getPackageArchiveInfo(apk.absolutePath, flags)
            ?: return "Android could not parse the downloaded APK."

        if (info.packageName != release.packageName) {
            return "Downloaded APK package identity does not match release metadata."
        }
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
        if (versionCode != release.versionCode || info.versionName != release.versionName) {
            return "Downloaded APK version does not match release metadata."
        }

        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.apkContentsSigners?.toList().orEmpty()
        } else {
            @Suppress("DEPRECATION")
            info.signatures?.toList().orEmpty()
        }
        if (signatures.isEmpty()) {
            return "Downloaded APK has no observable signing certificate."
        }

        val signerDigests = signatures
            .map {
                MessageDigest.getInstance("SHA-256")
                    .digest(it.toByteArray())
                    .toHex()
            }
            .toSet()
        if (signerDigests != setOf(release.signerSha256)) {
            return "Downloaded APK signing identity does not match release metadata."
        }
        return null
    }
}

private object AndroidDevelopmentPackageInstaller {
    fun install(
        context: Context,
        apk: File,
        release: DevelopmentRelease,
    ): DevelopmentInstallAttempt {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}"),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            return DevelopmentInstallAttempt.PermissionRequired
        }

        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(
            PackageInstaller.SessionParams.MODE_FULL_INSTALL,
        ).apply {
            setAppPackageName(release.packageName)
            setSize(release.sizeBytes)
        }
        val sessionId = installer.createSession(params)
        try {
            installer.openSession(sessionId).use { session ->
                apk.inputStream().use { input ->
                    session.openWrite("base.apk", 0, release.sizeBytes).use { output ->
                        input.copyTo(output)
                        session.fsync(output)
                    }
                }
                val callback = Intent(
                    context,
                    DevelopmentPackageInstallStatusReceiver::class.java,
                ).apply {
                    action =
                        "${context.packageName}.DEVELOPMENT_PACKAGE_INSTALL_STATUS.$sessionId"
                    putExtra(
                        DevelopmentPackageInstallStatusReceiver.EXTRA_PACKAGE_NAME,
                        release.packageName,
                    )
                    putExtra(
                        DevelopmentPackageInstallStatusReceiver.EXTRA_VERSION_CODE,
                        release.versionCode,
                    )
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    sessionId,
                    callback,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
                )
                session.commit(pendingIntent.intentSender)
            }
        } catch (error: Exception) {
            runCatching { installer.abandonSession(sessionId) }
            return DevelopmentInstallAttempt.Failed(
                error.message?.takeIf { it.isNotBlank() }
                    ?: "Android package installation could not start.",
            )
        }
        return DevelopmentInstallAttempt.Submitted
    }
}

class DevelopmentPackageInstallStatusReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val status = intent.getIntExtra(
            PackageInstaller.EXTRA_STATUS,
            PackageInstaller.STATUS_FAILURE,
        )
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            val confirmation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_INTENT) as? Intent
            }
            confirmation?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (confirmation != null) {
                context.startActivity(confirmation)
            }
            return
        }

        val expectedPackageName = intent.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty()
        if (expectedPackageName.isBlank()) return
        val observedPackageName = intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME)
        val reconciledStatus =
            if (
                status == PackageInstaller.STATUS_SUCCESS &&
                observedPackageName != expectedPackageName
            ) {
                PackageInstaller.STATUS_FAILURE_INVALID
            } else {
                status
            }

        val statusMessage = intent
            .getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
            .orEmpty()
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putInt("$expectedPackageName.status", reconciledStatus)
            .putString("$expectedPackageName.message", statusMessage.take(500))
            .putLong("$expectedPackageName.observedAt", System.currentTimeMillis())
            .apply()
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "packageName"
        const val EXTRA_VERSION_CODE = "versionCode"
        private const val PREFERENCES = "development-package-install-results"
    }
}

private fun ByteArray.toHex(): String =
    joinToString(separator = "") { byte -> "%02x".format(byte) }
