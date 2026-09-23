package com.goreecloud.appstore.delivery

import com.goreecloud.appstore.domain.IdentitySession
import com.goreecloud.appstore.domain.PackageDeliveryPolicy
import com.goreecloud.appstore.domain.StoreItem

/**
 * Read-only package-delivery preflight composition.
 *
 * This coordinator observes one exact package identity and feeds only that observation into the
 * pure PackageDeliveryPolicy. It never downloads package bytes, invokes PackageInstaller, mutates
 * installed packages, or manufactures accepted package absence.
 */
class PackageDeliveryPreflightCoordinator(
    private val installedPackageObservationGateway: InstalledPackageObservationGateway,
) {
    data class Result(
        val observation: InstalledPackageObservation,
        val decision: PackageDeliveryPolicy.Decision,
    )

    fun evaluate(
        session: IdentitySession,
        item: StoreItem,
        artifact: PackageDeliveryPolicy.ArtifactCandidate,
        evidence: PackageDeliveryPolicy.Evidence,
        action: PackageDeliveryPolicy.Action,
        context: PackageDeliveryPolicy.EvidenceEvaluationContext,
        sdkInt: Int,
    ): Result {
        // Run the pure policy once without any installed-package observation. If anything other
        // than installation state already blocks the candidate, do not touch PackageManager at all.
        // This prevents an unauthorized, stale, malformed, or otherwise rejected candidate from
        // using the preflight path as an installed-package presence probe.
        val preliminaryDecision = PackageDeliveryPolicy.evaluate(
            session = session,
            item = item,
            artifact = artifact,
            device = PackageDeliveryPolicy.DeviceState.unobserved(sdkInt),
            evidence = evidence,
            action = action,
            context = context,
        )
        val nonDeviceBlockers = preliminaryDecision.blockers -
            PackageDeliveryPolicy.Blocker.INSTALLATION_STATE_NOT_ACCEPTED
        if (nonDeviceBlockers.isNotEmpty()) {
            return Result(
                observation = InstalledPackageObservation.Unknown(
                    InstalledPackageObservation.Unknown.Reason.POLICY_PRECONDITION_REJECTED,
                ),
                decision = preliminaryDecision,
            )
        }

        val observation = installedPackageObservationGateway.observe(artifact.packageName)
        val deviceState = installedPackageObservationGateway.toDeviceState(
            sdkInt = sdkInt,
            observation = observation,
        )
        return Result(
            observation = observation,
            decision = PackageDeliveryPolicy.evaluate(
                session = session,
                item = item,
                artifact = artifact,
                device = deviceState,
                evidence = evidence,
                action = action,
                context = context,
            ),
        )
    }
}
