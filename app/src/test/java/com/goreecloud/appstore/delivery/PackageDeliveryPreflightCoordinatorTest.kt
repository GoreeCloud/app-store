package com.goreecloud.appstore.delivery

import com.goreecloud.appstore.domain.AccessRule
import com.goreecloud.appstore.domain.IdentitySession
import com.goreecloud.appstore.domain.PackageDeliveryPolicy
import com.goreecloud.appstore.domain.PackageDeliveryPolicy.AcceptanceState
import com.goreecloud.appstore.domain.PackageDeliveryPolicy.Action
import com.goreecloud.appstore.domain.PackageDeliveryPolicy.ArtifactCandidate
import com.goreecloud.appstore.domain.PackageDeliveryPolicy.Blocker
import com.goreecloud.appstore.domain.PackageDeliveryPolicy.Evidence
import com.goreecloud.appstore.domain.PackageDeliveryPolicy.EvidenceEvaluationContext
import com.goreecloud.appstore.domain.PackageDeliveryPolicy.ReleaseEvidence
import com.goreecloud.appstore.domain.PackageDeliveryPolicy.ReleaseEvidenceRecord
import com.goreecloud.appstore.domain.PackageDeliveryPolicy.ReleaseEvidenceType
import com.goreecloud.appstore.domain.ReleaseChannel
import com.goreecloud.appstore.domain.StoreItem
import com.goreecloud.appstore.domain.StoreItemType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageDeliveryPreflightCoordinatorTest {
    private val session = IdentitySession(
        subjectId = "user-1",
        displayName = "User",
        audiences = setOf("channel:stable"),
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

    private val context = EvidenceEvaluationContext(evaluatedAtEpochSeconds = 1_700_000_100L)

    @Test
    fun acceptedPositiveObservationCanMakeUpdateEligibleWithoutPackageMutation() {
        val lookedUp = mutableListOf<String>()
        val coordinator = coordinator { packageName ->
            lookedUp += packageName
            InstalledPackageLookupResult.Installed(packageName, versionCode = 100)
        }

        val result = coordinator.evaluate(
            session = session,
            item = item,
            artifact = artifact,
            evidence = acceptedEvidence(),
            action = Action.UPDATE,
            context = context,
            sdkInt = 35,
        )

        assertEquals(listOf(artifact.packageName), lookedUp)
        assertTrue(result.observation is InstalledPackageObservation.Installed)
        assertTrue(result.decision.eligibleForHandoff)
        assertTrue(result.decision.blockers.isEmpty())
    }

    @Test
    fun unobservedPackageCannotBecomeAcceptedAbsenceForFreshInstall() {
        val coordinator = coordinator {
            InstalledPackageLookupResult.NotObserved
        }

        val result = coordinator.evaluate(
            session = session,
            item = item,
            artifact = artifact,
            evidence = acceptedEvidence(),
            action = Action.INSTALL,
            context = context,
            sdkInt = 35,
        )

        assertTrue(result.observation is InstalledPackageObservation.Unknown)
        assertFalse(result.decision.eligibleForHandoff)
        assertTrue(Blocker.INSTALLATION_STATE_NOT_ACCEPTED in result.decision.blockers)
        assertFalse(Blocker.ALREADY_INSTALLED in result.decision.blockers)
    }

    @Test
    fun unobservedPackageCannotAuthorizeUpdateOrRollback() {
        for (action in listOf(Action.UPDATE, Action.ROLLBACK)) {
            val coordinator = coordinator {
                InstalledPackageLookupResult.NotObserved
            }

            val result = coordinator.evaluate(
                session = session,
                item = item,
                artifact = if (action == Action.ROLLBACK) artifact.copy(versionCode = 90) else artifact,
                evidence = acceptedEvidence(
                    if (action == Action.ROLLBACK) artifact.copy(versionCode = 90) else artifact,
                ),
                action = action,
                context = context,
                sdkInt = 35,
            )

            assertFalse(result.decision.eligibleForHandoff)
            assertTrue(Blocker.INSTALLATION_STATE_NOT_ACCEPTED in result.decision.blockers)
        }
    }

    @Test
    fun policyBlockersPreventInstalledPackageObservation() {
        val lookedUp = mutableListOf<String>()
        val mismatchedArtifact = artifact.copy(packageName = "com.example.other")
        val coordinator = coordinator { packageName ->
            lookedUp += packageName
            InstalledPackageLookupResult.Installed(packageName, versionCode = 100)
        }

        val result = coordinator.evaluate(
            session = session,
            item = item,
            artifact = mismatchedArtifact,
            evidence = acceptedEvidence(mismatchedArtifact),
            action = Action.UPDATE,
            context = context,
            sdkInt = 35,
        )

        assertTrue(lookedUp.isEmpty())
        assertEquals(
            InstalledPackageObservation.Unknown.Reason.POLICY_PRECONDITION_REJECTED,
            (result.observation as InstalledPackageObservation.Unknown).reason,
        )
        assertFalse(result.decision.eligibleForHandoff)
        assertTrue(Blocker.ARTIFACT_PACKAGE_MISMATCH in result.decision.blockers)
        assertTrue(Blocker.INSTALLATION_STATE_NOT_ACCEPTED in result.decision.blockers)
    }

    @Test
    fun rejectedReleaseEvidencePreventsInstalledPackageObservation() {
        val lookedUp = mutableListOf<String>()
        val coordinator = coordinator { packageName ->
            lookedUp += packageName
            InstalledPackageLookupResult.Installed(packageName, versionCode = 100)
        }
        val rejectedEvidence = acceptedEvidence().copy(signature = AcceptanceState.REJECTED)

        val result = coordinator.evaluate(
            session = session,
            item = item,
            artifact = artifact,
            evidence = rejectedEvidence,
            action = Action.UPDATE,
            context = context,
            sdkInt = 35,
        )

        assertTrue(lookedUp.isEmpty())
        assertEquals(
            InstalledPackageObservation.Unknown.Reason.POLICY_PRECONDITION_REJECTED,
            (result.observation as InstalledPackageObservation.Unknown).reason,
        )
        assertTrue(Blocker.SIGNATURE_NOT_ACCEPTED in result.decision.blockers)
    }

    @Test
    fun mismatchedObservedIdentityFailsClosedBeforeHandoff() {
        val coordinator = coordinator {
            InstalledPackageLookupResult.Installed("com.example.other", versionCode = 100)
        }

        val result = coordinator.evaluate(
            session = session,
            item = item,
            artifact = artifact,
            evidence = acceptedEvidence(),
            action = Action.UPDATE,
            context = context,
            sdkInt = 35,
        )

        assertEquals(
            InstalledPackageObservation.Unknown.Reason.RESPONSE_IDENTITY_MISMATCH,
            (result.observation as InstalledPackageObservation.Unknown).reason,
        )
        assertFalse(result.decision.eligibleForHandoff)
        assertTrue(Blocker.INSTALLATION_STATE_NOT_ACCEPTED in result.decision.blockers)
    }

    private fun coordinator(
        lookup: (String) -> InstalledPackageLookupResult,
    ): PackageDeliveryPreflightCoordinator =
        PackageDeliveryPreflightCoordinator(
            InstalledPackageObservationGateway(InstalledPackageLookup(lookup)),
        )

    private fun acceptedEvidence(
        candidate: ArtifactCandidate = artifact,
    ): Evidence = Evidence(
        catalogBinding = AcceptanceState.ACCEPTED,
        digest = AcceptanceState.ACCEPTED,
        signature = AcceptanceState.ACCEPTED,
        wardveil = AcceptanceState.ACCEPTED,
        release = ReleaseEvidence(
            buildProvenance = acceptedRecord(ReleaseEvidenceType.BUILD_PROVENANCE, candidate),
            sbom = acceptedRecord(ReleaseEvidenceType.SBOM, candidate),
            releaseApproval = acceptedRecord(ReleaseEvidenceType.RELEASE_APPROVAL, candidate),
            revocationStatus = acceptedRecord(ReleaseEvidenceType.REVOCATION_STATUS, candidate),
        ),
        rollback = AcceptanceState.ACCEPTED,
    )

    private fun acceptedRecord(
        type: ReleaseEvidenceType,
        candidate: ArtifactCandidate,
    ): ReleaseEvidenceRecord = ReleaseEvidenceRecord(
        type = type,
        state = AcceptanceState.ACCEPTED,
        producerId = "development.release-evidence-fixture",
        authorityDomain = "development.release-evidence",
        producerAuthority = AcceptanceState.ACCEPTED,
        subjectPackageName = candidate.packageName,
        artifactSha256 = candidate.sha256,
        evidenceSetId = "development-release-set-1",
        contractVersion = "development-evidence-v1",
        createdAtEpochSeconds = 1_700_000_000L,
        expiresAtEpochSeconds = 1_700_003_600L,
        sourceReference = "development-fixture:${type.name.lowercase()}",
    )
}
