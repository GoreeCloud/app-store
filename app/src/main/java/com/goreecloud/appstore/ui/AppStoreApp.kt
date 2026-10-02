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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material3.FilterChip
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
    var confirmClearFavorites by remember(session.subjectId) { mutableStateOf(false) }
    var confirmClearSaved by remember(session.subjectId) { mutableStateOf(false) }
    var selectedCategory by remember(session.subjectId, selectedTab) {
        mutableStateOf<String?>(null)
    }

    val entitled = remember(session, allItems) {
        EntitlementEngine.visibleItems(session, allItems)
    }
    val savedVisible = remember(entitled, savedItemIds) {
        entitled.filter { it.id in savedItemIds }
    }
    val favoriteVisible = remember(entitled, favoriteItemIds) {
        entitled.filter { it.id in favoriteItemIds }
    }
    val tabItems = remember(entitled, selectedTab) {
        when (selectedTab) {
            StoreTab.DISCOVER -> entitled
            StoreTab.APPS -> entitled.filter { it.type == StoreItemType.APPLICATION }
            StoreTab.SERVICES -> entitled.filter { it.type == StoreItemType.SERVICE }
            StoreTab.UPDATES, StoreTab.LIBRARY -> emptyList()
        }
    }
    val categories = remember(tabItems) {
        tabItems.map { it.category }.distinct().sorted()
    }
    val visible = remember(tabItems, query, selectedCategory) {
        tabItems.filter { item ->
            val matchesCategory = selectedCategory == null || item.category == selectedCategory
            val matchesQuery = query.isBlank() ||
                item.name.contains(query, ignoreCase = true) ||
                item.summary.contains(query, ignoreCase = true) ||
                item.category.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }
    val appCount = remember(entitled) {
        entitled.count { it.type == StoreItemType.APPLICATION }
    }
    val serviceCount = remember(entitled) {
        entitled.count { it.type == StoreItemType.SERVICE }
    }

    LaunchedEffect(selectedTab, session.subjectId) {
        query = ""
        selectedCategory = null
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
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (selectedTab) {
                    StoreTab.DISCOVER -> {
                        item {
                            StoreHero(
                                visibleCount = entitled.size,
                                appCount = appCount,
                                serviceCount = serviceCount,
                            )
                        }
                        item { StoreSearch(query = query, onQueryChanged = { query = it }) }
                        if (categories.isNotEmpty()) {
                            item {
                                CategoryStrip(
                                    categories = categories,
                                    selected = selectedCategory,
                                    onSelected = { selectedCategory = it },
                                )
                            }
                        }
                        item { DevelopmentStatusStrip(onClick = { showPlatformStatus = true }) }
                        if (guidanceState.isHintVisible(APP_STORE_CATALOG_HINT_ID)) {
                            item {
                                AppStoreCatalogGuidanceHint(
                                    onDismiss = {
                                        onDismissGuidanceHint(APP_STORE_CATALOG_HINT_ID)
                                    },
                                )
                            }
                        }
                        if (visible.isNotEmpty()) {
                            item {
                                StoreSectionHeading(
                                    title = "Featured",
                                    subtitle = "A quick look at what is available to this identity",
                                )
                            }
                            item {
                                FeaturedShelf(
                                    items = visible.take(6),
                                    onItemClick = { selectedItem = it },
                                )
                            }
                        }
                        item {
                            StoreSectionHeading(
                                title = "All available",
                                subtitle = catalogCountLabel(visible.size),
                            )
                        }
                    }

                    StoreTab.APPS -> {
                        item {
                            TabIntro(
                                title = "Apps",
                                body = "$appCount available to this identity",
                            )
                        }
                        item { StoreSearch(query = query, onQueryChanged = { query = it }) }
                        if (categories.isNotEmpty()) {
                            item {
                                CategoryStrip(
                                    categories = categories,
                                    selected = selectedCategory,
                                    onSelected = { selectedCategory = it },
                                )
                            }
                        }
                    }

                    StoreTab.SERVICES -> {
                        item {
                            TabIntro(
                                title = "Services",
                                body = "$serviceCount available to this identity",
                            )
                        }
                        item { StoreSearch(query = query, onQueryChanged = { query = it }) }
                        if (categories.isNotEmpty()) {
                            item {
                                CategoryStrip(
                                    categories = categories,
                                    selected = selectedCategory,
                                    onSelected = { selectedCategory = it },
                                )
                            }
                        }
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
                            item {
                                TextButton(
                                    modifier = Modifier.heightIn(min = 48.dp),
                                    onClick = { confirmClearFavorites = true },
                                ) {
                                    Text("Clear Favorites")
                                }
                            }
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

        if (confirmClearFavorites) {
            AlertDialog(
                onDismissRequest = { confirmClearFavorites = false },
                title = { Text("Clear Favorites?") },
                text = {
                    Text(
                        "This removes only this development identity’s device-local Favorites list. " +
                            "It does not uninstall apps, change entitlements, or affect account-wide history.",
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            favoriteItemIds = favoriteCatalogStore.clear(session.subjectId)
                            confirmClearFavorites = false
                        },
                    ) {
                        Text("Clear all Favorites")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { confirmClearFavorites = false }) {
                        Text("Cancel")
                    }
                },
            )
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

    Surface(
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.goreecloud_app_store_icon),
                contentDescription = null,
                modifier = Modifier
                    .size(42.dp)
                    .clip(GlazeArtworkShape),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    "App Store",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Text(
                    "GoreeCloud · Development",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }

            Box(modifier = Modifier.widthIn(min = 104.dp, max = 136.dp)) {
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
                        session.displayName.removeSuffix(" demo"),
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
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Guidance & setup") },
                        leadingIcon = { Icon(Icons.Rounded.Info, contentDescription = null) },
                        onClick = {
                            expanded = false
                            onShowGuidanceSettings()
                        },
                    )
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
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick),
        shape = GlazeCapsuleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.Info,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                "Simulated account · Platform status",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun StoreHero(
    visibleCount: Int,
    appCount: Int,
    serviceCount: Int,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = GlazeCardShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    "Explore GoreeCloud",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    "Apps and services available to this identity.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                CatalogStatChip("$visibleCount total")
                CatalogStatChip("$appCount apps")
                CatalogStatChip("$serviceCount services")
            }
        }
    }
}

@Composable
private fun CatalogStatChip(label: String) {
    Surface(shape = GlazeCapsuleShape, color = MaterialTheme.colorScheme.surface) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
        )
    }
}

@Composable
private fun TabIntro(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StoreSearch(query: String, onQueryChanged: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        singleLine = true,
        shape = GlazeCapsuleShape,
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        placeholder = {
            Text(
                "Search apps and services",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}


@Composable
private fun CategoryStrip(
    categories: List<String>,
    selected: String?,
    onSelected: (String?) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(end = 4.dp),
    ) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onSelected(null) },
                label = { Text("All") },
            )
        }
        items(categories, key = { "category:$it" }) { category ->
            FilterChip(
                selected = selected == category,
                onClick = { onSelected(if (selected == category) null else category) },
                label = { Text(category, maxLines = 1) },
            )
        }
    }
}

@Composable
private fun FeaturedShelf(
    items: List<StoreItem>,
    onItemClick: (StoreItem) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(end = 8.dp),
    ) {
        items(items, key = { "featured:${it.id}" }) { item ->
            FeaturedItemCard(item = item, onClick = { onItemClick(item) })
        }
    }
}

@Composable
private fun FeaturedItemCard(item: StoreItem, onClick: () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .width(180.dp)
            .clickable(onClick = onClick),
        shape = GlazeCardShape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StoreArtwork(item = item, size = 56.dp)
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    item.category,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
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
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StoreArtwork(item = item, size = 56.dp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
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
                    if (item.releaseChannel != ReleaseChannel.DEVELOPMENT) {
                        ReleaseChannelPill(item.releaseChannel)
                    }
                }
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = "View ${item.name}",
                modifier = Modifier.size(22.dp),
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
    LibraryCollectionEmptyState(
        icon = Icons.Rounded.FavoriteBorder,
        title = "No Favorites yet",
        body = "Open any available item and choose Add to Favorites.",
    )
}

@Composable
private fun SavedLibraryEmptyState() {
    LibraryCollectionEmptyState(
        icon = Icons.Rounded.BookmarkBorder,
        title = "Nothing saved for later",
        body = "Open any available item and choose Save for later.",
    )
}

@Composable
private fun LibraryCollectionEmptyState(
    icon: ImageVector,
    title: String,
    body: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = GlazeSmallCardShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(30.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = GlazeSmallCardShape,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = onDetails,
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp),
                ) {
                    Text("Development status")
                }
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
    Image(
        painter = painterResource(item.artworkResource()),
        contentDescription = item.name,
        modifier = Modifier
            .size(size)
            .clip(GlazeArtworkShape),
    )
}

private fun catalogCountLabel(count: Int): String = when (count) {
    1 -> "1 item available"
    else -> "$count items available"
}

private fun StoreItem.artworkResource(): Int = when (id) {
    "goreecloud.app-store" -> R.drawable.goreecloud_app_store_icon
    "goreecloud.launcher" -> R.drawable.goreecloud_launcher_icon
    "goreecloud.file-manager" -> R.drawable.goreecloud_file_manager_icon
    "goreecloud.dialer" -> R.drawable.goreecloud_dialer_icon
    "goreecloud.camera" -> R.drawable.goreecloud_camera_icon
    "goreecloud.messenger" -> R.drawable.goreecloud_messenger_icon
    "goreecloud.mail" -> R.drawable.goreecloud_mail_icon
    "goreecloud.browser" -> R.drawable.goreecloud_browser_icon
    "goreecloud.keyboard" -> R.drawable.goreecloud_keyboard_icon
    "goreecloud.memos" -> R.drawable.goreecloud_memos_icon
    "goreecloud.notes" -> R.drawable.goreecloud_notes_icon
    "goreecloud.tasks" -> R.drawable.goreecloud_tasks_icon
    "goreecloud.calendar" -> R.drawable.goreecloud_calendar_icon
    "goreecloud.contacts" -> R.drawable.goreecloud_contacts_icon
    "goreecloud.gallery" -> R.drawable.goreecloud_gallery_icon
    "goreecloud.since" -> R.drawable.goreecloud_since_icon
    "goreecloud.music" -> R.drawable.goreecloud_music_icon
    "goreecloud.bookmarks" -> R.drawable.goreecloud_bookmarks_icon
    "goreecloud.search" -> R.drawable.goreecloud_search_icon
    "goreecloud.photos" -> R.drawable.goreecloud_photos_icon
    "goreecloud.location" -> R.drawable.goreecloud_location_icon
    "goreecloud.feed" -> R.drawable.goreecloud_feed_icon
    "goreecloud.video" -> R.drawable.goreecloud_video_icon
    "goreecloud.changelogs" -> R.drawable.goreecloud_changelogs_icon
    "goreecloud.pdf-manager" -> R.drawable.goreecloud_pdf_manager_icon
    "goreecloud.manager" -> R.drawable.goreecloud_manager_icon
    "goreecloud.monitor" -> R.drawable.goreecloud_monitor_icon
    "goreecloud.terminal" -> R.drawable.goreecloud_terminal_icon
    "goreecloud.github-dashboard" -> R.drawable.goreecloud_github_dashboard_icon
    "goreecloud.identity-center" -> R.drawable.goreecloud_identity_center_icon
    "goreecloud.mesh-center" -> R.drawable.goreecloud_mesh_center_icon
    "goreecloud.sync" -> R.drawable.goreecloud_sync_icon
    "goreecloud.notify" -> R.drawable.goreecloud_notify_icon
    "goreecloud.network" -> R.drawable.goreecloud_network_icon
    else -> error("Missing official catalog artwork mapping for $id")
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
