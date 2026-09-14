package com.goreecloud.appstore.domain

/**
 * Pure pre-handoff policy for package delivery.
 *
 * A positive result means only that the candidate may be handed to a future package-delivery
 * implementation. It does not install, update, downgrade, or roll back a package and does not
 * replace backend re-authorization, Wardveil inspection, Android PackageInstaller acceptance,
 * signing authority, or release acceptance.
 */
object PackageDeliveryPolicy {
    enum class AcceptanceState { ACCEPTED, REJECTED, UNKNOWN }

    enum class Action { INSTALL, UPDATE, ROLLBACK }

    enum class Blocker {
        NOT_AN_APPLICATION,
        RELEASE_CHANNEL_NOT_AUTHORIZED,
        CATALOG_PACKAGE_IDENTITY_MISSING,
        ARTIFACT_PACKAGE_MISMATCH,
        ARTIFACT_VERSION_MISMATCH,
        ARTIFACT_CHANNEL_MISMATCH,
        DEVICE_INCOMPATIBLE,
        CATALOG_BINDING_NOT_ACCEPTED,
        DIGEST_NOT_ACCEPTED,
        SIGNATURE_NOT_ACCEPTED,
        WARDVEIL_NOT_ACCEPTED,
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

    data class Evidence(
        val catalogBinding: AcceptanceState,
        val digest: AcceptanceState,
        val signature: AcceptanceState,
        val wardveil: AcceptanceState,
        val rollback: AcceptanceState = AcceptanceState.UNKNOWN,
    )

    data class Decision(
        val eligibleForHandoff: Boolean,
        val blockers: Set<Blocker>,
    )

    fun evaluate(
        session: IdentitySession,
        item: StoreItem,
        artifact: ArtifactCandidate,
        device: DeviceState,
        evidence: Evidence,
        action: Action,
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
}
