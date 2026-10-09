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

    @Test
    fun searchAcceptsMultipleTermsAcrossFieldsInAnyOrder() {
        for (query in listOf("alpha communication", "  COMMUNICATION   alpha  ")) {
            assertEquals(
                listOf("alpha"),
                CatalogPresentation.filterAndSort(
                    items = items,
                    query = query,
                    category = null,
                    sort = CatalogSort.CATALOG_ORDER,
                ).map { it.id },
            )
        }
    }

    @Test
    fun searchRequiresEveryTermToMatch() {
        assertEquals(
            emptyList<String>(),
            CatalogPresentation.filterAndSort(
                items = items,
                query = "alpha platform",
                category = null,
                sort = CatalogSort.CATALOG_ORDER,
            ).map { it.id },
        )
    }

    @Test
    fun searchFoldsAccentsAndCompatibilityCharactersWithoutReordering() {
        val accented = item("cafe", "Café", "Utilities")
        val plain = item("plain", "Cafe", "Utilities")
        assertEquals(
            listOf("cafe", "plain"),
            CatalogPresentation.filterAndSort(
                items = listOf(accented, plain),
                query = "CAFÉ",
                category = null,
                sort = CatalogSort.CATALOG_ORDER,
            ).map { it.id },
        )
        assertEquals(
            listOf("cafe", "plain"),
            CatalogPresentation.filterAndSort(
                items = listOf(accented, plain),
                query = "cafe",
                category = null,
                sort = CatalogSort.CATALOG_ORDER,
            ).map { it.id },
        )
    }

    @Test
    fun accentedMultiTermSearchCannotBypassCategoryRestriction() {
        val catalog = listOf(
            item("camera", "Cámara", "Photography"),
            item("other", "Cámara", "Utilities"),
        )
        assertEquals(
            listOf("camera"),
            CatalogPresentation.filterAndSort(
                items = catalog,
                query = "photography CAMARA",
                category = "Photography",
                sort = CatalogSort.NAME,
            ).map { it.id },
        )
        assertEquals(
            emptyList<String>(),
            CatalogPresentation.filterAndSort(
                items = catalog,
                query = "utilities camara",
                category = "Photography",
                sort = CatalogSort.NAME,
            ).map { it.id },
        )
    }

    @Test
    fun librarySearchCanIncludeTypeWithoutChangingPublicCatalogSearch() {
        val first = items.first()
        assertEquals(false, CatalogSearch.matches(first, "application"))
        assertEquals(true, CatalogSearch.matches(first, "application", includeType = true))
        assertEquals(false, CatalogSearch.matches(first, "service", includeType = true))
        assertEquals(true, CatalogSearch.matches(first, "  "))
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
