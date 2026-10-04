package com.goreecloud.appstore.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogPresentationTest {
    private val items = listOf(
        item("zeta", "Zeta", "Platform"),
        item("alpha", "Alpha", "Communication"),
        item("beta", "Beta", "Communication"),
    )

    @Test
    fun catalogOrderPreservesAuthoritativeInputOrder() {
        assertEquals(
            listOf("zeta", "alpha", "beta"),
            CatalogPresentation.filterAndSort(
                items = items,
                query = "",
                category = null,
                sort = CatalogSort.CATALOG_ORDER,
            ).map { it.id },
        )
    }

    @Test
    fun nameSortIsDeterministic() {
        assertEquals(
            listOf("alpha", "beta", "zeta"),
            CatalogPresentation.filterAndSort(
                items = items,
                query = "",
                category = null,
                sort = CatalogSort.NAME,
            ).map { it.id },
        )
    }

    @Test
    fun categorySortUsesNameAsStableSecondaryKey() {
        assertEquals(
            listOf("alpha", "beta", "zeta"),
            CatalogPresentation.filterAndSort(
                items = items,
                query = "",
                category = null,
                sort = CatalogSort.CATEGORY,
            ).map { it.id },
        )
    }

    @Test
    fun queryAndCategoryAreAppliedBeforeSorting() {
        assertEquals(
            listOf("beta"),
            CatalogPresentation.filterAndSort(
                items = items,
                query = "beta",
                category = "Communication",
                sort = CatalogSort.NAME,
            ).map { it.id },
        )
    }

    private fun item(
        id: String,
        name: String,
        category: String,
    ) = StoreItem(
        id = id,
        name = name,
        summary = "$name summary",
        type = StoreItemType.APPLICATION,
        category = category,
        version = null,
        releaseChannel = ReleaseChannel.DEVELOPMENT,
        packageName = null,
        serviceUrl = null,
        accessRule = AccessRule(),
    )
}
