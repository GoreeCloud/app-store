package com.goreecloud.appstore.domain

/**
 * Pure pre-handoff policy for package delivery.
 *
 * A positive result means only that the candidate may be handed to a future package-delivery
 * implementation. It does not install, update, downgrade, or roll back a package and does not
 * replace backend re-authorization, authoritative release-evidence production, Wardveil
 * inspection, Android PackageInstaller acceptance, signing authority, or release acceptance.
 */
object PackageDeliveryPolicy {
    enum class AcceptanceState { ACCEPTED, REJECTED, UNKNOWN }

    enum class Action { INSTALL, UPDATE, ROLLBACK }

    enum class ReleaseEvidenceType {
        BUILD_PROVENANCE,
        SBOM,
        RELEASE_APPROVAL,
        REVOCATION_STATUS,
    }

    enum class Blocker {
        NOT_AN_APPLICATION,
        RELEASE_CHANNEL_NOT_AUTHORIZED,
        CATALOG_PACKAGE_IDENTITY_MISSING,
        ARTIFACT_PACKAGE_MISMATCH,
        ARTIFACT_VERSION_MISMATCH,
        ARTIFACT_CHANNEL_MISMATCH,
        ARTIFACT_DIGEST_IDENTITY_INVALID,
        DEVICE_INCOMPATIBLE,
        CATALOG_BINDING_NOT_ACCEPTED,
        DIGEST_NOT_ACCEPTED,
        SIGNATURE_NOT_ACCEPTED,
        WARDVEIL_NOT_ACCEPTED,
        BUILD_PROVENANCE_NOT_ACCEPTED,
        SBOM_NOT_ACCEPTED,
        RELEASE_APPROVAL_NOT_ACCEPTED,
        REVOCATION_STATUS_NOT_ACCEPTED,
        RELEASE_EVIDENCE_TYPE_MISMATCH,
        RELEASE_EVIDENCE_PRODUCER_IDENTITY_MISSING,
        RELEASE_EVIDENCE_AUTHORITY_DOMAIN_MISSING,
        RELEASE_EVIDENCE_PRODUCER_AUTHORITY_NOT_ACCEPTED,
        RELEASE_EVIDENCE_SUBJECT_SCOPE_MISSING,
        RELEASE_EVIDENCE_SUBJECT_SCOPE_MISMATCH,
        RELEASE_EVIDENCE_CONTRACT_VERSION_MISSING,
        RELEASE_EVIDENCE_SOURCE_REFERENCE_MISSING,
        RELEASE_EVIDENCE_ARTIFACT_DIGEST_MISSING,
        RELEASE_EVIDENCE_ARTIFACT_DIGEST_MISMATCH,
        RELEASE_EVIDENCE_SET_ID_MISSING,
        RELEASE_EVIDENCE_SET_ID_MISMATCH,
        EVIDENCE_EVALUATION_TIME_INVALID,
        RELEASE_EVIDENCE_TIME_INVALID,
        RELEASE_EVIDENCE_EXPIRED,
        INSTALLATION_STATE_NOT_ACCEPTED,
        INSTALLATION_STATE_INCONSISTENT,
        ALREADY_INSTALLED,
        NOT_INSTALLED,
        INSTALLED_PACKAGE_MISMATCH,
        UPDATE_VERSION_NOT_NEWER,
        ROLLBACK_VERSION_NOT_OLDER,
        ROLLBACK_NOT_ACCEPTED,
    }

    data class ArtifactCandidate(
        val packageName: String,
        val versionName: String,
        val versionCode: Long,
        val releaseChannel: ReleaseChannel,
        val minSdk: Int,
        val sha256: String,
    )

    /**
     * Installation state must carry explicit acceptance evidence.
     *
     * Null installed fields are meaningful only when [installationState] is ACCEPTED. An UNKNOWN
     * observation must never be interpreted as a verified package absence because Android package
     * visibility can make an installed application unobservable to this process.
     */
    data class DeviceState(
        val sdkInt: Int,
        val installedPackageName: String? = null,
        val installedVersionCode: Long? = null,
        val installationState: AcceptanceState = AcceptanceState.UNKNOWN,
    ) {
        companion object {
            fun observedAbsent(sdkInt: Int): DeviceState = DeviceState(
                sdkInt = sdkInt,
                installationState = AcceptanceState.ACCEPTED,
            )

            fun observedInstalled(
                sdkInt: Int,
                packageName: String,
                versionCode: Long,
            ): DeviceState = DeviceState(
                sdkInt = sdkInt,
                installedPackageName = packageName,
                installedVersionCode = versionCode,
                installationState = AcceptanceState.ACCEPTED,
            )

            fun unobserved(sdkInt: Int): DeviceState = DeviceState(sdkInt = sdkInt)
        }
    }

    /**
     * One producer-attributed release-evidence result.
     *
     * The App Store does not authenticate the producer or manufacture acceptance. It consumes the
     * explicit [producerAuthority] acceptance supplied by a future governed evidence integration
     * and independently verifies only the local envelope invariants represented here.
     */
    data class ReleaseEvidenceRecord(
        val type: ReleaseEvidenceType,
        val state: AcceptanceState = AcceptanceState.UNKNOWN,
        val producerId: String? = null,
        val authorityDomain: String? = null,
        val producerAuthority: AcceptanceState = AcceptanceState.UNKNOWN,
        val subjectPackageName: String? = null,
        val artifactSha256: String? = null,
        val evidenceSetId: String? = null,
        val contractVersion: String? = null,
        val createdAtEpochSeconds: Long? = null,
        val expiresAtEpochSeconds: Long? = null,
        val sourceReference: String? = null,
    )

    /**
     * Release evidence is intentionally fail-closed and keeps independent producer results.
     *
     * Missing records remain missing rather than becoming implicitly accepted. Future release,
     * provenance, SBOM, revocation, Identity, Mesh, or Wardveil integrations remain responsible for
     * producing and authenticating the underlying evidence and producer authority.
     */
    data class ReleaseEvidence(
        val buildProvenance: ReleaseEvidenceRecord? = null,
        val sbom: ReleaseEvidenceRecord? = null,
        val releaseApproval: ReleaseEvidenceRecord? = null,
        val revocationStatus: ReleaseEvidenceRecord? = null,
    )

    data class EvidenceEvaluationContext(
        val evaluatedAtEpochSeconds: Long,
    )

    data class Evidence(
        val catalogBinding: AcceptanceState,
        val digest: AcceptanceState,
        val signature: AcceptanceState,
        val wardveil: AcceptanceState,
        val release: ReleaseEvidence = ReleaseEvidence(),
        val rollback: AcceptanceState = AcceptanceState.UNKNOWN,
    )

    data class Decision(
        val eligibleForHandoff: Boolean,
        val blockers: Set<Blocker>,
    )

    private val canonicalSha256 = Regex("^[0-9a-f]{64}$")

    fun evaluate(
        session: IdentitySession,
        item: StoreItem,
        artifact: ArtifactCandidate,
        device: DeviceState,
        evidence: Evidence,
        action: Action,
        context: EvidenceEvaluationContext,
    ): Decision {
        val blockers = linkedSetOf<Blocker>()

        if (item.type != StoreItemType.APPLICATION) {
            blockers += Blocker.NOT_AN_APPLICATION
        }
        if (!ReleaseChannelAccess.canAccess(session, item.releaseChannel)) {
            blockers += Blocker.RELEASE_CHANNEL_NOT_AUTHORIZED
        }

        val catalogPackage = item.packageName?.trim().orEmpty()
        if (catalogPackage.isEmpty()) {
            blockers += Blocker.CATALOG_PACKAGE_IDENTITY_MISSING
        } else if (artifact.packageName != catalogPackage) {
            blockers += Blocker.ARTIFACT_PACKAGE_MISMATCH
        }

        val catalogVersion = item.version?.trim().orEmpty()
        if (catalogVersion.isEmpty() || artifact.versionName != catalogVersion) {
            blockers += Blocker.ARTIFACT_VERSION_MISMATCH
        }
        if (artifact.releaseChannel != item.releaseChannel) {
            blockers += Blocker.ARTIFACT_CHANNEL_MISMATCH
        }
        if (!canonicalSha256.matches(artifact.sha256)) {
            blockers += Blocker.ARTIFACT_DIGEST_IDENTITY_INVALID
        }
        if (artifact.minSdk < 1 || device.sdkInt < artifact.minSdk) {
            blockers += Blocker.DEVICE_INCOMPATIBLE
        }

        if (evidence.catalogBinding != AcceptanceState.ACCEPTED) {
            blockers += Blocker.CATALOG_BINDING_NOT_ACCEPTED
        }
        if (evidence.digest != AcceptanceState.ACCEPTED) {
            blockers += Blocker.DIGEST_NOT_ACCEPTED
        }
        if (evidence.signature != AcceptanceState.ACCEPTED) {
            blockers += Blocker.SIGNATURE_NOT_ACCEPTED
        }
        if (evidence.wardveil != AcceptanceState.ACCEPTED) {
            blockers += Blocker.WARDVEIL_NOT_ACCEPTED
        }

        if (context.evaluatedAtEpochSeconds < 0) {
            blockers += Blocker.EVIDENCE_EVALUATION_TIME_INVALID
        }

        validateReleaseEvidenceRecord(
            record = evidence.release.buildProvenance,
            expectedType = ReleaseEvidenceType.BUILD_PROVENANCE,
            notAcceptedBlocker = Blocker.BUILD_PROVENANCE_NOT_ACCEPTED,
            artifact = artifact,
            context = context,
            blockers = blockers,
        )
        validateReleaseEvidenceRecord(
            record = evidence.release.sbom,
            expectedType = ReleaseEvidenceType.SBOM,
            notAcceptedBlocker = Blocker.SBOM_NOT_ACCEPTED,
            artifact = artifact,
            context = context,
            blockers = blockers,
        )
        validateReleaseEvidenceRecord(
            record = evidence.release.releaseApproval,
            expectedType = ReleaseEvidenceType.RELEASE_APPROVAL,
            notAcceptedBlocker = Blocker.RELEASE_APPROVAL_NOT_ACCEPTED,
            artifact = artifact,
            context = context,
            blockers = blockers,
        )
        validateReleaseEvidenceRecord(
            record = evidence.release.revocationStatus,
            expectedType = ReleaseEvidenceType.REVOCATION_STATUS,
            notAcceptedBlocker = Blocker.REVOCATION_STATUS_NOT_ACCEPTED,
            artifact = artifact,
            context = context,
            blockers = blockers,
        )
        validateReleaseEvidenceSet(
            release = evidence.release,
            blockers = blockers,
        )

        val installedName = device.installedPackageName
        val installedCode = device.installedVersionCode
        val installationStateAccepted = device.installationState == AcceptanceState.ACCEPTED
        val installationStateConsistent = (installedName == null) == (installedCode == null)

        if (!installationStateAccepted) {
            blockers += Blocker.INSTALLATION_STATE_NOT_ACCEPTED
        }
        if (!installationStateConsistent) {
            blockers += Blocker.INSTALLATION_STATE_INCONSISTENT
        }

        if (installationStateAccepted && installationStateConsistent) {
            when (action) {
                Action.INSTALL -> {
                    if (installedName != null) {
                        blockers += Blocker.ALREADY_INSTALLED
                    }
                }

                Action.UPDATE -> {
                    if (installedName == null || installedCode == null) {
                        blockers += Blocker.NOT_INSTALLED
                    } else {
                        if (installedName != artifact.packageName) {
                            blockers += Blocker.INSTALLED_PACKAGE_MISMATCH
                        }
                        if (artifact.versionCode <= installedCode) {
                            blockers += Blocker.UPDATE_VERSION_NOT_NEWER
                        }
                    }
                }

                Action.ROLLBACK -> {
                    if (installedName == null || installedCode == null) {
                        blockers += Blocker.NOT_INSTALLED
                    } else {
                        if (installedName != artifact.packageName) {
                            blockers += Blocker.INSTALLED_PACKAGE_MISMATCH
                        }
                        if (artifact.versionCode >= installedCode) {
                            blockers += Blocker.ROLLBACK_VERSION_NOT_OLDER
                        }
                    }
                }
            }
        }

        if (action == Action.ROLLBACK && evidence.rollback != AcceptanceState.ACCEPTED) {
            blockers += Blocker.ROLLBACK_NOT_ACCEPTED
        }

        return Decision(
            eligibleForHandoff = blockers.isEmpty(),
            blockers = blockers,
        )
    }

    private fun validateReleaseEvidenceRecord(
        record: ReleaseEvidenceRecord?,
        expectedType: ReleaseEvidenceType,
        notAcceptedBlocker: Blocker,
        artifact: ArtifactCandidate,
        context: EvidenceEvaluationContext,
        blockers: MutableSet<Blocker>,
    ) {
        if (record == null) {
            blockers += notAcceptedBlocker
            return
        }

        if (record.state != AcceptanceState.ACCEPTED) {
            blockers += notAcceptedBlocker
        }
        if (record.type != expectedType) {
            blockers += Blocker.RELEASE_EVIDENCE_TYPE_MISMATCH
        }
        if (record.producerId.isNullOrBlank()) {
            blockers += Blocker.RELEASE_EVIDENCE_PRODUCER_IDENTITY_MISSING
        }
        if (record.authorityDomain.isNullOrBlank()) {
            blockers += Blocker.RELEASE_EVIDENCE_AUTHORITY_DOMAIN_MISSING
        }
        if (record.producerAuthority != AcceptanceState.ACCEPTED) {
            blockers += Blocker.RELEASE_EVIDENCE_PRODUCER_AUTHORITY_NOT_ACCEPTED
        }

        val subjectPackageName = record.subjectPackageName
        if (subjectPackageName.isNullOrBlank()) {
            blockers += Blocker.RELEASE_EVIDENCE_SUBJECT_SCOPE_MISSING
        } else if (subjectPackageName != artifact.packageName) {
            blockers += Blocker.RELEASE_EVIDENCE_SUBJECT_SCOPE_MISMATCH
        }
        if (record.contractVersion.isNullOrBlank()) {
            blockers += Blocker.RELEASE_EVIDENCE_CONTRACT_VERSION_MISSING
        }
        if (record.sourceReference.isNullOrBlank()) {
            blockers += Blocker.RELEASE_EVIDENCE_SOURCE_REFERENCE_MISSING
        }

        val releaseArtifactSha256 = record.artifactSha256
        if (releaseArtifactSha256.isNullOrBlank()) {
            blockers += Blocker.RELEASE_EVIDENCE_ARTIFACT_DIGEST_MISSING
        } else if (
            !canonicalSha256.matches(releaseArtifactSha256) ||
            releaseArtifactSha256 != artifact.sha256
        ) {
            blockers += Blocker.RELEASE_EVIDENCE_ARTIFACT_DIGEST_MISMATCH
        }

        val createdAt = record.createdAtEpochSeconds
        val expiresAt = record.expiresAtEpochSeconds
        if (
            createdAt == null ||
            expiresAt == null ||
            createdAt < 0 ||
            expiresAt <= createdAt ||
            context.evaluatedAtEpochSeconds < createdAt
        ) {
            blockers += Blocker.RELEASE_EVIDENCE_TIME_INVALID
        } else if (context.evaluatedAtEpochSeconds >= expiresAt) {
            blockers += Blocker.RELEASE_EVIDENCE_EXPIRED
        }
    }

    private fun validateReleaseEvidenceSet(
        release: ReleaseEvidence,
        blockers: MutableSet<Blocker>,
    ) {
        val records = listOf(
            release.buildProvenance,
            release.sbom,
            release.releaseApproval,
            release.revocationStatus,
        )

        if (records.any { record -> record?.evidenceSetId.isNullOrBlank() }) {
            blockers += Blocker.RELEASE_EVIDENCE_SET_ID_MISSING
        }

        val evidenceSetIds = records
            .mapNotNull { record -> record?.evidenceSetId?.takeIf { it.isNotBlank() } }
            .toSet()
        if (evidenceSetIds.size > 1) {
            blockers += Blocker.RELEASE_EVIDENCE_SET_ID_MISMATCH
        }
    }
}
