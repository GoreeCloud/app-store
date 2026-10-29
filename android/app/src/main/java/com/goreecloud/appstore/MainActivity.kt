package com.goreecloud.appstore

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File
import java.util.concurrent.Executors

data class ReleaseUi(
    val message: String = "Published release not checked",
    val busy: Boolean = false,
    val asset: ApkAsset? = null,
    val file: File? = null
)

class MainActivity : ComponentActivity() {
    private val pool = Executors.newFixedThreadPool(3)
    private lateinit var store: StoreRepository
    private val state = mutableStateMapOf<String, ReleaseUi>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = StoreRepository(this)
        setContent {
            MaterialTheme(colorScheme = if (Build.VERSION.SDK_INT >= 31) dynamicLightColorScheme(this) else lightColorScheme()) {
                Surface(Modifier.fillMaxSize()) {
                    CatalogScreen(store.products, state,
                        onCheck = ::checkRelease,
                        onDownload = ::download,
                        onInstall = ::install)
                }
            }
        }
    }

    private fun checkRelease(entry: StoreEntry) {
        state[entry.id] = ReleaseUi(message = "Checking verified releases…", busy = true)
        pool.execute {
            val result = runCatching { store.latest(entry) }
            runOnUiThread {
                state[entry.id] = result.fold(
                    onSuccess = { release ->
                        when {
                            release == null -> ReleaseUi("No published stable GitHub release")
                            release.asset == null -> ReleaseUi("${release.tag} · no digest-verified Android APK")
                            else -> ReleaseUi("${release.tag} · ${release.asset.name}", asset = release.asset)
                        }
                    },
                    onFailure = { ReleaseUi("Release check failed: ${it.message?.take(100)}") }
                )
            }
        }
    }

    private fun download(entry: StoreEntry) {
        val asset = state[entry.id]?.asset ?: return
        state[entry.id] = ReleaseUi("Downloading and verifying APK…", true, asset)
        pool.execute {
            val result = runCatching { store.download(entry, asset) }
            runOnUiThread {
                state[entry.id] = result.fold(
                    onSuccess = { ReleaseUi("Verified APK saved in Downloads/GoreeCloud", asset = asset, file = it) },
                    onFailure = { ReleaseUi("Download refused: ${it.message?.take(100)}", asset = asset) }
                )
            }
        }
    }

    private fun install(entry: StoreEntry) {
        val file = state[entry.id]?.file ?: return
        val packageId = entry.packageId
        if (packageId == null) {
            state[entry.id] = state[entry.id]!!.copy(message = "Installer locked: package ID not yet pinned in catalog")
            return
        }
        val parsed = packageManager.getPackageArchiveInfo(file.absolutePath, 0)
        if (parsed?.packageName != packageId) {
            state[entry.id] = state[entry.id]!!.copy(message = "Installer refused: APK package identity mismatch")
            return
        }
        if (!packageManager.canRequestPackageInstalls()) {
            startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:$packageName")))
            return
        }
        val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
        startActivity(Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        })
    }

    override fun onDestroy() {
        pool.shutdownNow()
        super.onDestroy()
    }
}

@Composable
private fun CatalogScreen(
    apps: List<StoreEntry>,
    states: Map<String, ReleaseUi>,
    onCheck: (StoreEntry) -> Unit,
    onDownload: (StoreEntry) -> Unit,
    onInstall: (StoreEntry) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp)) {
            Text("GoreeCloud", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("App Store", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(10.dp))
            Text("First-party apps. Verified releases. Your device stays in control.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(apps, key = { it.id }) { app ->
                val current = states[app.id] ?: ReleaseUi()
                ElevatedCard(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Text(app.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(5.dp))
                        Text(app.summary, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(12.dp))
                        Text(current.message, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(enabled = !current.busy, onClick = {
                                if (current.asset == null) onCheck(app) else onDownload(app)
                            }) {
                                Text(if (current.busy) "Working…" else if (current.asset == null) "Check release" else "Download APK")
                            }
                            if (current.file != null) {
                                OutlinedButton(onClick = { onInstall(app) }) {
                                    Text(if (app.packageId == null) "Install locked" else "Install")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
