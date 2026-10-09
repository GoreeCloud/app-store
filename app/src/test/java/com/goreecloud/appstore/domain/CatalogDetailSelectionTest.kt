package com.goreecloud.appstore.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CatalogDetailSelectionTest {
    private val restricted = StoreItem(
        id = "restricted",
        name = "Previously visible",
        summary = "Old metadata must not survive a session change.",
        type = StoreItemType.APPLICATION,
        category = "Developer",
        version = null,
        releaseChannel = ReleaseChannel.DEVELOPMENT,
        packageName = null,
        serviceUrl = null,
        accessRule = AccessRule(anyAudience = setOf("audience:developer")),
    )

    @Test
    fun previouslyVisibleItemIsHiddenAfterIdentityChanges() {
        val developer = IdentitySession(
            subjectId = "developer",
            displayName = "Developer",
            audiences = setOf("audience:developer"),
            isAuthenticated = true,
        )
        val standard = developer.copy(
            subjectId = "standard",
            displayName = "Standard",
            audiences = setOf("audience:standard"),
        )
        val catalog = listOf(restricted)
        val priorVisibility = EntitlementEngine.visibleItems(developer, catalog)
        val currentVisibility = EntitlementEngine.visibleItems(standard, catalog)

        assertEquals(restricted, CatalogDetailSelection.resolve(
            restricted,
            priorVisibility.associateBy { it.id },
        ))
        assertNull(CatalogDetailSelection.resolve(
            restricted,
            currentVisibility.associateBy { it.id },
        ))
    }

    @Test
    fun selectedIdResolvesFromCurrentEntitledMetadataNotStaleSelection() {
        val current = restricted.copy(name = "Current details", summary = "Current approved metadata")
        assertEquals(current, CatalogDetailSelection.resolve(
            restricted,
            mapOf(current.id to current),
        ))
        assertNull(CatalogDetailSelection.resolve(null, mapOf(current.id to current)))
    }
}
