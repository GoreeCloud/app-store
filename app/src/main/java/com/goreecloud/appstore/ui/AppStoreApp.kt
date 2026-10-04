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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sort
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
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.goreecloud.appstore.R
import com.goreecloud.appstore.data.CatalogJsonLoader
import com.goreecloud.appstore.domain.CatalogPresentation
import com.goreecloud.appstore.domain.CatalogSort
import com.goreecloud.appstore.domain.EntitlementEngine
import com.goreecloud.appstore.domain.IdentitySession
import com.goreecloud.appstore.domain.ReleaseChannel
import com.goreecloud.appstore.domain.StoreItem
import com.goreecloud.appstore.domain.StoreItemType
import com.goreecloud.appstore.identity.DevelopmentIdentityGateway
import com.goreecloud.appstore.library.FavoriteCatalogStore
import com.goreecloud.appstore.library.RecentlyViewedCatalogSelection
import com.goreecloud.appstore.library.SavedCatalogStore
import com.goreecloud.appstore.onboarding.AppStoreGuidanceState
import com.goreecloud.appstore.platform.IntegrationState
import com.goreecloud.appstore.platform.PlatformIntegrationRegistry

enum class StoreTab(val title: String, val icon: ImageVector) {
    DISCOVER("Discover", Icons.Rounded.Home),
    APPS("Apps", Icons.Rounded.Apps),
    SERVICES("Services", Icons.Rounded.Cloud),
    UPDATES("Updates", Icons.Rounded.Update),
    LIBRARY("Library", Icons.AutoMirrored.Rounded.LibraryBooks),
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
    var confirmClearRecentlyViewed by remember(session.subjectId) { mutableStateOf(false) }
    var recentlyViewedByIdentity by remember {
        mutableStateOf<Map<String, List<String>>>(emptyMap())
    }
    var libraryQuery by remember(session.subjectId) { mutableStateOf("") }
    var catalogSortByTab by remember(session.subjectId) {
        mutableStateOf<Map<StoreTab, CatalogSort>>(emptyMap())
    }
    val catalogSort = catalogSortByTab[selectedTab] ?: CatalogSort.CATALOG_ORDER
    val setCatalogSort: (CatalogSort) -> Unit = { sort ->
        catalogSortByTab = catalogSortByTab + (selectedTab to sort)
    }
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
    val entitledById = remember(entitled) { entitled.associateBy { it.id } }
    val recentlyViewedIds = recentlyViewedByIdentity[session.subjectId].orEmpty()
    val recentlyViewedVisible = remember(entitledById, recentlyViewedIds) {
        recentlyViewedIds.mapNotNull { entitledById[it] }
    }
    val favoriteLibraryVisible = remember(favoriteVisible, libraryQuery) {
        favoriteVisible.filter { it.matchesLibraryQuery(libraryQuery) }
    }
    val savedLibraryVisible = remember(savedVisible, libraryQuery) {
        savedVisible.filter { it.matchesLibraryQuery(libraryQuery) }
    }
    val recentLibraryVisible = remember(recentlyViewedVisible, libraryQuery) {
        recentlyViewedVisible.filter { it.matchesLibraryQuery(libraryQuery) }
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
    val filtered = remember(tabItems, query, selectedCategory) {
        CatalogPresentation.filterAndSort(
            items = tabItems,
            query = query,
            category = selectedCategory,
            sort = CatalogSort.CATALOG_ORDER,
        )
    }
    val visible = remember(tabItems, query, selectedCategory, catalogSort) {
        CatalogPresentation.filterAndSort(
            items = tabItems,
            query = query,
            category = selectedCategory,
            sort = catalogSort,
        )
    }
    val appCount = remember(entitled) {
        entitled.count { it.type == StoreItemType.APPLICATION }
    }
    val serviceCount = remember(entitled) {
        entitled.count { it.type == StoreItemType.SERVICE }
    }
    val openItem: (StoreItem) -> Unit = { item ->
        val subjectId = session.subjectId
        val next = RecentlyViewedCatalogSelection.record(
            current = recentlyViewedByIdentity[subjectId].orEmpty(),
            itemId = item.id,
        )
        recentlyViewedByIdentity = recentlyViewedByIdentity + (subjectId to next)
        selectedItem = item
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
                        item {
                            StoreSearch(
                                query = query,
                                placeholder = "Search apps and services",
                                onQueryChanged = { query = it },
                            )
                        }
                        if (categories.isNotEmpty()) {
                            item {
                                CategoryStrip(
                                    categories = categories,
                                    selected = selectedCategory,
                                    onSelected = { selectedCategory = it },
                                )
                            }
                        }
                        if (guidanceState.isHintVisible(APP_STORE_CATALOG_HINT_ID)) {
                            item {
                                AppStoreCatalogGuidanceHint(
                                    onDismiss = {
                                        onDismissGuidanceHint(APP_STORE_CATALOG_HINT_ID)
                                    },
                                )
                            }
                        }
                        if (
                            query.isBlank() &&
                            selectedCategory == null &&
                            recentlyViewedVisible.isNotEmpty()
                        ) {
                            item {
                                StoreSectionHeading(
                                    title = "Continue browsing",
                                    subtitle = "Recently opened this session",
                                )
                            }
                            item {
                                FeaturedShelf(
                                    items = recentlyViewedVisible.take(6),
                                    onItemClick = openItem,
                                )
                            }
                        }
                        if (visible.isNotEmpty()) {
                            item {
                                StoreSectionHeading(
                                    title = "Featured",
                                    subtitle = "Quick access to available items",
                                )
                            }
                            item {
                                FeaturedShelf(
                                    items = filtered.take(6),
                                    onItemClick = openItem,
                                )
                            }
                        }
                        item {
                            StoreSectionHeading(
                                title = "Browse all",
                                subtitle = catalogCountLabel(visible.size),
                                sort = catalogSort,
                                onSortChanged = setCatalogSort,
                            )
                        }
                    }

                    StoreTab.APPS -> {
                        item {
                            TabIntro(
                                title = "Apps",
                                body = catalogCountLabel(visible.size),
                                sort = catalogSort,
                                onSortChanged = setCatalogSort,
                            )
                        }
                        item {
                            StoreSearch(
                                query = query,
                                placeholder = "Search apps",
                                onQueryChanged = { query = it },
                            )
                        }
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
                                body = catalogCountLabel(visible.size),
                                sort = catalogSort,
                                onSortChanged = setCatalogSort,
                            )
                        }
                        item {
                            StoreSearch(
                                query = query,
                                placeholder = "Search services",
                                onQueryChanged = { query = it },
                            )
                        }
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
                            Text(
                                "Updates",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        item {
                            Box(
                                modifier = Modifier
                                    .fillParentMaxHeight(0.55f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center,
                            ) {
                                UnavailableState(
                                    icon = Icons.Rounded.Update,
                                    title = "Release delivery not connected",
                                    body = "Available app updates will appear here after authenticated release metadata and package delivery are connected.",
                                    onDetails = { showPlatformStatus = true },
                                )
                            }
                        }
                    }

                    StoreTab.LIBRARY -> {
                        item {
                            TabIntro(
                                title = "Library",
                                body = "Manage device-local collections and this session’s recently opened items for the active identity.",
                            )
                        }

                        val hasLibraryItems =
                            favoriteVisible.isNotEmpty() ||
                                savedVisible.isNotEmpty() ||
                                recentlyViewedVisible.isNotEmpty()

                        if (hasLibraryItems) {
                            item {
                                LibraryCollectionSummary(
                                    favorites = favoriteVisible.size,
                                    saved = savedVisible.size,
                                    recent = recentlyViewedVisible.size,
                                )
                            }
                            item {
                                StoreSearch(
                                    query = libraryQuery,
                                    placeholder = "Search your library",
                                    onQueryChanged = { libraryQuery = it },
                                )
                            }
                        }

                        val searchingLibrary = libraryQuery.isNotBlank()
                        val hasLibraryMatches =
                            favoriteLibraryVisible.isNotEmpty() ||
                                savedLibraryVisible.isNotEmpty() ||
                                recentLibraryVisible.isNotEmpty()

                        if (searchingLibrary && !hasLibraryMatches) {
                            item {
                                EmptyLibrarySearchState(
                                    onReset = { libraryQuery = "" },
                                )
                            }
                        } else {
                            if (recentLibraryVisible.isNotEmpty()) {
                                item {
                                    LibrarySectionHeading(
                                        title = "Recently opened",
                                        subtitle = libraryCollectionCountLabel(
                                            recentLibraryVisible.size,
                                            singular = "item this session",
                                            plural = "items this session",
                                        ),
                                        actionLabel = if (!searchingLibrary) "Clear" else null,
                                        onAction = { confirmClearRecentlyViewed = true },
                                    )
                                }
                                item {
                                    FeaturedShelf(
                                        items = recentLibraryVisible,
                                        onItemClick = openItem,
                                    )
                                }
                                item { Spacer(Modifier.height(4.dp)) }
                            }

                            if (!searchingLibrary || favoriteLibraryVisible.isNotEmpty()) {
                                item {
                                    LibrarySectionHeading(
                                        title = "Favorites",
                                        subtitle = libraryCollectionCountLabel(
                                            favoriteLibraryVisible.size,
                                            singular = "favorite",
                                            plural = "favorites",
                                        ),
                                        actionLabel = if (
                                            !searchingLibrary && favoriteVisible.isNotEmpty()
                                        ) {
                                            "Clear"
                                        } else {
                                            null
                                        },
                                        onAction = { confirmClearFavorites = true },
                                    )
                                }
                                if (favoriteLibraryVisible.isEmpty()) {
                                    item { FavoriteLibraryEmptyState() }
                                } else {
                                    items(
                                        favoriteLibraryVisible,
                                        key = { "favorite:${it.id}" },
                                    ) { item ->
                                        StoreItemCard(
                                            item = item,
                                            isFavorite = true,
                                            isSaved = item.id in savedItemIds,
                                            onClick = { openItem(item) },
                                        )
                                    }
                                }
                                item { Spacer(Modifier.height(4.dp)) }
                            }

                            if (!searchingLibrary || savedLibraryVisible.isNotEmpty()) {
                                item {
                                    LibrarySectionHeading(
                                        title = "Saved for later",
                                        subtitle = libraryCollectionCountLabel(
                                            savedLibraryVisible.size,
                                            singular = "saved item",
                                            plural = "saved items",
                                        ),
                                        actionLabel = if (
                                            !searchingLibrary && savedVisible.isNotEmpty()
                                        ) {
                                            "Clear"
                                        } else {
                                            null
                                        },
                                        onAction = { confirmClearSaved = true },
                                    )
                                }
                                if (savedLibraryVisible.isEmpty()) {
                                    item { SavedLibraryEmptyState() }
                                } else {
                                    items(
                                        savedLibraryVisible,
                                        key = { "saved:${it.id}" },
                                    ) { item ->
                                        StoreItemCard(
                                            item = item,
                                            isFavorite = item.id in favoriteItemIds,
                                            isSaved = true,
                                            onClick = { openItem(item) },
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            LibraryHistoryStatusRow(
                                onClick = { showPlatformStatus = true },
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
                                hasFilter = selectedCategory != null,
                                onReset = {
                                    query = ""
                                    selectedCategory = null
                                },
                            )
                        }
                    } else {
                        items(visible, key = { it.id }) { item ->
                            StoreItemCard(
                                item = item,
                                isFavorite = item.id in favoriteItemIds,
                                isSaved = item.id in savedItemIds,
                                onClick = { openItem(item) },
                            )
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

        if (confirmClearRecentlyViewed) {
            AlertDialog(
                onDismissRequest = { confirmClearRecentlyViewed = false },
                title = { Text("Clear recently opened?") },
                text = {
                    Text(
                        "This clears only this session’s recently opened items for the active development identity. " +
                            "Favorites and saved items are not changed.",
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            recentlyViewedByIdentity =
                                recentlyViewedByIdentity - session.subjectId
                            confirmClearRecentlyViewed = false
                        },
                    ) {
                        Text("Clear recent")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { confirmClearRecentlyViewed = false }) {
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
                onShowPlatformStatus = {
                    selectedItem = null
                    showPlatformStatus = true
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.goreecloud_app_store_icon),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
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
                    "Development · ${session.compactDisplayName()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Box(modifier = Modifier.width(64.dp)) {
                TextButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    onClick = { expanded = true },
                ) {
                    Icon(
                        Icons.Rounded.AccountCircle,
                        contentDescription =
                            "Current development identity: ${session.compactDisplayName()}. Open account menu.",
                    )
                    Icon(Icons.Rounded.ExpandMore, contentDescription = null)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    sessions.forEach { candidate ->
                        DropdownMenuItem(
                            text = { Text(candidate.compactDisplayName()) },
                            trailingIcon = {
                                if (candidate.subjectId == session.subjectId) {
                                    Icon(Icons.Rounded.Check, contentDescription = "Active identity")
                                }
                            },
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
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
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
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                item { CatalogStatChip("$visibleCount available") }
                item { CatalogStatChip("$appCount apps") }
                item { CatalogStatChip("$serviceCount services") }
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
private fun TabIntro(
    title: String,
    body: String,
    sort: CatalogSort? = null,
    onSortChanged: (CatalogSort) -> Unit = {},
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
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
        if (sort != null) {
            CatalogSortMenu(
                sort = sort,
                onSortChanged = onSortChanged,
            )
        }
    }
}

@Composable
private fun StoreSearch(
    query: String,
    placeholder: String,
    onQueryChanged: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        singleLine = true,
        shape = GlazeCapsuleShape,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = { focusManager.clearFocus() },
        ),
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChanged("") }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                }
            }
        } else {
            null
        },
        placeholder = {
            Text(
                placeholder,
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
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(end = 12.dp),
    ) {
        item {
            FilterChip(
                modifier = Modifier.heightIn(min = 40.dp),
                selected = selected == null,
                onClick = { onSelected(null) },
                label = { Text("All", style = MaterialTheme.typography.labelMedium) },
            )
        }
        items(categories, key = { "category:$it" }) { category ->
            FilterChip(
                modifier = Modifier.heightIn(min = 40.dp),
                selected = selected == category,
                onClick = { onSelected(if (selected == category) null else category) },
                label = { Text(category, style = MaterialTheme.typography.labelMedium, maxLines = 1) },
                trailingIcon = if (selected == category) {
                    {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                } else {
                    null
                },
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(end = 12.dp),
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
            .width(156.dp)
            .clickable(
                onClickLabel = "View ${item.name}",
                onClick = onClick,
            ),
        shape = GlazeSmallCardShape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            StoreArtwork(item = item, size = 48.dp)
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
private fun StoreSectionHeading(
    title: String,
    subtitle: String,
    sort: CatalogSort? = null,
    onSortChanged: (CatalogSort) -> Unit = {},
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (sort != null) {
            CatalogSortMenu(
                sort = sort,
                onSortChanged = onSortChanged,
            )
        }
    }
}

@Composable
private fun CatalogSortMenu(
    sort: CatalogSort,
    onSortChanged: (CatalogSort) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(
            onClick = { expanded = true },
        ) {
            Icon(
                Icons.Rounded.Sort,
                contentDescription = "Sort catalog. Current: ${sort.label()}",
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            CatalogSort.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label()) },
                    leadingIcon = if (option == sort) {
                        {
                            Icon(
                                Icons.Rounded.Check,
                                contentDescription = null,
                            )
                        }
                    } else {
                        null
                    },
                    onClick = {
                        onSortChanged(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

private fun CatalogSort.label(): String = when (this) {
    CatalogSort.CATALOG_ORDER -> "Catalog order"
    CatalogSort.NAME -> "Name"
    CatalogSort.CATEGORY -> "Category"
}

@Composable
private fun StoreItemCard(
    item: StoreItem,
    isFavorite: Boolean,
    isSaved: Boolean,
    onClick: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = "View ${item.name}",
                onClick = onClick,
            ),
        shape = GlazeSmallCardShape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StoreArtwork(item = item, size = 52.dp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
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
                    maxLines = 1,
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
                    if (isFavorite) {
                        Icon(
                            Icons.Rounded.Favorite,
                            contentDescription = "Favorited",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (isSaved) {
                        Icon(
                            Icons.Rounded.Bookmark,
                            contentDescription = "Saved for later",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
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
private fun EmptyCatalogState(
    authenticated: Boolean,
    hasQuery: Boolean,
    hasFilter: Boolean,
    onReset: () -> Unit,
) {
    val title = when {
        hasQuery || hasFilter -> "No matching results"
        authenticated -> "Nothing is available here"
        else -> "Sign in to see your catalog"
    }
    val body = when {
        hasQuery || hasFilter -> "Try a different search term or category."
        authenticated -> "This identity has no matching entries in this section."
        else -> "Production sign-in will be provided by GoreeCloud Identity."
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = GlazeSmallCardShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
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
            if (hasQuery || hasFilter) {
                TextButton(
                    modifier = Modifier.heightIn(min = 40.dp),
                    onClick = onReset,
                ) {
                    Text("Reset")
                }
            }
        }
    }
}

@Composable
private fun LibraryCollectionSummary(
    favorites: Int,
    saved: Int,
    recent: Int,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(end = 8.dp),
    ) {
        item { LibraryCountChip("$favorites Favorites") }
        item { LibraryCountChip("$saved Saved") }
        item { LibraryCountChip("$recent Recent") }
    }
}

@Composable
private fun LibraryCountChip(label: String) {
    Surface(
        shape = GlazeCapsuleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

private fun StoreItem.matchesLibraryQuery(query: String): Boolean {
    if (query.isBlank()) return true
    return name.contains(query, ignoreCase = true) ||
        summary.contains(query, ignoreCase = true) ||
        category.contains(query, ignoreCase = true) ||
        type.label().contains(query, ignoreCase = true)
}

private fun libraryCollectionCountLabel(
    count: Int,
    singular: String,
    plural: String,
): String = if (count == 1) "1 $singular" else "$count $plural"

@Composable
private fun LibrarySectionHeading(
    title: String,
    subtitle: String,
    actionLabel: String?,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (actionLabel != null) {
            TextButton(
                modifier = Modifier.heightIn(min = 40.dp),
                onClick = onAction,
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun EmptyLibrarySearchState(onReset: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = GlazeSmallCardShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    "No library matches",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Try another search or clear the library search.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(
                modifier = Modifier.heightIn(min = 40.dp),
                onClick = onReset,
            ) {
                Text("Reset")
            }
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
private fun LibraryHistoryStatusRow(
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = "View Development status",
                onClick = onClick,
            ),
        shape = GlazeSmallCardShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.LibraryBooks,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    "Installed history",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Not connected yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = "View Development status",
                modifier = Modifier.size(22.dp),
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onDetails),
        shape = GlazeSmallCardShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = GlazeSmallCardShape,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = "View Development status",
                modifier = Modifier.size(22.dp),
            )
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
    onShowPlatformStatus: () -> Unit,
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
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StoreArtwork(item = item, size = 72.dp)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
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

            Text(item.summary, style = MaterialTheme.typography.bodyMedium)

            Surface(
                shape = GlazeSmallCardShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item.version?.let { MetadataLine(label = "Version", value = it) }
                    MetadataLine(label = "Channel", value = item.releaseChannel.label())
                    MetadataLine(
                        label = "Access",
                        value = "Available to this development identity",
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    onClick = { onFavoriteChanged(!isFavorite) },
                ) {
                    Icon(
                        if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = null,
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(if (isFavorite) "Favorited" else "Favorite")
                }
                TextButton(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                    onClick = { onSavedChanged(!isSaved) },
                ) {
                    Icon(
                        if (isSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        contentDescription = null,
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(if (isSaved) "Saved" else "Save")
                }
            }

            Text(
                "Favorites and saved items stay on this device for the active development identity. They do not represent install ownership or account history.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            ProductAvailabilityCard(
                item = item,
                onClick = onShowPlatformStatus,
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ProductAvailabilityCard(
    item: StoreItem,
    onClick: () -> Unit,
) {
    val isApplication = item.type == StoreItemType.APPLICATION
    val title = if (isApplication) {
        "Installation unavailable"
    } else {
        "Service launch unavailable"
    }
    val body = if (isApplication) {
        "Release metadata, provenance, Wardveil verification, and package delivery are not connected yet."
    } else {
        "Production Identity authorization and approved service endpoint policy are not connected yet."
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = GlazeSmallCardShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = "View Development status",
            )
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
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Development status",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Current integration boundaries for this build.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            PlatformIntegrationRegistry.current.forEach { integration ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = GlazeSmallCardShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                integration.system,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Surface(
                                shape = GlazeCapsuleShape,
                                color = MaterialTheme.colorScheme.surface,
                            ) {
                                Text(
                                    integration.state.label(),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
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
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = GlazeSmallCardShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Production acceptance",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        "Not accepted",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
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

private fun IdentitySession.compactDisplayName(): String = when (displayName) {
    "Standard demo" -> "Standard"
    "Preview tester demo" -> "Preview"
    "Administrator demo" -> "Admin"
    "Developer demo" -> "Developer"
    else -> displayName.removeSuffix(" demo")
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
