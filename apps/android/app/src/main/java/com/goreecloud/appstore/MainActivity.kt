package com.goreecloud.appstore

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject

private data class StoreItem(
    val slug: String,
    val name: String,
    val summary: String,
    val category: String,
    val platforms: List<String>,
    val sourceUrl: String,
)

private fun readCatalog(activity: ComponentActivity): List<StoreItem> {
    val json = activity.assets.open("catalog.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
    val array = JSONObject(json).getJSONArray("apps")
    return (0 until array.length()).map { index ->
        val entry = array.getJSONObject(index)
        val platforms = entry.getJSONArray("platforms")
        StoreItem(
            slug = entry.getString("slug"),
            name = entry.getString("name"),
            summary = entry.getString("summary"),
            category = entry.getString("category"),
            platforms = (0 until platforms.length()).map { platforms.getString(it) },
            sourceUrl = entry.getString("source_url"),
        )
    }.sortedBy { it.name.lowercase() }
}

// Snapshot of Iris light/dark semantic roles from the pinned gc-ui source.
// A shared, published native adapter will replace this bridge after acceptance.
private val irisLight = lightColorScheme(
    primary = Color(0xFF5146C8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8E5FF),
    onPrimaryContainer = Color(0xFF30266F),
    background = Color(0xFFF6F7FB),
    onBackground = Color(0xFF1B2030),
    surface = Color.White,
    onSurface = Color(0xFF1B2030),
)
private val irisDark = darkColorScheme(
    primary = Color(0xFFBFB8FF),
    onPrimary = Color(0xFF272057),
    primaryContainer = Color(0xFF413974),
    onPrimaryContainer = Color(0xFFE8E5FF),
    background = Color(0xFF0D121D),
    onBackground = Color(0xFFF2F4FB),
    surface = Color(0xFF171D2A),
    onSurface = Color(0xFFF2F4FB),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val catalog = runCatching { readCatalog(this) }
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) irisDark else irisLight) {
                AppStoreScreen(
                    apps = catalog.getOrNull() ?: emptyList(),
                    error = catalog.exceptionOrNull()?.let { "Bundled catalog unavailable" },
                    openSource = { url ->
                        val uri = Uri.parse(url)
                        if (uri.scheme == "https" && uri.host == "github.com" &&
                            uri.path?.startsWith("/GoreeCloud/") == true) {
                            startActivity(Intent(Intent.ACTION_VIEW, uri))
                        }
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppStoreScreen(apps: List<StoreItem>, error: String?, openSource: (String) -> Unit) {
    var search by remember { mutableStateOf("") }
    var platform by remember { mutableStateOf("all") }
    var category by remember { mutableStateOf("all") }
    val categories = remember(apps) { listOf("all") + apps.map { it.category }.distinct().sorted() }
    val visible = remember(apps, search, platform, category) {
        apps.filter { item ->
            (search.isBlank() || listOf(item.name, item.summary, item.category)
                .any { it.contains(search.trim(), ignoreCase = true) }) &&
                (platform == "all" || platform in item.platforms) &&
                (category == "all" || item.category == category)
        }
    }
    Scaffold(topBar = { TopAppBar(title = { Text("GoreeCloud App Store", fontWeight = FontWeight.Bold) }) }) { insets ->
        Column(
            modifier = Modifier.fillMaxSize().padding(insets)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 18.dp),
        ) {
            Spacer(Modifier.height(12.dp))
            Text("Discover applications", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Text(
                    "Development preview • Browse source only. No verified installers or automatic updates.",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.height(18.dp))
            OutlinedTextField(
                value = search, onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search applications") },
                singleLine = true,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                listOf("all", "web", "linux", "android").forEach { value ->
                    FilterChip(selected = platform == value, onClick = { platform = value },
                        label = { Text(value.replaceFirstChar { it.uppercase() }) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                categories.forEach { value ->
                    FilterChip(selected = category == value, onClick = { category = value },
                        label = { Text(if (value == "all") "All categories" else value) })
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(error ?: "${visible.size} of ${apps.size} source projects",
                style = MaterialTheme.typography.labelMedium,
                color = if (error == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(10.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 30.dp)) {
                items(visible, key = { it.slug }) { item ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Text(item.name, style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(item.category + " · " + item.platforms.joinToString(" · "),
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(12.dp))
                            Text(item.summary, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(14.dp))
                            Button(onClick = { openSource(item.sourceUrl) }) { Text("View source") }
                        }
                    }
                }
            }
        }
    }
}
