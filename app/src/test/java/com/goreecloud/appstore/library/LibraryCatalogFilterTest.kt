package com.goreecloud.appstore.library

import com.goreecloud.appstore.domain.AccessRule
import com.goreecloud.appstore.domain.ReleaseChannel
import com.goreecloud.appstore.domain.StoreItem
import com.goreecloud.appstore.domain.StoreItemType
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryCatalogFilterTest {
    private val app = StoreItem(
        id = "app.notes",
        name = "GoreeCloud Notes",
        summary = "Private notes and writing",
        type = StoreItemType.APPLICATION,
        category = "Productivity",
        version = "1.0",
        releaseChannel = ReleaseChannel.DEVELOPMENT,
        packageName = "com.goreecloud.notes",
        serviceUrl = null,
        accessRule = AccessRule(),
    )
    private val service = StoreItem(
        id = "service.search",
        name = "GoreeCloud Search",
        summary = "Search your authorized GoreeCloud sources",
        type = StoreItemType.SERVICE,
        category = "Services",
        version = null,
        releaseChannel = ReleaseChannel.DEVELOPMENT,
        packageName = null,
        serviceUrl = "https://search.goreecloud.test",
        accessRule = AccessRule(),
    )

    @Test
    fun blankQueryPreservesInputOrder() {
        assertEquals(
            listOf(app, service),
            LibraryCatalogFilter.apply(listOf(app, service), "   ", LibraryItemTypeFilter.ALL),
        )
    }

    @Test
    fun queryMatchesNameSummaryAndCategoryCaseInsensitively() {
        assertEquals(listOf(app), LibraryCatalogFilter.apply(listOf(app, service), "notes", LibraryItemTypeFilter.ALL))
        assertEquals(listOf(service), LibraryCatalogFilter.apply(listOf(app, service), "AUTHORIZED", LibraryItemTypeFilter.ALL))
        assertEquals(listOf(app), LibraryCatalogFilter.apply(listOf(app, service), "productivity", LibraryItemTypeFilter.ALL))
    }

    @Test
    fun typeFilterRestrictsAlreadyAuthorizedCollectionOnly() {
        assertEquals(listOf(app), LibraryCatalogFilter.apply(listOf(app, service), "", LibraryItemTypeFilter.APPS))
        assertEquals(listOf(service), LibraryCatalogFilter.apply(listOf(app, service), "", LibraryItemTypeFilter.SERVICES))
    }
}
