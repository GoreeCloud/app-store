package com.goreecloud.appstore.delivery

import android.content.Context

object PackageDeliveryGatewayFactory {
    fun create(context: Context): PackageDeliveryGateway =
        DevelopmentPackageDeliveryGateway(context.applicationContext)
}
