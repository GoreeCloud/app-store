package com.goreecloud.appstore.domain

import com.goreecloud.appstore.domain.PackageDeliveryPolicy.AcceptanceState
import com.goreecloud.appstore.domain.PackageDeliveryPolicy.Action
import com.goreecloud.appstore.domain.PackageDeliveryPolicy.ArtifactCandidate
import com.goreecloud.appstore.domain.PackageDeliveryPolicy.Blocker
import com.goreecloud.appstore.domain.PackageDeliveryPolicy.DeviceState
import com.goreecloud.appstore.domain.PackageDeliveryPolicy.Evidence
import com.goreecloud.appstore.domain.PackageDeliveryPolicy.ReleaseEvidence
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
        sha256 = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
    )

    private val accepted = Evidence(
        catalogBinding = AcceptanceState.ACCEPTED,
        digest = AcceptanceState.ACCEPTED,
        signature = AcceptanceState.ACCEPTED,
        wardveil = AcceptanceState.ACCEPTED,
        release = ReleaseEvidence.acceptedFor(artifact),
        rollback = AcceptanceState.ACCEPTED,
    )

    @Test
    fun installIsEligibleOnlyWhenEveryRequiredFactIncludingAbsenceIsAccepted() {
        val decision = PackageDeliveryPolicy.evaluate(
            session = session,
            item = item,
            artifact = artifact,
            device = DeviceState.observedAbsent(sdkInt = 35),
            evidence = accepted,
            action = Action.INSTALL,
        )

        assertTrue(decision.eligibleForHandoff)
        assertTrue(decision.blockers.isEmpty())
    }

    @Test
    fun unknownInstallationStateCannotBeTreatedAsVerifiedAbsence() {
        val decision = PackageDeliveryPolicy.evaluate(
            session = session,
            item = item,
            artifact = artifact,
            device = DeviceState.unobserved(sdkInt = 35),
            evidence = accepted,
            action = Action.INSTALL,
        )

        assertFalse(decision.eligibleForHandoff)
        assertTrue(Blocker.INSTALLATION_STATE_NOT_ACCEPTED in decision.blockers)
        assertFalse(Blocker.ALREADY_INSTALLED in decision.blockers)
    }

    @Test
    fun partialInstallationStateIsRejectedAsInconsistent() {
        val decision = PackageDeliveryPolicy.evaluate(
            session = session,
            item = item,
            artifact = artifact,
            device = DeviceState(
                sdkInt = 35,
                installedPackageName = artifact.packageName,
                installedVersionCode = null,
                installationState = AcceptanceState.ACCEPTED,
            ),
            evidence = accepted,
            action = Action.UPDATE,
        )

        assertFalse(decision.eligibleForHandoff)
        assertTrue(Blocker.INSTALLATION_STATE_INCONSISTENT in decision.blockers)
    }

    @Test
    fun unknownOrRejectedTrustEvidenceFailsClosed() {
        val fields = listOf(
            accepted.copy(catalogBinding = AcceptanceState.UNKNOWN) to Blocker.CATALOG_BINDING_NOT_ACCEPTED,
            accepted.copy(digest = AcceptanceState.REJECTED) to Blocker.DIGEST_NOT_ACCEPTED,
            accepted.copy(signature = AcceptanceState.UNKNOWN) to Blocker.SIGNATURE_NOT_ACCEPTED,
            accepted.copy(wardveil = AcceptanceState.REJECTED) to Blocker.WARDVEIL_NOT_ACCEPTED,
            accepted.copy(
                release = accepted.release.copy(buildProvenance = AcceptanceState.UNKNOWN),
            ) to Blocker.BUILD_PROVENANCE_NOT_ACCEPTED,
            accepted.copy(
                release = accepted.release.copy(sbom = AcceptanceState.REJECTED),
            ) to Blocker.SBOM_NOT_ACCEPTED,
            accepted.copy(
                release = accepted.release.copy(releaseApproval = AcceptanceState.UNKNOWN),
            ) to Blocker.RELEASE_APPROVAL_NOT_ACCEPTED,
            accepted.copy(
                release = accepted.release.copy(revocationStatus = AcceptanceState.REJECTED),
            ) to Blocker.REVOCATION_STATUS_NOT_ACCEPTED,
        )

        fields.forEach { (evidence, blocker) ->
            val decision = PackageDeliveryPolicy.evaluate(
                session,
                item,
                artifact,
                DeviceState.observedAbsent(sdkInt = 35),
                evidence,
                Action.INSTALL,
            )
            assertFalse(decision.eligibleForHandoff)
            assertTrue(blocker in decision.blockers)
        }
    }

    @Test
    fun missingReleaseEvidenceDefaultsUnknownAndFailsClosed() {
        val missingReleaseEvidence = Evidence(
            catalogBinding = AcceptanceState.ACCEPTED,
            digest = AcceptanceState.ACCEPTED,
            signature = AcceptanceState.ACCEPTED,
            wardveil = AcceptanceState.ACCEPTED,
        )

        val decision = PackageDeliveryPolicy.evaluate(
            session,
            item,
            artifact,
            DeviceState.observedAbsent(sdkInt = 35),
            missingReleaseEvidence,
            Action.INSTALL,
        )

        assertFalse(decision.eligibleForHandoff)
        assertTrue(Blocker.BUILD_PROVENANCE_NOT_ACCEPTED in decision.blockers)
        assertTrue(Blocker.SBOM_NOT_ACCEPTED in decision.blockers)
        assertTrue(Blocker.RELEASE_APPROVAL_NOT_ACCEPTED in decision.blockers)
        assertTrue(Blocker.REVOCATION_STATUS_NOT_ACCEPTED in decision.blockers)
        assertTrue(Blocker.RELEASE_EVIDENCE_ARTIFACT_DIGEST_MISSING in decision.blockers)
    }

    @Test
    fun invalidArtifactDigestIdentityFailsClosed() {
        val invalidArtifact = artifact.copy(
            sha256 = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
        )
        val decision = PackageDeliveryPolicy.evaluate(
            session,
            item,
            invalidArtifact,
            DeviceState.observedAbsent(sdkInt = 35),
            accepted.copy(release = ReleaseEvidence.acceptedFor(invalidArtifact)),
            Action.INSTALL,
        )

        assertFalse(decision.eligibleForHandoff)
        assertTrue(Blocker.ARTIFACT_DIGEST_IDENTITY_INVALID in decision.blockers)
        assertTrue(Blocker.RELEASE_EVIDENCE_ARTIFACT_DIGEST_MISMATCH in decision.blockers)
    }

    @Test
    fun releaseEvidenceRequiresBoundArtifactDigest() {
        val decision = PackageDeliveryPolicy.evaluate(
            session,
            item,
            artifact,
            DeviceState.observedAbsent(sdkInt = 35),
            accepted.copy(release = accepted.release.copy(artifactSha256 = null)),
            Action.INSTALL,
        )

        assertFalse(decision.eligibleForHandoff)
        assertTrue(Blocker.RELEASE_EVIDENCE_ARTIFACT_DIGEST_MISSING in decision.blockers)
    }

    @Test
    fun releaseEvidenceCannotBeReusedForDifferentArtifact() {
        val differentArtifact = artifact.copy(
            sha256 = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
        )
        val decision = PackageDeliveryPolicy.evaluate(
            session,
            item,
            differentArtifact,
            DeviceState.observedAbsent(sdkInt = 35),
            accepted,
            Action.INSTALL,
        )

        assertFalse(decision.eligibleForHandoff)
        assertTrue(Blocker.RELEASE_EVIDENCE_ARTIFACT_DIGEST_MISMATCH in decision.blockers)
    }

    @Test
    fun unauthorizedReleaseChannelCannotBeHandedOff() {
        val rcItem = item.copy(releaseChannel = ReleaseChannel.RC)
        val rcArtifact = artifact.copy(releaseChannel = ReleaseChannel.RC)

        val decision = PackageDeliveryPolicy.evaluate(
            session,
            rcItem,
            rcArtifact,
            DeviceState.observedAbsent(sdkInt = 35),
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
            DeviceState.observedAbsent(sdkInt = 35),
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
            DeviceState.observedAbsent(sdkInt = 35),
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
            DeviceState.observedInstalled(
                sdkInt = 35,
                packageName = artifact.packageName,
                versionCode = 100,
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
            DeviceState.observedInstalled(35, artifact.packageName, 100),
            accepted,
            Action.UPDATE,
        )
        assertFalse(oldOrSame.eligibleForHandoff)
        assertTrue(Blocker.UPDATE_VERSION_NOT_NEWER in oldOrSame.blockers)

        val wrongPackage = PackageDeliveryPolicy.evaluate(
            session,
            item,
            artifact,
            DeviceState.observedInstalled(35, "com.example.other", 100),
            accepted,
            Action.UPDATE,
        )
        assertFalse(wrongPackage.eligibleForHandoff)
        assertTrue(Blocker.INSTALLED_PACKAGE_MISMATCH in wrongPackage.blockers)

        val acceptedUpdate = PackageDeliveryPolicy.evaluate(
            session,
            item,
            artifact,
            DeviceState.observedInstalled(35, artifact.packageName, 100),
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
            DeviceState.observedInstalled(35, artifact.packageName, 100),
            accepted.copy(rollback = AcceptanceState.UNKNOWN),
            Action.ROLLBACK,
        )
        assertFalse(missingAcceptance.eligibleForHandoff)
        assertTrue(Blocker.ROLLBACK_NOT_ACCEPTED in missingAcceptance.blockers)

        val notOlder = PackageDeliveryPolicy.evaluate(
            session,
            item,
            artifact.copy(versionCode = 100),
            DeviceState.observedInstalled(35, artifact.packageName, 100),
            accepted,
            Action.ROLLBACK,
        )
        assertFalse(notOlder.eligibleForHandoff)
        assertTrue(Blocker.ROLLBACK_VERSION_NOT_OLDER in notOlder.blockers)

        val acceptedRollback = PackageDeliveryPolicy.evaluate(
            session,
            item,
            artifact.copy(versionCode = 90),
            DeviceState.observedInstalled(35, artifact.packageName, 100),
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
            DeviceState.observedAbsent(sdkInt = 35),
            accepted,
            Action.INSTALL,
        )

        assertFalse(decision.eligibleForHandoff)
        assertTrue(Blocker.NOT_AN_APPLICATION in decision.blockers)
        assertTrue(Blocker.CATALOG_PACKAGE_IDENTITY_MISSING in decision.blockers)
    }
}
