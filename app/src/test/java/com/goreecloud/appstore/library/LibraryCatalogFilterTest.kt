package com.goreecloud.appstore.library

import com.goreecloud.appstore.domain.AccessRule
import com.goreecloud.appstore.domain.EntitlementEngine
import com.goreecloud.appstore.domain.IdentitySession
import com.goreecloud.appstore.domain.ReleaseChannel
import com.goreecloud.appstore.domain.StoreItem
import com.goreecloud.appstore.domain.StoreItemType
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryCatalogFilterTest {
    private val app = item("app.notes", "GoreeCloud Notes", StoreItemType.APPLICATION, "Productivity")
    private val service = item("service.search", "GoreeCloud Search", StoreItemType.SERVICE, "Services")
    private val restricted = item(
        "restricted",
        "Private Dev Tool",
        StoreItemType.APPLICATION,
        "Developer",
        AccessRule(anyAudience = setOf("audience:developer")),
    )

    @Test
    fun blankQueryAndAllTypePreserveAlreadyEntitledInputOrder() {
        assertEquals(
            listOf(app, service),
            LibraryCatalogFilter.apply(listOf(app, service), "  ", LibraryItemTypeFilter.ALL),
        )
    }

    @Test
    fun typeChipFiltersBothApplicationAndServiceCollections() {
        val entries = listOf(service, app)
        assertEquals(
            listOf(app),
            LibraryCatalogFilter.apply(entries, "", LibraryItemTypeFilter.APPS),
        )
        assertEquals(
            listOf(service),
            LibraryCatalogFilter.apply(entries, "", LibraryItemTypeFilter.SERVICES),
        )
    }

    @Test
    fun normalizedMultiTermSearchAndTypeFilterCompose() {
        val accented = app.copy(name = "GoreeCloud Café", summary = "Private writing")
        assertEquals(
            listOf(accented),
            LibraryCatalogFilter.apply(
                listOf(service, accented),
                "CAFE private",
                LibraryItemTypeFilter.APPS,
            ),
        )
        assertEquals(
            emptyList<StoreItem>(),
            LibraryCatalogFilter.apply(
                listOf(service, accented),
                "cafe private",
                LibraryItemTypeFilter.SERVICES,
            ),
        )
    }

    @Test
    fun filterNeverRecoversUnentitledItemsExcludedByCaller() {
        val standard = IdentitySession(
            subjectId = "dev:standard",
            displayName = "Standard",
            audiences = setOf("audience:standard"),
            isAuthenticated = true,
        )
        val entitled = EntitlementEngine.visibleItems(standard, listOf(app, restricted))
        assertEquals(
            emptyList<StoreItem>(),
            LibraryCatalogFilter.apply(entitled, "private dev tool", LibraryItemTypeFilter.ALL),
        )
    }

    private fun item(
        id: String,
        name: String,
        type: StoreItemType,
        category: String,
        accessRule: AccessRule = AccessRule(),
    ) = StoreItem(
        id = id,
        name = name,
        summary = "Private writing and authorized sources",
        type = type,
        category = category,
        version = null,
        releaseChannel = ReleaseChannel.DEVELOPMENT,
        packageName = null,
        serviceUrl = null,
        accessRule = accessRule,
    )
}
