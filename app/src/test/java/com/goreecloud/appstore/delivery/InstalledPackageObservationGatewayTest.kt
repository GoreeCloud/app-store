package com.goreecloud.appstore.delivery

import com.goreecloud.appstore.domain.PackageDeliveryPolicy.AcceptanceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InstalledPackageObservationGatewayTest {
    private class FakeLookup(
        private val result: InstalledPackageLookupResult,
    ) : InstalledPackageLookup {
        var lookupCount: Int = 0
        var lastPackageName: String? = null

        override fun lookup(packageName: String): InstalledPackageLookupResult {
            lookupCount += 1
            lastPackageName = packageName
            return result
        }
    }

    @Test
    fun installedPackageObservationPreservesExactIdentityAndVersion() {
        val platform = FakeLookup(
            InstalledPackageLookupResult.Installed("com.goreecloud.browser", 123),
        )
        val gateway = InstalledPackageObservationGateway(platform)

        val observation = gateway.observe("com.goreecloud.browser")

        assertEquals(
            InstalledPackageObservation.Installed("com.goreecloud.browser", 123),
            observation,
        )
        assertEquals(1, platform.lookupCount)
        assertEquals("com.goreecloud.browser", platform.lastPackageName)

        val device = gateway.toDeviceState(35, observation)
        assertEquals(AcceptanceState.ACCEPTED, device.installationState)
        assertEquals("com.goreecloud.browser", device.installedPackageName)
        assertEquals(123L, device.installedVersionCode)
    }

    @Test
    fun notObservedNeverBecomesAcceptedAbsence() {
        val gateway = InstalledPackageObservationGateway(
            FakeLookup(InstalledPackageLookupResult.NotObserved),
        )

        val observation = gateway.observe("com.goreecloud.browser")
        assertEquals(
            InstalledPackageObservation.Unknown.Reason.NOT_FOUND_OR_NOT_VISIBLE,
            (observation as InstalledPackageObservation.Unknown).reason,
        )

        val device = gateway.toDeviceState(35, observation)
        assertEquals(AcceptanceState.UNKNOWN, device.installationState)
        assertEquals(null, device.installedPackageName)
        assertEquals(null, device.installedVersionCode)
    }

    @Test
    fun malformedPackageIdentityFailsBeforePlatformLookup() {
        val platform = FakeLookup(InstalledPackageLookupResult.NotObserved)
        val gateway = InstalledPackageObservationGateway(platform)

        listOf(
            "",
            "com",
            " com.goreecloud.browser",
            "com.goreecloud.browser ",
            "com/goreecloud/browser",
            "com..browser",
            "com.goreecloud.browser\n",
        ).forEach { packageName ->
            val observation = gateway.observe(packageName)
            assertEquals(
                InstalledPackageObservation.Unknown.Reason.INVALID_PACKAGE_IDENTITY,
                (observation as InstalledPackageObservation.Unknown).reason,
            )
        }

        assertEquals(0, platform.lookupCount)
    }

    @Test
    fun mismatchedPlatformIdentityAndInvalidVersionFailClosed() {
        val mismatch = InstalledPackageObservationGateway(
            FakeLookup(InstalledPackageLookupResult.Installed("com.example.other", 123)),
        ).observe("com.goreecloud.browser")
        assertEquals(
            InstalledPackageObservation.Unknown.Reason.RESPONSE_IDENTITY_MISMATCH,
            (mismatch as InstalledPackageObservation.Unknown).reason,
        )

        val invalidVersion = InstalledPackageObservationGateway(
            FakeLookup(InstalledPackageLookupResult.Installed("com.goreecloud.browser", -1)),
        ).observe("com.goreecloud.browser")
        assertEquals(
            InstalledPackageObservation.Unknown.Reason.INVALID_VERSION_CODE,
            (invalidVersion as InstalledPackageObservation.Unknown).reason,
        )
    }

    @Test
    fun platformFailureRemainsUnknown() {
        val observation = InstalledPackageObservationGateway(
            FakeLookup(InstalledPackageLookupResult.Failed("example.Failure")),
        ).observe("com.goreecloud.browser")

        assertTrue(observation is InstalledPackageObservation.Unknown)
        assertEquals(
            InstalledPackageObservation.Unknown.Reason.PLATFORM_FAILURE,
            (observation as InstalledPackageObservation.Unknown).reason,
        )
    }
}
