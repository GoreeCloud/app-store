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
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppStoreOnboardingRuntimeTest {
    @Test
    fun firstUseSetupResumesAcrossRecreationAndStaysCompleted() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        check(
            context.getSharedPreferences(
                "goreecloud_app_store_guidance",
                Context.MODE_PRIVATE,
            ).edit().clear().commit(),
        )

        val device = UiDevice.getInstance(instrumentation)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitForText(device, "Welcome to your GoreeCloud catalog")
            waitForText(device, "Continue").click()

            waitForText(device, "Know what the Store can do today")
            scenario.recreate()
            device.waitForIdle()
            waitForText(device, "Know what the Store can do today")
            waitForText(device, "Continue").click()

            waitForText(device, "Choose helpful guidance")
            waitForText(device, "Finish setup").click()

            waitForText(device, "Available to you")
            val persisted = SharedPreferencesAppStoreGuidanceStore(context).read()
            assertTrue(
                "Completed onboarding must be durably persisted",
                persisted?.setupCompleted == true,
            )

            scenario.recreate()
            device.waitForIdle()
            waitForText(device, "Available to you")
        }
    }

    private fun waitForText(
        device: UiDevice,
        text: String,
    ): UiObject2 {
        val node = device.wait(
            Until.findObject(By.text(text)),
            UI_TIMEOUT_MS,
        )
        return checkNotNull(node) {
            "Timed out waiting for rendered text: $text"
        }
    }

    private companion object {
        const val UI_TIMEOUT_MS = 10_000L
    }
}
