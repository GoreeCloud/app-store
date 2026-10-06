package com.goreecloud.appstore.delivery

import android.content.Context
import com.goreecloud.appstore.domain.IdentitySession
import com.goreecloud.appstore.domain.StoreItem

object PackageDeliveryGatewayFactory {
    fun create(context: Context): PackageDeliveryGateway = UnavailableReleasePackageDeliveryGateway
}

private object UnavailableReleasePackageDeliveryGateway : PackageDeliveryGateway {
    override val isAvailable: Boolean = false

    override fun probe(
        session: IdentitySession,
        item: StoreItem,
        callback: (PackageDeliveryState) -> Unit,
    ) {
        callback(
            PackageDeliveryState.Unavailable(
                "Production package delivery has not completed GoreeCloud acceptance.",
            ),
        )
    }

    override fun downloadAndInstall(
        session: IdentitySession,
        item: StoreItem,
        release: DevelopmentDeliveryRelease,
        callback: (PackageDeliveryState) -> Unit,
    ) {
        callback(
            PackageDeliveryState.Failed(
                reason = "Production package delivery is disabled.",
                retryable = false,
            ),
        )
    }
}
