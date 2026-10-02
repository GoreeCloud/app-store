package com.goreecloud.appstore

import android.content.Context
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppStoreOnboardingRuntimeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun firstUseSetupResumesAcrossRecreationAndStaysCompleted() {
        composeRule.onNodeWithText("Welcome to your GoreeCloud catalog")
            .assertIsDisplayed()

        composeRule.onNodeWithText("Continue").performClick()
        composeRule.onNodeWithText("Know what the Store can do today")
            .assertIsDisplayed()

        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Know what the Store can do today")
            .assertIsDisplayed()

        composeRule.onNodeWithText("Continue").performClick()
        composeRule.onNodeWithText("Choose helpful guidance")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Finish setup").performClick()

        composeRule.onNodeWithText("Available to you")
            .assertIsDisplayed()

        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Welcome to your GoreeCloud catalog")
            .assertDoesNotExist()
        composeRule.onNodeWithText("Available to you")
            .assertIsDisplayed()
    }

    companion object {
        @JvmStatic
        @BeforeClass
        fun clearPersistedGuidance() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            check(
                context.getSharedPreferences(
                    "goreecloud_app_store_guidance",
                    Context.MODE_PRIVATE,
                ).edit().clear().commit(),
            )
        }
    }
}
