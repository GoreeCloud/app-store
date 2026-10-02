package com.goreecloud.appstore

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.goreecloud.appstore.onboarding.SharedPreferencesAppStoreGuidanceStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppStoreOnboardingRuntimeTest {
    @Test
    fun firstUseSetupResumesAtPersistedStepAfterRecreation() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = resetGuidance()
        val device = UiDevice.getInstance(instrumentation)

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitForText(device, "Welcome to your GoreeCloud catalog")
            waitForText(device, "Continue").click()
            waitForText(device, "Know what the Store can do today")

            val persistedBeforeRecreation =
                checkNotNull(SharedPreferencesAppStoreGuidanceStore(context).read())
            assertEquals(1, persistedBeforeRecreation.setupStep)
            assertTrue(!persistedBeforeRecreation.setupCompleted)

            scenario.recreate()
            waitForText(device, "Know what the Store can do today")

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
            waitForText(device, "Welcome to your GoreeCloud catalog")
            waitForText(device, "Continue").click()
            waitForText(device, "Know what the Store can do today")
            waitForTextWithScroll(device, "Continue").click()
            waitForText(device, "Choose helpful guidance")
            waitForTextWithScroll(device, "Finish setup").click()

            waitForText(device, "Available to you")

            val persisted = SharedPreferencesAppStoreGuidanceStore(context).read()
            assertTrue(
                "Completed onboarding must be durably persisted",
                persisted?.setupCompleted == true,
            )

            scenario.recreate()
            waitForText(device, "Available to you")
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

    private fun waitForTextWithScroll(
        device: UiDevice,
        text: String,
    ): UiObject2 {
        device.wait(Until.findObject(By.text(text)), SHORT_UI_TIMEOUT_MS)?.let { return it }

        repeat(MAX_SCROLL_ATTEMPTS) {
            val centerX = device.displayWidth / 2
            val height = device.displayHeight
            device.swipe(
                centerX,
                (height * 0.82f).toInt(),
                centerX,
                (height * 0.34f).toInt(),
                24,
            )
            device.waitForIdle()
            device.wait(Until.findObject(By.text(text)), SHORT_UI_TIMEOUT_MS)?.let { return it }
        }

        error("Timed out waiting for visible text after scrolling: $text")
    }

    private companion object {
        const val UI_TIMEOUT_MS = 10_000L
        const val SHORT_UI_TIMEOUT_MS = 1_500L
        const val MAX_SCROLL_ATTEMPTS = 4
    }
}
