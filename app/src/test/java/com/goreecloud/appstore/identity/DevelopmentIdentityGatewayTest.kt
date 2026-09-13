package com.goreecloud.appstore.identity

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DevelopmentIdentityGatewayTest {
    @Test
    fun developerDemoCarriesStandardCatalogAudienceAndDevelopmentChannel() {
        val developer = DevelopmentIdentityGateway.availableSessions
            .single { it.subjectId == "dev:developer" }

        assertTrue(developer.isAuthenticated)
        assertTrue("audience:standard" in developer.audiences)
        assertTrue("audience:developer" in developer.audiences)
        assertTrue("channel:development" in developer.audiences)
    }

    @Test
    fun nonDeveloperDemoSessionsDoNotGainDevelopmentChannelImplicitly() {
        val nonDeveloperSessions = DevelopmentIdentityGateway.availableSessions
            .filterNot { it.subjectId == "dev:developer" }

        assertTrue(nonDeveloperSessions.isNotEmpty())
        nonDeveloperSessions.forEach { session ->
            assertFalse(
                "${session.subjectId} must not gain development-channel access implicitly",
                "channel:development" in session.audiences,
            )
        }
    }
}
