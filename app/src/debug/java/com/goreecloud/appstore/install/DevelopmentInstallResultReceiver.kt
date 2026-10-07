package com.goreecloud.appstore.install

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build

class DevelopmentInstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_INSTALL_RESULT) return

        val status = intent.getIntExtra(
            PackageInstaller.EXTRA_STATUS,
            PackageInstaller.STATUS_FAILURE,
        )
        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty()
        val versionCode = intent.getLongExtra(EXTRA_VERSION_CODE, -1L)

        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putInt("last_status", status)
            .putString("last_package", packageName)
            .putLong("last_version_code", versionCode)
            .putString(
                "last_status_message",
                intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)?.take(200),
            )
            .apply()

        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            val confirmationIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_INTENT) as? Intent
            }
            confirmationIntent
                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ?.let(context::startActivity)
        }
    }

    companion object {
        const val ACTION_INSTALL_RESULT =
            "com.goreecloud.appstore.dev.action.PACKAGE_INSTALL_RESULT"
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_VERSION_CODE = "version_code"
        const val PREFERENCES = "development_package_install_results"
    }
}
