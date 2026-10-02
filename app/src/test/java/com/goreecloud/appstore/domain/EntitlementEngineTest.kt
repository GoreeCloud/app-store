package com.goreecloud.appstore.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EntitlementEngineTest {
    @Test
    fun signedOutSessionCannotViewSignedInEntry() {
        val session = IdentitySession("anonymous", "Signed out", emptySet(), false)
        val rule = AccessRule(requireSignedIn = true)

        assertFalse(EntitlementEngine.canView(session, rule))
    }

    @Test
    fun matchingAudienceCanViewEntry() {
        val session = IdentitySession(
            "subject",
            "User",
            setOf("audience:standard"),
            true,
        )
        val rule = AccessRule(anyAudience = setOf("audience:standard"))

        assertTrue(EntitlementEngine.canView(session, rule))
    }

    @Test
    fun administratorDoesNotBypassAudienceRulesImplicitly() {
        val session = IdentitySession(
            "admin",
            "Administrator",
            setOf("audience:administrator"),
            true,
        )
        val rule = AccessRule(anyAudience = setOf("audience:developer"))

        assertFalse(EntitlementEngine.canView(session, rule))
    }
    @Test
    fun developerDevelopmentFixtureCanBrowseStandardCatalogWithoutBypassingOtherRules() {
        val developer = com.goreecloud.appstore.identity.DevelopmentIdentityGateway.availableSessions
            .first { it.subjectId == "dev:developer" }

        assertTrue("audience:standard" in developer.audiences)
        assertTrue("audience:developer" in developer.audiences)
        assertTrue(
            EntitlementEngine.canView(
                developer,
                AccessRule(anyAudience = setOf("audience:standard")),
            ),
        )
        assertFalse(
            EntitlementEngine.canView(
                developer,
                AccessRule(anyAudience = setOf("audience:administrator")),
            ),
        )
    }

}
