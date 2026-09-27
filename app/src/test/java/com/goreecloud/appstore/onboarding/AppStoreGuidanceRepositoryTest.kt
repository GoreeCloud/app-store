package com.goreecloud.appstore.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppStoreGuidanceRepositoryTest {
    @Test
    fun firstUseStartsIncompleteWithHintsEnabled() {
        val repository = AppStoreGuidanceRepository(FakeStore())

        val state = repository.load()

        assertFalse(state.setupCompleted)
        assertEquals(0, state.setupStep)
        assertTrue(state.hintsEnabled)
        assertTrue(state.dismissedHintIds.isEmpty())
    }

    @Test
    fun setupProgressAndCompletionSurviveRepositoryRecreation() {
        val store = FakeStore()
        val firstRepository = AppStoreGuidanceRepository(store)

        val stepOne = firstRepository.nextSetupStep(firstRepository.load())
        assertEquals(1, stepOne.setupStep)

        val recreatedRepository = AppStoreGuidanceRepository(store)
        val resumed = recreatedRepository.load()
        assertEquals(1, resumed.setupStep)
        assertFalse(resumed.setupCompleted)

        val completed = recreatedRepository.completeSetup(resumed)
        assertTrue(completed.setupCompleted)
        assertEquals(AppStoreGuidanceState.LAST_SETUP_STEP, completed.setupStep)

        val reopened = AppStoreGuidanceRepository(store).load()
        assertTrue(reopened.setupCompleted)
        assertEquals(AppStoreGuidanceState.LAST_SETUP_STEP, reopened.setupStep)
    }

    @Test
    fun hintsCanBeDisabledDismissedResetAndReenabled() {
        val store = FakeStore()
        val repository = AppStoreGuidanceRepository(store)
        var state = repository.load()

        state = repository.dismissHint(state, "catalog")
        assertFalse(state.isHintVisible("catalog"))

        state = repository.resetDismissedHints(state)
        assertTrue(state.isHintVisible("catalog"))

        state = repository.setHintsEnabled(state, false)
        assertFalse(state.isHintVisible("catalog"))

        state = repository.setHintsEnabled(state, true)
        assertTrue(state.isHintVisible("catalog"))
    }

    @Test
    fun replayResetsSetupProgressWithoutOverwritingHintPreference() {
        val store = FakeStore()
        val repository = AppStoreGuidanceRepository(store)
        var state = repository.load()
        state = repository.setHintsEnabled(state, false)
        state = repository.completeSetup(state)
        assertTrue(state.setupCompleted)

        val replay = repository.replaySetup(state)

        assertFalse(replay.setupCompleted)
        assertEquals(0, replay.setupStep)
        assertFalse(replay.hintsEnabled)
    }

    @Test
    fun failedPersistenceDoesNotAdvancePresentedState() {
        val store = FakeStore(acceptWrites = false)
        val repository = AppStoreGuidanceRepository(store)
        val initial = repository.load()

        val attempted = repository.nextSetupStep(initial)

        assertEquals(initial, attempted)
        assertEquals(null, store.stored)
    }

    private class FakeStore(
        var stored: AppStoreGuidanceState? = null,
        var acceptWrites: Boolean = true,
    ) : AppStoreGuidanceStore {
        override fun read(): AppStoreGuidanceState? = stored

        override fun write(state: AppStoreGuidanceState): Boolean {
            if (!acceptWrites) return false
            stored = state
            return true
        }
    }
}
