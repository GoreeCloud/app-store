package com.goreecloud.appstore.platform

enum class IntegrationState {
    TARGETED,
    SOURCE_BOUNDARY,
    NOT_CONNECTED,
}

data class PlatformIntegrationStatus(
    val system: String,
    val state: IntegrationState,
    val detail: String,
)

object PlatformIntegrationRegistry {
    val current = listOf(
        PlatformIntegrationStatus(
            system = "Glaze",
            state = IntegrationState.TARGETED,
            detail = "Targets the current Stable 2.0.0 consumer contract; conformance is not yet claimed.",
        ),
        PlatformIntegrationStatus(
            system = "GoreeCloud Identity",
            state = IntegrationState.SOURCE_BOUNDARY,
            detail = "Identity and entitlement boundaries exist; production OIDC runtime is not connected.",
        ),
        PlatformIntegrationStatus(
            system = "Wardveil Security",
            state = IntegrationState.SOURCE_BOUNDARY,
            detail = "Package verification is reserved; package delivery is not connected.",
        ),
        PlatformIntegrationStatus(
            system = "Privacy Shield",
            state = IntegrationState.SOURCE_BOUNDARY,
            detail = "Development analytics are disabled; production privacy-policy integration remains pending.",
        ),
        PlatformIntegrationStatus(
            system = "Everkeep",
            state = IntegrationState.SOURCE_BOUNDARY,
            detail = "Library-history recovery is defined; production continuity acceptance remains pending.",
        ),
        PlatformIntegrationStatus(
            system = "GoreeCloud Mesh",
            state = IntegrationState.SOURCE_BOUNDARY,
            detail = "Catalog and lifecycle events are defined; production transport is not connected.",
        ),
    )
}

interface PackageDeliveryGateway {
    val isAvailable: Boolean
}

object UnavailablePackageDeliveryGateway : PackageDeliveryGateway {
    override val isAvailable: Boolean = false
}
