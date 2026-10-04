package com.goreecloud.appstore.library

import org.junit.Assert.assertEquals
import org.junit.Test

class RecentlyViewedCatalogSelectionTest {
    @Test
    fun recordingMovesItemToFrontWithoutDuplicates() {
        val first = RecentlyViewedCatalogSelection.record(
            current = listOf("app.one", "app.two"),
            itemId = "app.three",
        )
        assertEquals(listOf("app.three", "app.one", "app.two"), first)

        val revisited = RecentlyViewedCatalogSelection.record(
            current = first,
            itemId = "app.one",
        )
        assertEquals(listOf("app.one", "app.three", "app.two"), revisited)
    }

    @Test
    fun normalizationIsBoundedAndRemovesBlankEntries() {
        val current = (1..20).map { "app.$it" } + listOf("", "app.2")
        val normalized = RecentlyViewedCatalogSelection.normalize(current, limit = 5)
        assertEquals(listOf("app.1", "app.2", "app.3", "app.4", "app.5"), normalized)
    }

    @Test(expected = IllegalArgumentException::class)
    fun blankItemFailsClosed() {
        RecentlyViewedCatalogSelection.record(emptyList(), "   ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun nonPositiveLimitFailsClosed() {
        RecentlyViewedCatalogSelection.normalize(emptyList(), limit = 0)
    }
}
