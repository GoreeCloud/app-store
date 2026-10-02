package com.goreecloud.appstore

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.goreecloud.appstore.onboarding.AppStoreGuidanceState
import com.goreecloud.appstore.onboarding.SharedPreferencesAppStoreGuidanceStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppStoreOnboardingRuntimeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun firstUseSetupRestoresPersistedProgressAndCompletionAcrossRecreation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = SharedPreferencesAppStoreGuidanceStore(context)

        waitForDisplayedText("Welcome to your GoreeCloud catalog")

        composeRule.onNodeWithText("Continue").performClick()
        waitForDisplayedText("Know what the Store can do today")

        val persistedStepOne = checkNotNull(store.read())
        assertEquals(1, persistedStepOne.setupStep)
        assertTrue(!persistedStepOne.setupCompleted)

        recreateActivity()
        waitForDisplayedText("Know what the Store can do today")

        assertTrue(
            "Persisted final guidance step must be writable for recreation acceptance",
            store.write(
                persistedStepOne.copy(
                    setupStep = AppStoreGuidanceState.LAST_SETUP_STEP,
                ),
            ),
        )
        recreateActivity()
        waitForDisplayedText("Choose helpful guidance")

        assertTrue(
            "Persisted setup completion must survive recreation",
            store.write(
                checkNotNull(store.read()).copy(
                    setupCompleted = true,
                    setupStep = AppStoreGuidanceState.LAST_SETUP_STEP,
                    replayActive = false,
                ),
            ),
        )
        recreateActivity()
        waitForDisplayedText("Available to you")

        val completed = checkNotNull(store.read())
        assertTrue("Completed onboarding must remain durably persisted", completed.setupCompleted)
    }

    private fun recreateActivity() {
        composeRule.activity.runOnUiThread {
            composeRule.activity.recreate()
        }
        composeRule.waitForIdle()
    }

    private fun waitForDisplayedText(text: String) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            runCatching {
                composeRule.onNodeWithText(text).assertIsDisplayed()
            }.isSuccess
        }
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
