package com.goreecloud.appstore

import android.content.Context
import android.content.res.Configuration
import androidx.core.view.WindowCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.goreecloud.appstore.data.CatalogJsonLoader
import com.goreecloud.appstore.domain.EntitlementEngine
import com.goreecloud.appstore.domain.StoreItemType
import com.goreecloud.appstore.identity.DevelopmentIdentityGateway
import com.goreecloud.appstore.library.FavoriteCatalogStore
import com.goreecloud.appstore.onboarding.SharedPreferencesAppStoreGuidanceStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppStoreOnboardingRuntimeTest {
    @Test
    fun systemBarIconsMatchCurrentAppearance() {
        resetGuidance()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val light = (
                    activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                ) != Configuration.UI_MODE_NIGHT_YES
                val controller = WindowCompat.getInsetsController(
                    activity.window,
                    activity.window.decorView,
                )
                assertEquals(light, controller.isAppearanceLightStatusBars)
                assertEquals(light, controller.isAppearanceLightNavigationBars)
            }
        }
    }

    @Test
    fun disconnectedUpdatesGuidanceStaysNearHeading() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val device = UiDevice.getInstance(instrumentation)
        val context = resetGuidance()
        ActivityScenario.launch(MainActivity::class.java).use {
            waitForText(device, "Welcome to GoreeCloud App Store")
            clickTextButton(device, "Continue")
            waitForText(device, "What works today")
            clickTextButton(device, "Continue")
            waitForText(device, "Helpful tips")
            clickTextButton(device, "Start browsing")
            waitForText(device, "Discover")

            clickTextButton(device, "Updates")
            val header = device.findObjects(By.text("Updates"))
                .minByOrNull { it.visibleBounds.top }
            val message = waitForText(device, "Updates not connected yet")
            val screenDensity = context.resources.displayMetrics.density
            assertTrue(
                "Updates disconnected state should remain close to its heading",
                header != null &&
                    (message.visibleBounds.top - header.visibleBounds.top) /
                    screenDensity < 220f,
            )
        }
    }

    @Test
    fun catalogCategoryControlsHaveAccessibleTouchTargets() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val device = UiDevice.getInstance(instrumentation)
        val context = resetGuidance()

        ActivityScenario.launch(MainActivity::class.java).use {
            waitForText(device, "Welcome to GoreeCloud App Store")
            clickTextButton(device, "Continue")
            waitForText(device, "What works today")
            clickTextButton(device, "Continue")
            waitForText(device, "Helpful tips")
            clickTextButton(device, "Start browsing")
            waitForText(device, "Discover")

            // Verify both the default filter and a named category on the actual
            // rendered touch surface, not just a source-level dp constant.
            for (label in listOf("All", "Communication")) {
                var target: UiObject2? = waitForText(device, label)
                while (target != null && !target.isClickable) {
                    target = target.parent
                }
                val chip = checkNotNull(target) {
                    "No clickable category control found for: $label"
                }
                val heightDp = chip.visibleBounds.height() / context.resources.displayMetrics.density
                assertTrue(
                    "Category filter $label is smaller than the 48dp touch target: $heightDp dp",
                    heightDp >= 47.5f,
                )
            }
        }
    }

    @Test
    fun firstUseSetupResumesAtPersistedStepAfterRecreation() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = resetGuidance()
        val device = UiDevice.getInstance(instrumentation)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitForText(device, "Welcome to GoreeCloud App Store")
            clickTextButton(device, "Continue")
            waitForText(device, "What works today")

            val persistedBeforeRecreation =
                checkNotNull(SharedPreferencesAppStoreGuidanceStore(context).read())
            assertEquals(1, persistedBeforeRecreation.setupStep)
            assertTrue(!persistedBeforeRecreation.setupCompleted)

            scenario.recreate()
            waitForText(device, "What works today")

            val persistedAfterRecreation =
                checkNotNull(SharedPreferencesAppStoreGuidanceStore(context).read())
            assertEquals(1, persistedAfterRecreation.setupStep)
            assertTrue(!persistedAfterRecreation.setupCompleted)
        }
    }

    @Test
    fun firstUseSetupCompletesAndStaysCompletedAfterRecreation() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = resetGuidance()
        val device = UiDevice.getInstance(instrumentation)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitForText(device, "Welcome to GoreeCloud App Store")
            clickTextButton(device, "Continue")
            waitForText(device, "What works today")
            clickTextButton(device, "Continue")
            waitForText(device, "Helpful tips")
            clickTextButton(device, "Start browsing")

            waitForText(device, "Discover")

            val persisted = SharedPreferencesAppStoreGuidanceStore(context).read()
            assertTrue(
                "Completed onboarding must be durably persisted",
                persisted?.setupCompleted == true,
            )

            scenario.recreate()
            waitForText(device, "Discover")
        }
    }

    @Test
    fun favoritesCanBeClearedForActiveDevelopmentIdentity() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = resetGuidance()
        val device = UiDevice.getInstance(instrumentation)
        val session = DevelopmentIdentityGateway.initialSession
        val favoriteStore = FavoriteCatalogStore(context)
        val entitled = EntitlementEngine.visibleItems(
            session,
            CatalogJsonLoader.load(context),
        )
        val favoriteItem = entitled.first()
        favoriteStore.clear(session.subjectId)
        favoriteStore.setFavorite(session.subjectId, favoriteItem.id, true)

        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                waitForText(device, "Welcome to GoreeCloud App Store")
                clickTextButton(device, "Continue")
                waitForText(device, "What works today")
                clickTextButton(device, "Continue")
                waitForText(device, "Helpful tips")
                clickTextButton(device, "Start browsing")
                waitForText(device, "Discover")

                clickTextButton(device, "Library")
                waitForText(device, favoriteItem.name)
                clickTextButton(device, "Clear")
                waitForText(device, "Clear Favorites?")
                clickTextButton(device, "Clear all Favorites")
                waitForText(device, "No Favorites yet")

                assertTrue(favoriteStore.load(session.subjectId).isEmpty())
            }
        } finally {
            favoriteStore.clear(session.subjectId)
        }
    }

    @Test
    fun recentlyOpenedAppearsDuringCurrentSession() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = resetGuidance()
        val device = UiDevice.getInstance(instrumentation)
        val session = DevelopmentIdentityGateway.initialSession
        val item = EntitlementEngine.visibleItems(
            session,
            CatalogJsonLoader.load(context),
        ).first { it.type == StoreItemType.APPLICATION }

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitForText(device, "Welcome to GoreeCloud App Store")
            clickTextButton(device, "Continue")
            waitForText(device, "What works today")
            clickTextButton(device, "Continue")
            waitForText(device, "Helpful tips")
            clickTextButton(device, "Start browsing")
            waitForText(device, "Discover")

            clickTextButton(device, "Apps")
            waitForText(device, item.name)
            clickTextButton(device, item.name)
            device.waitForIdle()
            device.pressBack()
            device.waitForIdle()

            clickTextButton(device, "Library")
            waitForText(device, "Recently opened")
            waitForText(device, item.name)

            scenario.recreate()
            waitForText(device, "Discover")
            clickTextButton(device, "Library")
            waitForText(device, "No Favorites yet")
            assertTrue(
                "Session-only recent items must not survive Activity recreation",
                !device.hasObject(By.text("Recently opened")),
            )
        }
    }

    private fun resetGuidance(): Context {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(
            context.getSharedPreferences(
                "goreecloud_app_store_guidance",
                Context.MODE_PRIVATE,
            ).edit().clear().commit(),
        )
        return context
    }

    private fun clickTextButton(
        device: UiDevice,
        text: String,
    ) {
        var target: UiObject2? = waitForText(device, text)
        while (target != null && !target.isClickable) {
            target = target.parent
        }
        checkNotNull(target) {
            "No clickable ancestor found for rendered text: $text"
        }.click()
        device.waitForIdle()
    }

    private fun waitForText(
        device: UiDevice,
        text: String,
    ): UiObject2 {
        val objectOnScreen = device.wait(
            Until.findObject(By.text(text)),
            UI_TIMEOUT_MS,
        )
        return checkNotNull(objectOnScreen) {
            "Timed out waiting for visible text: $text"
        }
    }

    private companion object {
        const val UI_TIMEOUT_MS = 30_000L
    }
}
