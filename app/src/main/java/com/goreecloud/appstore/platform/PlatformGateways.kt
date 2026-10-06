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
            detail = "Targets the GLAZE UI V1.6 / 1.6.0 Official Anchor; rendered conformance is not yet claimed.",
        ),
        PlatformIntegrationStatus(
            system = "GoreeCloud Identity",
            state = IntegrationState.SOURCE_BOUNDARY,
            detail = "Development subject re-authorization exists for the bounded package backend; production OIDC runtime is not connected.",
        ),
        PlatformIntegrationStatus(
            system = "Wardveil Security",
            state = IntegrationState.SOURCE_BOUNDARY,
            detail = "Delivery-enabled Development artifacts require a current clean Wardveil ClamAV reference scan; production Wardveil acceptance remains pending.",
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
