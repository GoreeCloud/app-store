package com.goreecloud.appstore.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseChannelAccessTest {
    @Test
    fun signedOutSessionCannotAccessAnyChannel() {
        val session = IdentitySession("anonymous", "Signed out", emptySet(), false)

        assertTrue(ReleaseChannelAccess.visibleChannels(session).isEmpty())
    }

    @Test
    fun stableOnlyClaimDoesNotGrantPrereleaseChannels() {
        val session = IdentitySession(
            "standard",
            "Standard",
            setOf("audience:standard", "channel:stable"),
            true,
        )

        assertTrue(ReleaseChannelAccess.canAccess(session, ReleaseChannel.STABLE))
        assertFalse(ReleaseChannelAccess.canAccess(session, ReleaseChannel.BETA))
        assertFalse(ReleaseChannelAccess.canAccess(session, ReleaseChannel.RC))
        assertFalse(ReleaseChannelAccess.canAccess(session, ReleaseChannel.DEVELOPMENT))
        assertFalse(ReleaseChannelAccess.canAccess(session, ReleaseChannel.DEBUG))
    }

    @Test
    fun administratorRoleAloneDoesNotBypassChannelPolicy() {
        val session = IdentitySession(
            "admin",
            "Administrator",
            setOf("audience:administrator", "channel:stable"),
            true,
        )

        assertFalse(ReleaseChannelAccess.canAccess(session, ReleaseChannel.BETA))
        assertFalse(ReleaseChannelAccess.canAccess(session, ReleaseChannel.DEBUG))
    }

    @Test
    fun previewClaimsExposeStableBetaAndRcOnly() {
        val session = IdentitySession(
            "preview",
            "Preview",
            setOf("channel:stable", "channel:beta", "channel:rc"),
            true,
        )

        assertEquals(
            setOf(ReleaseChannel.STABLE, ReleaseChannel.RC, ReleaseChannel.BETA),
            ReleaseChannelAccess.visibleChannels(session),
        )
    }

    @Test
    fun developerClaimsCanExplicitlyExposeAllChannels() {
        val session = IdentitySession(
            "developer",
            "Developer",
            setOf(
                "channel:stable",
                "channel:rc",
                "channel:beta",
                "channel:development",
                "channel:debug",
            ),
            true,
        )

        ReleaseChannel.entries.forEach { channel ->
            assertTrue(ReleaseChannelAccess.canAccess(session, channel))
        }
    }
}
