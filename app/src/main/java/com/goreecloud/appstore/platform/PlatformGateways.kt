package com.goreecloud.appstore.platform

enum class IntegrationState { TARGETED, SOURCE_BOUNDARY, NOT_CONNECTED }

data class PlatformIntegrationStatus(
    val system: String,
    val state: IntegrationState,
    val detail: String,
)

object GlazeUiContract {
    const val VERSION = "1.4.0"
    const val RELEASE_TAG = "v1.4.0"
    const val RELEASE_REVISION = "84cb3db4884042f0fa25ed6d475a127fb110f596"
    // V1.4's verified Stable merge revision is the exact downstream source pin for this consumer.
    const val SOURCE_QUALIFICATION_ANCHOR = RELEASE_REVISION
    const val ROLLBACK_VERSION = "1.3.0"
    const val MATERIAL_RULE = "Neutral glass remains the material foundation. Optical adaptation is contextual, bounded, and subordinate to meaning, accessibility, privacy, security, and task completion."
    const val SYSTEM_SHELL_SCOPE = "Application"
    const val CONFORMANCE_ACCEPTED = false
}

object PlatformIntegrationRegistry {
    val current = listOf(
        PlatformIntegrationStatus(
            system = "GLAZE UI V1.4 — Optical Intelligence",
            state = IntegrationState.TARGETED,
            detail = "Android, Linux, and Web source mappings target current Stable 1.4.0. V1.3 structural behavior is retained while optical behavior remains local, bounded, accessibility-subordinate, and non-semantic. Fresh consumer-local V1.4 rendered, accessibility, representative-target, performance, rollback, and production acceptance remain pending.",
        ),
        PlatformIntegrationStatus(
            system = "GoreeCloud Manager",
            state = IntegrationState.NOT_CONNECTED,
            detail = "Manager registration, operational visibility, and administrative integration are not established.",
        ),
        PlatformIntegrationStatus(
            system = "GoreeCloud Identity",
            state = IntegrationState.SOURCE_BOUNDARY,
            detail = "Identity gateway and entitlement inputs exist; production OIDC/runtime integration is not connected.",
        ),
        PlatformIntegrationStatus(
            system = "Wardveil Security",
            state = IntegrationState.SOURCE_BOUNDARY,
            detail = "Package trust and verification boundaries are reserved; package delivery remains disabled.",
        ),
        PlatformIntegrationStatus(
            system = "Privacy Shield",
            state = IntegrationState.SOURCE_BOUNDARY,
            detail = "The Development client collects no analytics; production privacy-policy and runtime acceptance remain pending.",
        ),
        PlatformIntegrationStatus(
            system = "Everkeep",
            state = IntegrationState.SOURCE_BOUNDARY,
            detail = "Library/history recovery boundaries are defined; production continuity evidence remains pending.",
        ),
        PlatformIntegrationStatus(
            system = "GoreeCloud Mesh",
            state = IntegrationState.SOURCE_BOUNDARY,
            detail = "Catalog/lifecycle coordination boundaries are defined; production event transport is not connected.",
        ),
    )
}

interface PackageDeliveryGateway { val isAvailable: Boolean }
object UnavailablePackageDeliveryGateway : PackageDeliveryGateway {
    override val isAvailable = false
}
