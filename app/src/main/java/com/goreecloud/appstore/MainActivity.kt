package com.goreecloud.appstore

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.goreecloud.appstore.ui.GoreeCloudAppStoreRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val isLightAppearance = (
            resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        ) != Configuration.UI_MODE_NIGHT_YES
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = isLightAppearance
            isAppearanceLightNavigationBars = isLightAppearance
        }
        setContent {
            GoreeCloudAppStoreRoot()
        }
    }
}
