package com.goreecloud.appstore.onboarding

import android.content.Context

data class AppStoreGuidanceState(
    val setupCompleted: Boolean = false,
    val setupStep: Int = 0,
    val replayActive: Boolean = false,
    val hintsEnabled: Boolean = true,
    val dismissedHintIds: Set<String> = emptySet(),
) {
    init {
        require(setupStep in 0..LAST_SETUP_STEP) {
            "setupStep must be between 0 and $LAST_SETUP_STEP"
        }
    }

    fun isHintVisible(hintId: String): Boolean =
        hintsEnabled && hintId !in dismissedHintIds

    companion object {
        const val LAST_SETUP_STEP = 2
    }
}

interface AppStoreGuidanceStore {
    fun read(): AppStoreGuidanceState?
    fun write(state: AppStoreGuidanceState): Boolean
}

/**
 * Small state machine for mandatory first-use guidance.
 *
 * Every transition is persisted before the returned UI state advances. A failed write therefore
 * leaves the caller on the last durable state rather than presenting setup as complete when it is
 * not. This is intentionally independent from Identity, entitlement, package-delivery, and release
 * authority.
 */
class AppStoreGuidanceRepository(
    private val store: AppStoreGuidanceStore,
) {
    fun load(): AppStoreGuidanceState =
        store.read() ?: AppStoreGuidanceState()

    fun nextSetupStep(current: AppStoreGuidanceState): AppStoreGuidanceState {
        if (current.setupCompleted && !current.replayActive) return current
        val next = current.copy(
            setupStep = (current.setupStep + 1).coerceAtMost(AppStoreGuidanceState.LAST_SETUP_STEP),
        )
        return persist(current, next)
    }

    fun previousSetupStep(current: AppStoreGuidanceState): AppStoreGuidanceState {
        if (current.setupCompleted && !current.replayActive) return current
        val next = current.copy(setupStep = (current.setupStep - 1).coerceAtLeast(0))
        return persist(current, next)
    }

    fun completeSetup(current: AppStoreGuidanceState): AppStoreGuidanceState =
        persist(
            current,
            current.copy(
                setupCompleted = true,
                setupStep = AppStoreGuidanceState.LAST_SETUP_STEP,
                replayActive = false,
            ),
        )

    fun replaySetup(current: AppStoreGuidanceState): AppStoreGuidanceState {
        if (!current.setupCompleted || current.replayActive) return current
        return persist(
            current,
            current.copy(
                setupStep = 0,
                replayActive = true,
            ),
        )
    }

    fun cancelReplay(current: AppStoreGuidanceState): AppStoreGuidanceState {
        if (!current.setupCompleted || !current.replayActive) return current
        return persist(
            current,
            current.copy(
                setupStep = AppStoreGuidanceState.LAST_SETUP_STEP,
                replayActive = false,
            ),
        )
    }

    fun setHintsEnabled(
        current: AppStoreGuidanceState,
        enabled: Boolean,
    ): AppStoreGuidanceState =
        persist(current, current.copy(hintsEnabled = enabled))

    fun dismissHint(
        current: AppStoreGuidanceState,
        hintId: String,
    ): AppStoreGuidanceState {
        if (hintId.isBlank()) return current
        return persist(
            current,
            current.copy(dismissedHintIds = current.dismissedHintIds + hintId),
        )
    }

    fun resetDismissedHints(current: AppStoreGuidanceState): AppStoreGuidanceState =
        persist(current, current.copy(dismissedHintIds = emptySet()))

    private fun persist(
        current: AppStoreGuidanceState,
        next: AppStoreGuidanceState,
    ): AppStoreGuidanceState =
        if (store.write(next)) next else current
}

class SharedPreferencesAppStoreGuidanceStore(
    context: Context,
) : AppStoreGuidanceStore {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    override fun read(): AppStoreGuidanceState? {
        if (!preferences.contains(KEY_SCHEMA_VERSION)) return null

        val schemaVersion = preferences.getInt(KEY_SCHEMA_VERSION, -1)
        if (schemaVersion !in 1..SCHEMA_VERSION) {
            // Unknown persisted state must not silently mark setup complete or re-enable hints.
            return AppStoreGuidanceState(
                setupCompleted = false,
                setupStep = 0,
                replayActive = false,
                hintsEnabled = false,
            )
        }

        return AppStoreGuidanceState(
            setupCompleted = preferences.getBoolean(KEY_SETUP_COMPLETED, false),
            setupStep = preferences.getInt(KEY_SETUP_STEP, 0)
                .coerceIn(0, AppStoreGuidanceState.LAST_SETUP_STEP),
            replayActive =
                schemaVersion >= 2 && preferences.getBoolean(KEY_REPLAY_ACTIVE, false),
            hintsEnabled = preferences.getBoolean(KEY_HINTS_ENABLED, true),
            dismissedHintIds = preferences.getStringSet(KEY_DISMISSED_HINT_IDS, emptySet())
                ?.toSet()
                .orEmpty(),
        )
    }

    override fun write(state: AppStoreGuidanceState): Boolean =
        preferences.edit()
            .putInt(KEY_SCHEMA_VERSION, SCHEMA_VERSION)
            .putBoolean(KEY_SETUP_COMPLETED, state.setupCompleted)
            .putInt(KEY_SETUP_STEP, state.setupStep)
            .putBoolean(KEY_REPLAY_ACTIVE, state.replayActive)
            .putBoolean(KEY_HINTS_ENABLED, state.hintsEnabled)
            .putStringSet(KEY_DISMISSED_HINT_IDS, state.dismissedHintIds.toSet())
            .commit()

    private companion object {
        const val PREFERENCES_NAME = "goreecloud_app_store_guidance"
        const val SCHEMA_VERSION = 2
        const val KEY_SCHEMA_VERSION = "schema_version"
        const val KEY_SETUP_COMPLETED = "setup_completed"
        const val KEY_SETUP_STEP = "setup_step"
        const val KEY_REPLAY_ACTIVE = "replay_active"
        const val KEY_HINTS_ENABLED = "hints_enabled"
        const val KEY_DISMISSED_HINT_IDS = "dismissed_hint_ids"
    }
}
