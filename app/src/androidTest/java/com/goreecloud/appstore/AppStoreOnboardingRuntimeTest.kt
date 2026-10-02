package com.goreecloud.appstore

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.goreecloud.appstore.onboarding.SharedPreferencesAppStoreGuidanceStore
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppStoreOnboardingRuntimeTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun firstUseSetupResumesAcrossRecreationAndStaysCompleted() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(
            context.getSharedPreferences(
                "goreecloud_app_store_guidance",
                Context.MODE_PRIVATE,
            ).edit().clear().commit(),
        )

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitForDisplayedText("Welcome to your GoreeCloud catalog")

            composeRule.onNodeWithText("Continue").performClick()
            waitForDisplayedText("Know what the Store can do today")

            scenario.recreate()
            composeRule.waitForIdle()
            waitForDisplayedText("Know what the Store can do today")

            composeRule.onNodeWithText("Continue").performClick()
            waitForDisplayedText("Choose helpful guidance")
            composeRule.onNodeWithText("Finish setup").performClick()

            waitForDisplayedText("Available to you")

            val persisted = SharedPreferencesAppStoreGuidanceStore(context).read()
            assertTrue(
                "Completed onboarding must be durably persisted",
                persisted?.setupCompleted == true,
            )

            scenario.recreate()
            composeRule.waitForIdle()
            waitForDisplayedText("Available to you")
        }
    }

    private fun waitForDisplayedText(text: String) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching {
                composeRule.onNodeWithText(text).assertIsDisplayed()
            }.isSuccess
        }
    }
}
