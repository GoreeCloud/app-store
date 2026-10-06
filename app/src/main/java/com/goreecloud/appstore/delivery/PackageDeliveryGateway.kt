package com.goreecloud.appstore.delivery

import com.goreecloud.appstore.domain.IdentitySession
import com.goreecloud.appstore.domain.StoreItem

data class DevelopmentDeliveryRelease(
    val storeItemId: String,
    val artifactId: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val releaseChannel: String,
    val minSdk: Int,
    val sha256: String,
    val certificateSha256: String,
    val sizeBytes: Long,
    val downloadPath: String,
    val wardveilEvidenceRef: String,
    val wardveilValidUntilEpochSeconds: Long,
)

sealed interface PackageDeliveryState {
    data class Unavailable(val reason: String) : PackageDeliveryState
    data object Checking : PackageDeliveryState
    data class Ready(val release: DevelopmentDeliveryRelease) : PackageDeliveryState
    data class Downloading(val release: DevelopmentDeliveryRelease) : PackageDeliveryState
    data class InstallPermissionRequired(val release: DevelopmentDeliveryRelease) : PackageDeliveryState
    data class AwaitingAndroid(val release: DevelopmentDeliveryRelease) : PackageDeliveryState
    data class Installed(val release: DevelopmentDeliveryRelease, val installedVersionCode: Long) :
        PackageDeliveryState
    data class Failed(val reason: String, val retryable: Boolean = true) : PackageDeliveryState
}

interface PackageDeliveryGateway {
    val isAvailable: Boolean

    fun probe(
        session: IdentitySession,
        item: StoreItem,
        callback: (PackageDeliveryState) -> Unit,
    )

    fun downloadAndInstall(
        session: IdentitySession,
        item: StoreItem,
        release: DevelopmentDeliveryRelease,
        callback: (PackageDeliveryState) -> Unit,
    )
}
