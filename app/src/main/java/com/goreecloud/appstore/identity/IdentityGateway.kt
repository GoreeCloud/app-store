package com.goreecloud.appstore.identity

import com.goreecloud.appstore.domain.IdentitySession

/**
 * Boundary for GoreeCloud Identity.
 *
 * The development adapter below exists only to exercise entitlement behavior while the
 * production GoreeCloud Identity application-facing runtime remains unaccepted.
 * Audience names here are fixtures and do not define production GoreeCloud Identity policy.
 */
interface IdentityGateway {
    val availableSessions: List<IdentitySession>
    val initialSession: IdentitySession
}

object DevelopmentIdentityGateway : IdentityGateway {
    override val availableSessions = listOf(
        IdentitySession(
            subjectId = "dev:standard",
            displayName = "Standard demo",
            audiences = setOf(
                "audience:standard",
                "channel:stable",
            ),
            isAuthenticated = true,
        ),
        IdentitySession(
            subjectId = "dev:preview",
            displayName = "Preview tester demo",
            audiences = setOf(
                "audience:standard",
                "channel:stable",
                "channel:beta",
                "channel:rc",
            ),
            isAuthenticated = true,
        ),
        IdentitySession(
            subjectId = "dev:administrator",
            displayName = "Administrator demo",
            audiences = setOf(
                "audience:standard",
                "audience:administrator",
                "channel:stable",
            ),
            isAuthenticated = true,
        ),
        IdentitySession(
            subjectId = "dev:developer",
            displayName = "Developer demo",
            audiences = setOf(
                "audience:standard",
                "audience:developer",
                "channel:stable",
                "channel:rc",
                "channel:beta",
                "channel:development",
                "channel:debug",
            ),
            isAuthenticated = true,
        ),
        IdentitySession(
            subjectId = "dev:signed-out",
            displayName = "Signed out",
            audiences = emptySet(),
            isAuthenticated = false,
        ),
    )

    override val initialSession: IdentitySession = availableSessions.first()
}
