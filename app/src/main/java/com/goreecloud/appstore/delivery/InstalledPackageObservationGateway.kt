package com.goreecloud.appstore.delivery

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.goreecloud.appstore.domain.PackageDeliveryPolicy

sealed interface InstalledPackageLookupResult {
    data class Installed(
        val packageName: String,
        val versionCode: Long,
    ) : InstalledPackageLookupResult

    /** Android cannot distinguish absent from hidden when package visibility denies observation. */
    data object NotObserved : InstalledPackageLookupResult

    data class Failed(val failureType: String) : InstalledPackageLookupResult
}

fun interface InstalledPackageLookup {
    fun lookup(packageName: String): InstalledPackageLookupResult
}

sealed interface InstalledPackageObservation {
    data class Installed(
        val packageName: String,
        val versionCode: Long,
    ) : InstalledPackageObservation

    data class Unknown(val reason: Reason) : InstalledPackageObservation {
        enum class Reason {
            INVALID_PACKAGE_IDENTITY,
            NOT_FOUND_OR_NOT_VISIBLE,
            PLATFORM_FAILURE,
            RESPONSE_IDENTITY_MISMATCH,
            INVALID_VERSION_CODE,
        }
    }
}

/**
 * Reads one explicitly supplied package identity at a time.
 *
 * This gateway never enumerates installed applications and never interprets Android's
 * NameNotFoundException as proof of absence. On Android 11+ an installed package can be hidden by
 * package-visibility rules, so an unobserved lookup remains UNKNOWN and cannot authorize INSTALL.
 */
class InstalledPackageObservationGateway(
    private val lookup: InstalledPackageLookup,
) {
    fun observe(packageName: String): InstalledPackageObservation {
        if (!isCanonicalPackageName(packageName)) {
            return InstalledPackageObservation.Unknown(
                InstalledPackageObservation.Unknown.Reason.INVALID_PACKAGE_IDENTITY,
            )
        }

        return when (val result = lookup.lookup(packageName)) {
            is InstalledPackageLookupResult.Installed -> when {
                result.packageName != packageName -> InstalledPackageObservation.Unknown(
                    InstalledPackageObservation.Unknown.Reason.RESPONSE_IDENTITY_MISMATCH,
                )

                result.versionCode < 0L -> InstalledPackageObservation.Unknown(
                    InstalledPackageObservation.Unknown.Reason.INVALID_VERSION_CODE,
                )

                else -> InstalledPackageObservation.Installed(
                    packageName = result.packageName,
                    versionCode = result.versionCode,
                )
            }

            InstalledPackageLookupResult.NotObserved -> InstalledPackageObservation.Unknown(
                InstalledPackageObservation.Unknown.Reason.NOT_FOUND_OR_NOT_VISIBLE,
            )

            is InstalledPackageLookupResult.Failed -> InstalledPackageObservation.Unknown(
                InstalledPackageObservation.Unknown.Reason.PLATFORM_FAILURE,
            )
        }
    }

    fun toDeviceState(
        sdkInt: Int,
        observation: InstalledPackageObservation,
    ): PackageDeliveryPolicy.DeviceState = when (observation) {
        is InstalledPackageObservation.Installed -> PackageDeliveryPolicy.DeviceState.observedInstalled(
            sdkInt = sdkInt,
            packageName = observation.packageName,
            versionCode = observation.versionCode,
        )

        is InstalledPackageObservation.Unknown -> PackageDeliveryPolicy.DeviceState.unobserved(sdkInt)
    }

    private fun isCanonicalPackageName(value: String): Boolean {
        if (value.isBlank() || value.length > 255 || value != value.trim()) return false
        if (value.any(Char::isISOControl)) return false
        val segments = value.split('.')
        return segments.size >= 2 && segments.all(PACKAGE_SEGMENT::matches)
    }

    companion object {
        private val PACKAGE_SEGMENT = Regex("[A-Za-z][A-Za-z0-9_]*")
    }
}

/**
 * Android implementation for one exact package lookup.
 *
 * No QUERY_ALL_PACKAGES permission and no installed-package enumeration are required or used here.
 * NameNotFoundException and SecurityException both remain NotObserved because neither proves that
 * the target package is absent under Android package-visibility rules.
 */
class AndroidInstalledPackageLookup(context: Context) : InstalledPackageLookup {
    private val packageManager = context.applicationContext.packageManager

    override fun lookup(packageName: String): InstalledPackageLookupResult = try {
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0)
        }
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
        InstalledPackageLookupResult.Installed(
            packageName = info.packageName,
            versionCode = versionCode,
        )
    } catch (_: PackageManager.NameNotFoundException) {
        InstalledPackageLookupResult.NotObserved
    } catch (_: SecurityException) {
        InstalledPackageLookupResult.NotObserved
    } catch (error: RuntimeException) {
        InstalledPackageLookupResult.Failed(error::class.java.name)
    }
}
