package com.goreecloud.appstore.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedCatalogSelectionTest {
    @Test
    fun normalizationIsBoundedToNonBlankUniqueIds() {
        assertEquals(
            linkedSetOf("app.one", "service.two"),
            SavedCatalogSelection.normalize(
                listOf(" app.one ", "", "app.one", "service.two"),
            ),
        )
    }

    @Test
    fun setSavedAddsAndRemovesWithoutChangingOtherItems() {
        val added = SavedCatalogSelection.setSaved(
            current = setOf("app.one"),
            itemId = "service.two",
            saved = true,
        )
        assertTrue("app.one" in added)
        assertTrue("service.two" in added)

        val removed = SavedCatalogSelection.setSaved(
            current = added,
            itemId = "app.one",
            saved = false,
        )
        assertFalse("app.one" in removed)
        assertEquals(setOf("service.two"), removed)
    }

    @Test(expected = IllegalArgumentException::class)
    fun blankIdentityFailsClosed() {
        SavedCatalogSelection.setSaved(
            current = emptySet(),
            itemId = "   ",
            saved = true,
        )
    }

    @Test
    fun identityNamespaceIsDeterministicWithoutEmbeddingRawSubject() {
        val subject = "development-user-123@example.test"
        val first = SavedCatalogIdentityNamespace.keyFor(subject)
        val second = SavedCatalogIdentityNamespace.keyFor(subject)
        val whitespaceVariant = SavedCatalogIdentityNamespace.keyFor("  $subject  ")

        assertEquals(first, second)
        assertNotEquals(first, whitespaceVariant)
        assertTrue(first.startsWith("saved_items_v1:"))
        assertFalse(first.contains(subject))
        assertEquals("saved_items_v1:".length + 64, first.length)
    }

    @Test
    fun favoriteIdentityNamespaceIsDistinctAndDoesNotEmbedRawSubject() {
        val subject = "development-user-123@example.test"
        val saved = SavedCatalogIdentityNamespace.keyFor(subject)
        val favorite = FavoriteCatalogIdentityNamespace.keyFor(subject)

        assertTrue(favorite.startsWith("favorite_items_v1:"))
        assertFalse(favorite.contains(subject))
        assertEquals("favorite_items_v1:".length + 64, favorite.length)
        assertNotEquals(saved, favorite)
        assertEquals(favorite, FavoriteCatalogIdentityNamespace.keyFor(subject))
        assertNotEquals(favorite, FavoriteCatalogIdentityNamespace.keyFor("  $subject  "))
    }

    @Test
    fun recentlyViewedMovesOpenedItemToFrontAndCapsHistory() {
        val current = (1..25).map { "item.$it" }

        val updated = RecentlyViewedCatalogSelection.record(
            current = current,
            itemId = "item.10",
        )

        assertEquals(RecentlyViewedCatalogSelection.MAX_ITEMS, updated.size)
        assertEquals("item.10", updated.first())
        assertEquals(1, updated.count { it == "item.10" })
        assertFalse("item.25" in updated)
    }

    @Test
    fun recentlyViewedCodecRoundTripsAndMalformedDataFailsClosed() {
        val ids = listOf("app.one", "service:two", "app.three")
        val encoded = RecentlyViewedCatalogSelection.encode(ids)

        assertEquals(ids, RecentlyViewedCatalogSelection.decode(encoded))
        assertEquals(emptyList<String>(), RecentlyViewedCatalogSelection.decode("broken"))
        assertEquals(emptyList<String>(), RecentlyViewedCatalogSelection.decode("9999:x"))
        val twentyValid = RecentlyViewedCatalogSelection.encode((1..20).map { "item.$it" })
        assertEquals(
            emptyList<String>(),
            RecentlyViewedCatalogSelection.decode(twentyValid + "broken"),
        )
    }

    @Test
    fun recentlyViewedIdentityNamespaceIsDistinctAndDoesNotEmbedRawSubject() {
        val subject = "development-user-123@example.test"
        val recent = RecentlyViewedCatalogIdentityNamespace.keyFor(subject)

        assertTrue(recent.startsWith("recently_viewed_items_v1:"))
        assertFalse(recent.contains(subject))
        assertEquals("recently_viewed_items_v1:".length + 64, recent.length)
        assertNotEquals(SavedCatalogIdentityNamespace.keyFor(subject), recent)
        assertNotEquals(FavoriteCatalogIdentityNamespace.keyFor(subject), recent)
        assertEquals(recent, RecentlyViewedCatalogIdentityNamespace.keyFor(subject))
        assertNotEquals(recent, RecentlyViewedCatalogIdentityNamespace.keyFor("  $subject  "))
    }

    @Test(expected = IllegalArgumentException::class)
    fun blankRecentlyViewedIdentityNamespaceFailsClosed() {
        RecentlyViewedCatalogIdentityNamespace.keyFor("   ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun blankFavoriteIdentityNamespaceFailsClosed() {
        FavoriteCatalogIdentityNamespace.keyFor("   ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun blankIdentityNamespaceFailsClosed() {
        SavedCatalogIdentityNamespace.keyFor("   ")
    }

}
