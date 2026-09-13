package com.goreecloud.appstore.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlazeOpticalV14Test {
    @Test
    fun provenanceMatchesCurrentStableV14Authority() {
        assertEquals("1.4.0", GlazeOpticalV14.VERSION)
        assertEquals(
            "84cb3db4884042f0fa25ed6d475a127fb110f596",
            GlazeOpticalV14.STABLE_REVISION,
        )
        assertEquals(0.08f, GlazeOpticalV14.MAX_MEMORY_INFLUENCE)
    }

    @Test
    fun reducedTransparencyAndForcedColorsFailClosedToSolidAccessible() {
        listOf(
            GlazeOpticalInput(reducedTransparency = true),
            GlazeOpticalInput(forcedColors = true),
        ).forEach { input ->
            val treatment = GlazeOpticalV14.resolve(input)
            assertEquals(GlazeOpticalMode.SOLID_ACCESSIBLE, treatment.mode)
            assertEquals(0f, treatment.frostStrength)
            assertEquals(1f, treatment.semanticProtection)
            assertEquals(0f, treatment.environmentalMemoryInfluence)
            assertEquals(0f, treatment.decorativeWarmth)
        }
    }

    @Test
    fun increasedContrastSuppressesDecorativeOptics() {
        val treatment = GlazeOpticalV14.resolve(
            GlazeOpticalInput(increasedContrast = true, backgroundComplexity = 1f),
        )
        assertEquals(GlazeOpticalMode.PROTECTED_GLASS, treatment.mode)
        assertEquals(1f, treatment.semanticProtection)
        assertEquals(0f, treatment.environmentalMemoryInfluence)
        assertEquals(0f, treatment.decorativeWarmth)
    }

    @Test
    fun neutralOpticsRemainBoundedAndNonCollecting() {
        val low = GlazeOpticalV14.resolve(GlazeOpticalInput(backgroundComplexity = -10f))
        val high = GlazeOpticalV14.resolve(GlazeOpticalInput(backgroundComplexity = 10f))

        assertEquals(GlazeOpticalMode.NEUTRAL_GLASS, low.mode)
        assertEquals(0.55f, low.frostStrength)
        assertEquals(0.80f, high.frostStrength)
        assertTrue(high.frostStrength <= 0.80f)
        assertEquals(0f, low.environmentalMemoryInfluence)
        assertEquals(0f, high.environmentalMemoryInfluence)
    }
}
