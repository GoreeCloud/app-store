package com.goreecloud.appstore.ui

/**
 * Bounded App Store mapping to the current Stable GLAZE UI authority.
 *
 * This object is presentation-only. It cannot grant catalog entitlement, package-delivery authority,
 * Identity state, privacy/security decisions, installation permission, or release acceptance.
 */
internal object GlazeV16PresentationPolicy {
    const val stableVersion = "1.6.0"
    const val stableSourceRevision = "a7180679ea851389e0f3004515f9a25f420e716d"
    const val platformContractVersion = "0.4"
    const val platformContractRevision = "e49b9afdea094c96a36a0457b1603f2fa8e8fa6b"

    const val colorOnlyMeaningAllowed = false
    const val mayInferIdentityState = false
    const val mayInferEntitlement = false
    const val mayInferPackageTrust = false
    const val mayGrantInstallAuthority = false
    const val renderedAcceptance = false
    const val representativeDeviceAcceptance = false
    const val humanVisualAcceptance = false

    fun minimumTouchTargetDp(touchAssistance: Boolean): Int =
        if (touchAssistance) 56 else 48
}
