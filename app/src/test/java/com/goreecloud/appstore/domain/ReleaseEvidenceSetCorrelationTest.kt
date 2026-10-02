package com.goreecloud.appstore.domain

import com.goreecloud.appstore.domain.PackageDeliveryPolicy.AcceptanceState
import com.goreecloud.appstore.domain.PackageDeliveryPolicy.Action
import com.goreecloud.appstore.domain.PackageDeliveryPolicy.ArtifactCandidate
import com.goreecloud.appstore.domain.PackageDeliveryPolicy.Blocker
import com.goreecloud.appstore.domain.PackageDeliveryPolicy.DeviceState
import com.goreecloud.appstore.domain.PackageDeliveryPolicy.Evidence
import com.goreecloud.appstore.domain.PackageDeliveryPolicy.EvidenceEvaluationContext
import com.goreecloud.appstore.domain.PackageDeliveryPolicy.ReleaseEvidence
import com.goreecloud.appstore.domain.PackageDeliveryPolicy.ReleaseEvidenceRecord
import com.goreecloud.appstore.domain.PackageDeliveryPolicy.ReleaseEvidenceType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseEvidenceSetCorrelationTest {
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

    private val context = EvidenceEvaluationContext(
        evaluatedAtEpochSeconds = 1_700_000_100L,
    )

    private fun acceptedRecord(
        type: ReleaseEvidenceType,
        evidenceSetId: String? = "development-release-set-1",
    ): ReleaseEvidenceRecord = ReleaseEvidenceRecord(
        type = type,
        state = AcceptanceState.ACCEPTED,
        producerId = "development.release-evidence-fixture",
        authorityDomain = "development.release-evidence",
        producerAuthority = AcceptanceState.ACCEPTED,
        subjectPackageName = artifact.packageName,
        artifactSha256 = artifact.sha256,
        evidenceSetId = evidenceSetId,
        contractVersion = "development-evidence-v1",
        createdAtEpochSeconds = 1_700_000_000L,
        expiresAtEpochSeconds = 1_700_003_600L,
        sourceReference = "development-fixture:${type.name.lowercase()}",
    )

    private fun acceptedRelease(): ReleaseEvidence = ReleaseEvidence(
        buildProvenance = acceptedRecord(ReleaseEvidenceType.BUILD_PROVENANCE),
        sbom = acceptedRecord(ReleaseEvidenceType.SBOM),
        releaseApproval = acceptedRecord(ReleaseEvidenceType.RELEASE_APPROVAL),
        revocationStatus = acceptedRecord(ReleaseEvidenceType.REVOCATION_STATUS),
    )

    private fun evidence(release: ReleaseEvidence): Evidence = Evidence(
        catalogBinding = AcceptanceState.ACCEPTED,
        digest = AcceptanceState.ACCEPTED,
        signature = AcceptanceState.ACCEPTED,
        wardveil = AcceptanceState.ACCEPTED,
        release = release,
        rollback = AcceptanceState.ACCEPTED,
    )

    private fun evaluate(release: ReleaseEvidence) = PackageDeliveryPolicy.evaluate(
        session = session,
        item = item,
        artifact = artifact,
        device = DeviceState.observedAbsent(sdkInt = 35),
        evidence = evidence(release),
        action = Action.INSTALL,
        context = context,
    )

    @Test
    fun oneCorrelatedEvidenceSetCanPass() {
        val decision = evaluate(acceptedRelease())

        assertTrue(decision.eligibleForHandoff)
        assertTrue(decision.blockers.isEmpty())
    }

    @Test
    fun missingEvidenceSetIdentityFailsClosed() {
        val release = acceptedRelease().copy(
            buildProvenance = acceptedRecord(
                ReleaseEvidenceType.BUILD_PROVENANCE,
                evidenceSetId = null,
            ),
        )

        val decision = evaluate(release)

        assertFalse(decision.eligibleForHandoff)
        assertTrue(Blocker.RELEASE_EVIDENCE_SET_ID_MISSING in decision.blockers)
    }

    @Test
    fun recordsFromDifferentEvidenceSetsCannotBeMixed() {
        val release = acceptedRelease().copy(
            sbom = acceptedRecord(
                ReleaseEvidenceType.SBOM,
                evidenceSetId = "development-release-set-2",
            ),
        )

        val decision = evaluate(release)

        assertFalse(decision.eligibleForHandoff)
        assertTrue(Blocker.RELEASE_EVIDENCE_SET_ID_MISMATCH in decision.blockers)
    }
}
