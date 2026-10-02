@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.goreecloud.appstore.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.goreecloud.appstore.R
import com.goreecloud.appstore.data.CatalogJsonLoader
import com.goreecloud.appstore.domain.EntitlementEngine
import com.goreecloud.appstore.domain.IdentitySession
import com.goreecloud.appstore.domain.ReleaseChannel
import com.goreecloud.appstore.domain.StoreItem
import com.goreecloud.appstore.domain.StoreItemType
import com.goreecloud.appstore.identity.DevelopmentIdentityGateway
import com.goreecloud.appstore.library.FavoriteCatalogStore
import com.goreecloud.appstore.library.SavedCatalogStore
import com.goreecloud.appstore.onboarding.AppStoreGuidanceState
import com.goreecloud.appstore.platform.IntegrationState
import com.goreecloud.appstore.platform.PlatformIntegrationRegistry
import com.goreecloud.appstore.platform.UnavailablePackageDeliveryGateway

enum class StoreTab(val title: String, val icon: ImageVector) {
    DISCOVER("Discover", Icons.Rounded.Home),
    APPS("Apps", Icons.Rounded.Apps),
    SERVICES("Services", Icons.Rounded.Cloud),
    UPDATES("Updates", Icons.Rounded.Update),
    LIBRARY("Library", Icons.Rounded.LibraryBooks),
}

@Composable
fun GoreeCloudAppStore(
    guidanceState: AppStoreGuidanceState,
    onShowGuidanceSettings: () -> Unit,
    onDismissGuidanceHint: (String) -> Unit,
) {
    val context = LocalContext.current
    val allItems = remember { CatalogJsonLoader.load(context) }
    val identityGateway = remember { DevelopmentIdentityGateway }
    val savedCatalogStore = remember(context.applicationContext) {
        SavedCatalogStore(context.applicationContext)
    }
    val favoriteCatalogStore = remember(context.applicationContext) {
        FavoriteCatalogStore(context.applicationContext)
    }
    val listState = rememberLazyListState()

    var session by remember { mutableStateOf(identityGateway.initialSession) }
    var selectedTab by remember { mutableStateOf(StoreTab.DISCOVER) }
    var query by remember { mutableStateOf("") }
    var selectedItem by remember { mutableStateOf<StoreItem?>(null) }
    var showPlatformStatus by remember { mutableStateOf(false) }
    var savedItemIds by remember(session.subjectId) {
        mutableStateOf(savedCatalogStore.load(session.subjectId))
    }
    var favoriteItemIds by remember(session.subjectId) {
        mutableStateOf(favoriteCatalogStore.load(session.subjectId))
    }
    var confirmClearSaved by remember(session.subjectId) { mutableStateOf(false) }

    val entitled = remember(session, allItems) {
        EntitlementEngine.visibleItems(session, allItems)
    }
    val savedVisible = remember(entitled, savedItemIds) {
        entitled.filter { it.id in savedItemIds }
    }
    val favoriteVisible = remember(entitled, favoriteItemIds) {
        entitled.filter { it.id in favoriteItemIds }
    }
    val visible = remember(entitled, selectedTab, query) {
        val tabItems = when (selectedTab) {
            StoreTab.DISCOVER -> entitled
            StoreTab.APPS -> entitled.filter { it.type == StoreItemType.APPLICATION }
            StoreTab.SERVICES -> entitled.filter { it.type == StoreItemType.SERVICE }
            StoreTab.UPDATES, StoreTab.LIBRARY -> emptyList()
        }
        if (query.isBlank()) {
            tabItems
        } else {
            tabItems.filter {
                it.name.contains(query, ignoreCase = true) ||
                    it.summary.contains(query, ignoreCase = true) ||
                    it.category.contains(query, ignoreCase = true)
            }
        }
    }

    LaunchedEffect(selectedTab, session.subjectId) {
        query = ""
        listState.scrollToItem(0)
    }

    GlazeTheme {
        Scaffold(
            topBar = {
                StoreTopBar(
                    session = session,
                    sessions = identityGateway.availableSessions,
                    onSessionSelected = { session = it },
                    onShowGuidanceSettings = onShowGuidanceSettings,
                    onShowPlatformStatus = { showPlatformStatus = true },
                )
            },
            bottomBar = {
                StoreNavigation(
                    selected = selectedTab,
                    onSelected = { selectedTab = it },
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { scaffoldPadding ->
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(scaffoldPadding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                when (selectedTab) {
                    StoreTab.DISCOVER -> {
                        item { DevelopmentStatusStrip(onClick = { showPlatformStatus = true }) }
                        item { StoreHero(visibleCount = entitled.size) }
                        if (guidanceState.isHintVisible(APP_STORE_CATALOG_HINT_ID)) {
                            item {
                                AppStoreCatalogGuidanceHint(
                                    onDismiss = {
                                        onDismissGuidanceHint(APP_STORE_CATALOG_HINT_ID)
                                    },
                                )
                            }
                        }
                        item { StoreSearch(query = query, onQueryChanged = { query = it }) }
                        item {
                            StoreSectionHeading(
                                title = "Available to you",
                                subtitle = catalogCountLabel(visible.size),
                            )
                        }
                    }

                    StoreTab.APPS -> {
                        item {
                            TabIntro(
                                title = "Apps",
                                body = "GoreeCloud applications available to the active development identity.",
                            )
                        }
                        item { StoreSearch(query = query, onQueryChanged = { query = it }) }
                    }

                    StoreTab.SERVICES -> {
                        item {
                            TabIntro(
                                title = "Services",
                                body = "GoreeCloud services available to the active development identity.",
                            )
                        }
                        item { StoreSearch(query = query, onQueryChanged = { query = it }) }
                    }

                    StoreTab.UPDATES -> {
                        item {
                            TabIntro(
                                title = "Updates",
                                body = "Application updates will appear here when production release delivery is connected.",
                            )
                        }
                        item {
                            UnavailableState(
                                icon = Icons.Rounded.Update,
                                title = "Updates are unavailable in this development build",
                                body = "The update feed remains disabled until authenticated release metadata and package-delivery integration are accepted.",
                                onDetails = { showPlatformStatus = true },
                            )
                        }
                    }

                    StoreTab.LIBRARY -> {
                        item {
                            TabIntro(
                                title = "Library",
                                body = "Keep device-local Favorites and Save for later collections for entitled GoreeCloud items. Installed and historical library state remains separate and unavailable.",
                            )
                        }
                        item {
                            StoreSectionHeading(
                                title = "Favorites",
                                subtitle = if (favoriteVisible.size == 1) {
                                    "1 favorite for this development identity"
                                } else {
                                    "${favoriteVisible.size} favorites for this development identity"
                                },
                            )
                        }
                        if (favoriteVisible.isEmpty()) {
                            item { FavoriteLibraryEmptyState() }
                        } else {
                            items(favoriteVisible, key = { "favorite:${it.id}" }) { item ->
                                StoreItemCard(item = item, onClick = { selectedItem = item })
                            }
                        }
                        item { Spacer(Modifier.height(6.dp)) }
                        item {
                            StoreSectionHeading(
                                title = "Saved for later",
                                subtitle = if (savedVisible.size == 1) {
                                    "1 item saved for this development identity"
                                } else {
                                    "${savedVisible.size} items saved for this development identity"
                                },
                            )
                        }
                        if (savedVisible.isEmpty()) {
                            item {
                                SavedLibraryEmptyState()
                            }
                        } else {
                            item {
                                TextButton(
                                    modifier = Modifier.heightIn(min = 48.dp),
                                    onClick = { confirmClearSaved = true },
                                ) {
                                    Text("Clear saved for later")
                                }
                            }
                            items(savedVisible, key = { "saved:${it.id}" }) { item ->
                                StoreItemCard(item = item, onClick = { selectedItem = item })
                            }
                        }
                        item {
                            UnavailableState(
                                icon = Icons.Rounded.LibraryBooks,
                                title = "Installed library history is unavailable",
                                body = "Install history, cross-device library recovery, and previously owned state remain disabled until the authoritative delivery and Everkeep-backed library contracts are connected.",
                                onDetails = { showPlatformStatus = true },
                            )
                        }
                    }
                }

                if (
                    selectedTab == StoreTab.DISCOVER ||
                    selectedTab == StoreTab.APPS ||
                    selectedTab == StoreTab.SERVICES
                ) {
                    if (visible.isEmpty()) {
                        item {
                            EmptyCatalogState(
                                authenticated = session.isAuthenticated,
                                hasQuery = query.isNotBlank(),
                            )
                        }
                    } else {
                        items(visible, key = { it.id }) { item ->
                            StoreItemCard(item = item, onClick = { selectedItem = item })
                        }
                    }
                }

                item { Spacer(Modifier.height(8.dp)) }
            }
        }

        if (confirmClearSaved) {
            AlertDialog(
                onDismissRequest = { confirmClearSaved = false },
                title = { Text("Clear saved items?") },
                text = {
                    Text(
                        "This removes only this development identity’s device-local Save for later list. " +
                            "It does not uninstall apps, change entitlements, or affect account-wide history.",
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            savedItemIds = savedCatalogStore.clear(session.subjectId)
                            confirmClearSaved = false
                        },
                    ) {
                        Text("Clear saved")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { confirmClearSaved = false }) {
                        Text("Cancel")
                    }
                },
            )
        }

        selectedItem?.let { item ->
            StoreItemSheet(
                item = item,
                isFavorite = item.id in favoriteItemIds,
                onFavoriteChanged = { favorite ->
                    favoriteItemIds = favoriteCatalogStore.setFavorite(
                        subjectId = session.subjectId,
                        itemId = item.id,
                        favorite = favorite,
                    )
                },
                isSaved = item.id in savedItemIds,
                onSavedChanged = { saved ->
                    savedItemIds = savedCatalogStore.setSaved(
                        subjectId = session.subjectId,
                        itemId = item.id,
                        saved = saved,
                    )
                },
                onDismiss = { selectedItem = null },
            )
        }

        if (showPlatformStatus) {
            PlatformStatusSheet(onDismiss = { showPlatformStatus = false })
        }
    }
}

@Composable
private fun StoreTopBar(
    session: IdentitySession,
    sessions: List<IdentitySession>,
    onSessionSelected: (IdentitySession) -> Unit,
    onShowGuidanceSettings: () -> Unit,
    onShowPlatformStatus: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(tonalElevation = 2.dp, color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        "GoreeCloud",
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                    )
                    Surface(
                        shape = GlazeCapsuleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Text(
                            "Development",
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1,
                        )
                    }
                }
                Text(
                    "App Store",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }

            Box(
                modifier = Modifier.widthIn(min = 138.dp, max = 172.dp),
            ) {
                TextButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    onClick = { expanded = true },
                ) {
                    Icon(Icons.Rounded.AccountCircle, contentDescription = null)
                    Spacer(Modifier.size(5.dp))
                    Text(
                        session.displayName,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Icon(Icons.Rounded.ExpandMore, contentDescription = null)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    sessions.forEach { candidate ->
                        DropdownMenuItem(
                            text = { Text(candidate.displayName) },
                            onClick = {
                                onSessionSelected(candidate)
                                expanded = false
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Guidance & setup") },
                        leadingIcon = { Icon(Icons.Rounded.Info, contentDescription = null) },
                        onClick = {
                            expanded = false
                            onShowGuidanceSettings()
                        },
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Development status") },
                        leadingIcon = { Icon(Icons.Rounded.Info, contentDescription = null) },
                        onClick = {
                            expanded = false
                            onShowPlatformStatus()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DevelopmentStatusStrip(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = GlazeSmallCardShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    "Simulated accounts are active",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    "Tap for platform and integration status.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun StoreHero(visibleCount: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = GlazeCardShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = GlazeSmallCardShape,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(R.drawable.goreecloud_app_store_icon),
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(GlazeArtworkShape),
                    )
                }
            }
            Text(
                "Your GoreeCloud, in one place.",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                "Discover applications and services that this development identity is authorized to see.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Surface(shape = GlazeCapsuleShape, color = MaterialTheme.colorScheme.surface) {
                Text(
                    "$visibleCount available",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun TabIntro(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StoreSearch(query: String, onQueryChanged: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = GlazeCapsuleShape,
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        placeholder = {
            Text(
                "Search your available catalog",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}

@Composable
private fun StoreSectionHeading(title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StoreItemCard(item: StoreItem, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = GlazeCardShape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StoreArtwork(item = item, size = 64.dp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    item.summary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${item.type.label()} · ${item.category}",
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    ReleaseChannelPill(item.releaseChannel)
                }
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = "View ${item.name}",
            )
        }
    }
}

@Composable
private fun ReleaseChannelPill(channel: ReleaseChannel) {
    Surface(
        shape = GlazeCapsuleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            channel.label(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}

@Composable
private fun EmptyCatalogState(authenticated: Boolean, hasQuery: Boolean) {
    val title = when {
        hasQuery -> "No matching results"
        authenticated -> "Nothing is available here"
        else -> "Sign in to see your catalog"
    }
    val body = when {
        hasQuery -> "Try a different search term within the catalog available to this identity."
        authenticated -> "This development identity has no matching entries in the current section."
        else -> "Production sign-in will be provided by GoreeCloud Identity."
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = GlazeCardShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FavoriteLibraryEmptyState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = GlazeCardShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Rounded.FavoriteBorder,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                "No Favorites yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                "Open an entitled app or service and choose Add to Favorites. Favorites remain device-local and separated by development identity.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SavedLibraryEmptyState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = GlazeCardShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                Icons.Rounded.BookmarkBorder,
                contentDescription = null,
                modifier = Modifier.size(34.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                "Nothing saved for later",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                "Open an entitled app or service and choose Save for later. Saves stay only on this device and are separated by the active development identity.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun UnavailableState(
    icon: ImageVector,
    title: String,
    body: String,
    onDetails: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = GlazeCardShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                modifier = Modifier.size(68.dp),
                shape = GlazeSmallCardShape,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(34.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onDetails) {
                Text("Development status")
            }
        }
    }
}

@Composable
private fun StoreNavigation(selected: StoreTab, onSelected: (StoreTab) -> Unit) {
    NavigationBar(
        modifier = Modifier.navigationBarsPadding(),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
    ) {
        StoreTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = selected == tab,
                onClick = { onSelected(tab) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(tab.title, maxLines = 1) },
            )
        }
    }
}

@Composable
private fun StoreItemSheet(
    item: StoreItem,
    isFavorite: Boolean,
    onFavoriteChanged: (Boolean) -> Unit,
    isSaved: Boolean,
    onSavedChanged: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StoreArtwork(item = item, size = 80.dp)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        item.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "${item.type.label()} · ${item.category}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    ReleaseChannelPill(item.releaseChannel)
                }
            }

            Text(item.summary, style = MaterialTheme.typography.bodyLarge)

            Surface(
                shape = GlazeSmallCardShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item.version?.let { MetadataLine(label = "Version", value = it) }
                    MetadataLine(label = "Channel", value = item.releaseChannel.label())
                    MetadataLine(
                        label = "Access",
                        value = "Available to this development identity",
                    )
                }
            }

            TextButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                onClick = { onFavoriteChanged(!isFavorite) },
            ) {
                Icon(
                    if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = null,
                )
                Spacer(Modifier.size(8.dp))
                Text(if (isFavorite) "Remove from Favorites" else "Add to Favorites")
            }

            Text(
                "Favorites stay on this device for the active development identity and do not represent install ownership, account history, or Everkeep recovery.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            TextButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                onClick = { onSavedChanged(!isSaved) },
            ) {
                Icon(
                    if (isSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                    contentDescription = null,
                )
                Spacer(Modifier.size(8.dp))
                Text(if (isSaved) "Remove from saved" else "Save for later")
            }

            Text(
                if (isSaved) {
                    "Saved for this development identity on this device only."
                } else {
                    "Save-for-later state is device-local and does not represent install ownership, account library history, or Everkeep recovery."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val actionAvailable =
                item.type == StoreItemType.APPLICATION && UnavailablePackageDeliveryGateway.isAvailable
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = actionAvailable,
                onClick = {},
            ) {
                Text(
                    if (item.type == StoreItemType.APPLICATION) {
                        if (actionAvailable) "Install" else "Install unavailable"
                    } else {
                        "Open unavailable"
                    },
                )
            }

            Text(
                if (item.type == StoreItemType.APPLICATION) {
                    "Installation remains disabled until approved release metadata, artifact provenance, Wardveil verification, and package delivery are connected."
                } else {
                    "Service launch remains disabled until production GoreeCloud Identity authorization and approved endpoint policy are connected."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun MetadataLine(label: String, value: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun PlatformStatusSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Development status",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "These diagnostics describe current implementation boundaries. They are not production trust or acceptance badges.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            PlatformIntegrationRegistry.current.forEachIndexed { index, integration ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            integration.system,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Surface(
                            shape = GlazeCapsuleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Text(
                                integration.state.label(),
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                            )
                        }
                    }
                    Text(
                        integration.detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (index != PlatformIntegrationRegistry.current.lastIndex) {
                    HorizontalDivider()
                }
            }

            Surface(
                shape = GlazeSmallCardShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    "Production acceptance remains false for this App Store build.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun StoreArtwork(item: StoreItem, size: Dp) {
    val resource = item.artworkResource()
    if (resource != null) {
        Image(
            painter = painterResource(resource),
            contentDescription = item.name,
            modifier = Modifier
                .size(size)
                .clip(GlazeArtworkShape),
        )
    } else {
        Surface(
            modifier = Modifier.size(size),
            shape = GlazeArtworkShape,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (item.type == StoreItemType.APPLICATION) {
                        Icons.Rounded.Apps
                    } else {
                        Icons.Rounded.Cloud
                    },
                    contentDescription = item.name,
                    modifier = Modifier.size(size * 0.46f),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private fun catalogCountLabel(count: Int): String = when (count) {
    1 -> "1 item in this development catalog"
    else -> "$count items in this development catalog"
}

private fun StoreItem.artworkResource(): Int? = when (id) {
    "goreecloud.browser" -> R.drawable.goreecloud_browser_icon
    "goreecloud.messenger" -> R.drawable.goreecloud_messenger_icon
    "goreecloud.location" -> R.drawable.goreecloud_location_icon
    "goreecloud.manager" -> R.drawable.goreecloud_manager_icon
    "goreecloud.identity-center" -> R.drawable.goreecloud_identity_center_icon
    "goreecloud.mesh-center" -> R.drawable.goreecloud_mesh_center_icon
    else -> null
}

private fun StoreItemType.label(): String = when (this) {
    StoreItemType.APPLICATION -> "Application"
    StoreItemType.SERVICE -> "Service"
}

private fun ReleaseChannel.label(): String = when (this) {
    ReleaseChannel.STABLE -> "Stable"
    ReleaseChannel.RC -> "RC"
    ReleaseChannel.BETA -> "Beta"
    ReleaseChannel.DEVELOPMENT -> "Development"
    ReleaseChannel.DEBUG -> "Debug"
}

private fun IntegrationState.label(): String = when (this) {
    IntegrationState.TARGETED -> "Targeted"
    IntegrationState.SOURCE_BOUNDARY -> "Boundary ready"
    IntegrationState.NOT_CONNECTED -> "Not connected"
}
