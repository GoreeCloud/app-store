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
            clickTextButton(device, "Continue")
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
            clickTextButton(device, "Continue")
            waitForText(device, "Know what the Store can do today")
            clickTextButton(device, "Continue")
            waitForText(device, "Choose helpful guidance")
            clickTextButton(device, "Finish setup")

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
        const val UI_TIMEOUT_MS = 10_000L
    }
}
