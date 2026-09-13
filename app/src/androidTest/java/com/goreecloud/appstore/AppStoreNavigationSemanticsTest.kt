package com.goreecloud.appstore

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onParent
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AppStoreNavigationSemanticsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun bottomNavigationDestinationsExposeAtLeast48DpSemanticTargets() {
        val density = InstrumentationRegistry.getInstrumentation()
            .targetContext.resources.displayMetrics.density

        listOf("Discover", "Apps", "Services", "Updates", "Library").forEach { label ->
            val interaction = composeRule.onNode(hasText(label) and hasClickAction())
            interaction
                .assertExists()
                .assertIsDisplayed()
                .assertHasClickAction()

            if (label == "Discover") {
                interaction.assertIsSelected()
            }

            val bounds = interaction.fetchSemanticsNode().boundsInRoot
            val widthDp = bounds.width / density
            val heightDp = bounds.height / density

            assertTrue(
                "$label semantic target width is ${widthDp}dp; expected at least 48dp",
                widthDp >= 48.0f,
            )
            assertTrue(
                "$label semantic target height is ${heightDp}dp; expected at least 48dp",
                heightDp >= 48.0f,
            )

            println("APP_STORE_NAV_TARGET label=$label widthDp=$widthDp heightDp=$heightDp")
        }
    }

    @Test
    fun bottomNavigationDestinationsDriveExpectedContent() {
        val expectations = listOf(
            "Apps" to "Search apps",
            "Services" to "Search services",
            "Updates" to "Updates are unavailable in this development build",
            "Library" to "Library history is unavailable in this development build",
            "Discover" to "Search apps and services",
        )

        expectations.forEach { (label, expectedContent) ->
            val navigation = composeRule.onNode(hasText(label) and hasClickAction())
            navigation.performClick()
            composeRule.waitForIdle()
            navigation.assertIsSelected()
            composeRule.onNode(hasText(expectedContent)).assertExists().assertIsDisplayed()
        }
    }

    @Test
    fun categoryFiltersAreTouchSizedAndNarrowOnlyTheEntitledCatalog() {
        val density = InstrumentationRegistry.getInstrumentation()
            .targetContext.resources.displayMetrics.density

        // The shared fixture is a development-channel catalog. Exercise discovery through the
        // explicit Developer demo identity instead of weakening channel authorization for the
        // default Standard demo identity. The collapsed selector intentionally uses the compact
        // session label "Standard"; full fixture display names appear only inside the menu.
        composeRule.onNode(hasText("Standard") and hasClickAction())
            .assertExists()
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNode(hasText("Developer demo") and hasClickAction())
            .assertExists()
            .performClick()
        composeRule.waitForIdle()

        // Discover owns the category controls as its fourth LazyColumn item (index 3):
        // development notice, hero, search, then category filters. Once categories materialize,
        // both the vertical catalog and horizontal category row are scrollable. Bind explicitly to
        // the vertical catalog by requiring the Discover search field as a descendant rather than
        // depending on semantics-tree ordering.
        val catalog = composeRule.onNode(
            hasScrollAction() and hasAnyDescendant(hasText("Search apps and services"))
        )
        catalog.performScrollToIndex(3)
        composeRule.waitForIdle()

        val all = composeRule.onNode(hasText("All") and hasClickAction())
        all.assertExists().assertIsDisplayed().assertHasClickAction()
        all.onParent().performScrollToNode(hasText("Communication") and hasClickAction())
        composeRule.waitForIdle()

        val communication = composeRule.onNode(hasText("Communication") and hasClickAction())
        communication.assertExists().assertIsDisplayed().assertHasClickAction()
        val bounds = communication.fetchSemanticsNode().boundsInRoot
        assertTrue(bounds.width / density >= 48.0f)
        assertTrue(bounds.height / density >= 48.0f)

        communication.performClick()
        composeRule.waitForIdle()
        composeRule.onNode(hasText("1 item in this development catalog"))
            .assertExists()
            .assertIsDisplayed()

        // Communication remains materialized after selection, so use its category-row
        // parent as the stable scroll authority to return to All. Do not try to resolve
        // an off-screen All child before scrolling the horizontal LazyRow back to it.
        composeRule.onNode(hasText("Communication") and hasClickAction())
            .onParent()
            .performScrollToNode(hasText("All") and hasClickAction())
        composeRule.waitForIdle()

        composeRule.onNode(hasText("All") and hasClickAction())
            .assertExists()
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNode(hasText("11 items in this development catalog"))
            .assertExists()
            .assertIsDisplayed()
    }
}
