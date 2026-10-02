package com.goreecloud.appstore.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class GlazeV16PresentationPolicyTest {
    @Test
    fun currentStableAuthorityIsPinnedExactly() {
        assertEquals("1.6.0", GlazeV16PresentationPolicy.stableVersion)
        assertEquals(
            "a7180679ea851389e0f3004515f9a25f420e716d",
            GlazeV16PresentationPolicy.stableSourceRevision,
        )
        assertEquals("0.4", GlazeV16PresentationPolicy.platformContractVersion)
        assertEquals(
            "e49b9afdea094c96a36a0457b1603f2fa8e8fa6b",
            GlazeV16PresentationPolicy.platformContractRevision,
        )
        assertEquals(48, GlazeV16PresentationPolicy.minimumTouchTargetDp(false))
        assertEquals(56, GlazeV16PresentationPolicy.minimumTouchTargetDp(true))
    }

    @Test
    fun presentationCannotManufactureAppStoreAuthority() {
        assertFalse(GlazeV16PresentationPolicy.colorOnlyMeaningAllowed)
        assertFalse(GlazeV16PresentationPolicy.mayInferIdentityState)
        assertFalse(GlazeV16PresentationPolicy.mayInferEntitlement)
        assertFalse(GlazeV16PresentationPolicy.mayInferPackageTrust)
        assertFalse(GlazeV16PresentationPolicy.mayGrantInstallAuthority)
        assertFalse(GlazeV16PresentationPolicy.renderedAcceptance)
        assertFalse(GlazeV16PresentationPolicy.representativeDeviceAcceptance)
        assertFalse(GlazeV16PresentationPolicy.humanVisualAcceptance)
    }
}
