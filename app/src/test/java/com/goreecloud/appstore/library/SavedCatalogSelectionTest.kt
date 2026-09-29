package com.goreecloud.appstore.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
}
