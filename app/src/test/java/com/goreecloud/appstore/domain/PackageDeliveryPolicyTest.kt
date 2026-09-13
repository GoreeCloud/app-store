package com.goreecloud.appstore.domain

import com.goreecloud.appstore.domain.PackageDeliveryPolicy.AcceptanceState
import com.goreecloud.appstore.domain.PackageDeliveryPolicy.Action
import com.goreecloud.appstore.domain.PackageDeliveryPolicy.ArtifactCandidate
import com.goreecloud.appstore.domain.PackageDeliveryPolicy.Blocker
import com.goreecloud.appstore.domain.PackageDeliveryPolicy.DeviceState
import com.goreecloud.appstore.domain.PackageDeliveryPolicy.Evidence
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PackageDeliveryPolicyTest {
    private val session = IdentitySession(
        subjectId = "user-1",
        displayName = "User",
        audiences = setOf("channel:stable", "channel:beta"),
        isAuthenticated = true,
    )

    private val item = StoreItem(
        id = "browser",
        name = "GoreeCloud Browser",
        summary = "Browser",
        type = StoreItemType.APPLICATION,
        category = "Internet",
        version = "1.2.3",
        releaseChannel = ReleaseChannel.STABLE,
        packageName = "com.goreecloud.browser",
        serviceUrl = null,
        accessRule = AccessRule(),
    )

    private val artifact = ArtifactCandidate(
        packageName = "com.goreecloud.browser",
        versionName = "1.2.3",
        versionCode = 123,
        releaseChannel = ReleaseChannel.STABLE,
        minSdk = 29,
    )

    private val accepted = Evidence(
        catalogBinding = AcceptanceState.ACCEPTED,
        digest = AcceptanceState.ACCEPTED,
        signature = AcceptanceState.ACCEPTED,
        wardveil = AcceptanceState.ACCEPTED,
        rollback = AcceptanceState.ACCEPTED,
    )

    @Test
    fun installIsEligibleOnlyWhenEveryRequiredFactIsAccepted() {
        val decision = PackageDeliveryPolicy.evaluate(
            session = session,
            item = item,
            artifact = artifact,
            device = DeviceState(sdkInt = 35),
            evidence = accepted,
            action = Action.INSTALL,
        )

        assertTrue(decision.eligibleForHandoff)
        assertTrue(decision.blockers.isEmpty())
    }

    @Test
    fun unknownOrRejectedTrustEvidenceFailsClosed() {
        val fields = listOf(
            accepted.copy(catalogBinding = AcceptanceState.UNKNOWN) to Blocker.CATALOG_BINDING_NOT_ACCEPTED,
            accepted.copy(digest = AcceptanceState.REJECTED) to Blocker.DIGEST_NOT_ACCEPTED,
            accepted.copy(signature = AcceptanceState.UNKNOWN) to Blocker.SIGNATURE_NOT_ACCEPTED,
            accepted.copy(wardveil = AcceptanceState.REJECTED) to Blocker.WARDVEIL_NOT_ACCEPTED,
        )

        fields.forEach { (evidence, blocker) ->
            val decision = PackageDeliveryPolicy.evaluate(
                session,
                item,
                artifact,
                DeviceState(sdkInt = 35),
                evidence,
                Action.INSTALL,
            )
            assertFalse(decision.eligibleForHandoff)
            assertTrue(blocker in decision.blockers)
        }
    }

    @Test
    fun unauthorizedReleaseChannelCannotBeHandedOff() {
        val rcItem = item.copy(releaseChannel = ReleaseChannel.RC)
        val rcArtifact = artifact.copy(releaseChannel = ReleaseChannel.RC)

        val decision = PackageDeliveryPolicy.evaluate(
            session,
            rcItem,
            rcArtifact,
            DeviceState(sdkInt = 35),
            accepted,
            Action.INSTALL,
        )

        assertFalse(decision.eligibleForHandoff)
        assertTrue(Blocker.RELEASE_CHANNEL_NOT_AUTHORIZED in decision.blockers)
    }

    @Test
    fun catalogArtifactIdentityMustMatchExactly() {
        val mismatch = PackageDeliveryPolicy.evaluate(
            session,
            item,
            artifact.copy(packageName = "com.example.other", versionName = "9.9.9", releaseChannel = ReleaseChannel.BETA),
            DeviceState(sdkInt = 35),
            accepted,
            Action.INSTALL,
        )

        assertFalse(mismatch.eligibleForHandoff)
        assertTrue(Blocker.ARTIFACT_PACKAGE_MISMATCH in mismatch.blockers)
        assertTrue(Blocker.ARTIFACT_VERSION_MISMATCH in mismatch.blockers)
        assertTrue(Blocker.ARTIFACT_CHANNEL_MISMATCH in mismatch.blockers)
    }

    @Test
    fun incompatibleDeviceFailsClosed() {
        val decision = PackageDeliveryPolicy.evaluate(
            session,
            item,
            artifact.copy(minSdk = 36),
            DeviceState(sdkInt = 35),
            accepted,
            Action.INSTALL,
        )

        assertFalse(decision.eligibleForHandoff)
        assertTrue(Blocker.DEVICE_INCOMPATIBLE in decision.blockers)
    }

    @Test
    fun installRefusesAlreadyInstalledState() {
        val decision = PackageDeliveryPolicy.evaluate(
            session,
            item,
            artifact,
            DeviceState(
                sdkInt = 35,
                installedPackageName = artifact.packageName,
                installedVersionCode = 100,
            ),
            accepted,
            Action.INSTALL,
        )

        assertFalse(decision.eligibleForHandoff)
        assertTrue(Blocker.ALREADY_INSTALLED in decision.blockers)
    }

    @Test
    fun updateRequiresSamePackageAndStrictlyNewerVersion() {
        val oldOrSame = PackageDeliveryPolicy.evaluate(
            session,
            item,
            artifact.copy(versionCode = 100),
            DeviceState(35, artifact.packageName, 100),
            accepted,
            Action.UPDATE,
        )
        assertFalse(oldOrSame.eligibleForHandoff)
        assertTrue(Blocker.UPDATE_VERSION_NOT_NEWER in oldOrSame.blockers)

        val wrongPackage = PackageDeliveryPolicy.evaluate(
            session,
            item,
            artifact,
            DeviceState(35, "com.example.other", 100),
            accepted,
            Action.UPDATE,
        )
        assertFalse(wrongPackage.eligibleForHandoff)
        assertTrue(Blocker.INSTALLED_PACKAGE_MISMATCH in wrongPackage.blockers)

        val acceptedUpdate = PackageDeliveryPolicy.evaluate(
            session,
            item,
            artifact,
            DeviceState(35, artifact.packageName, 100),
            accepted,
            Action.UPDATE,
        )
        assertTrue(acceptedUpdate.eligibleForHandoff)
    }

    @Test
    fun rollbackRequiresOlderVersionAndExplicitRollbackAcceptance() {
        val missingAcceptance = PackageDeliveryPolicy.evaluate(
            session,
            item,
            artifact.copy(versionCode = 90),
            DeviceState(35, artifact.packageName, 100),
            accepted.copy(rollback = AcceptanceState.UNKNOWN),
            Action.ROLLBACK,
        )
        assertFalse(missingAcceptance.eligibleForHandoff)
        assertTrue(Blocker.ROLLBACK_NOT_ACCEPTED in missingAcceptance.blockers)

        val notOlder = PackageDeliveryPolicy.evaluate(
            session,
            item,
            artifact.copy(versionCode = 100),
            DeviceState(35, artifact.packageName, 100),
            accepted,
            Action.ROLLBACK,
        )
        assertFalse(notOlder.eligibleForHandoff)
        assertTrue(Blocker.ROLLBACK_VERSION_NOT_OLDER in notOlder.blockers)

        val acceptedRollback = PackageDeliveryPolicy.evaluate(
            session,
            item,
            artifact.copy(versionCode = 90),
            DeviceState(35, artifact.packageName, 100),
            accepted,
            Action.ROLLBACK,
        )
        assertTrue(acceptedRollback.eligibleForHandoff)
    }

    @Test
    fun servicesNeverEnterPackageDelivery() {
        val service = item.copy(
            type = StoreItemType.SERVICE,
            packageName = null,
            serviceUrl = "https://service.example",
        )

        val decision = PackageDeliveryPolicy.evaluate(
            session,
            service,
            artifact,
            DeviceState(sdkInt = 35),
            accepted,
            Action.INSTALL,
        )

        assertFalse(decision.eligibleForHandoff)
        assertTrue(Blocker.NOT_AN_APPLICATION in decision.blockers)
        assertTrue(Blocker.CATALOG_PACKAGE_IDENTITY_MISSING in decision.blockers)
    }
}
