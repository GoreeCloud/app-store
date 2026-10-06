plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.goreecloud.appstore"
    compileSdk = 37

    signingConfigs {
        create("development") {
            storeFile = rootProject.file("development/signing/goreecloud-development.p12")
            storePassword = "goreecloud-development-only"
            keyAlias = "goreecloud-development"
            keyPassword = "goreecloud-development-only"
            storeType = "PKCS12"
        }
    }

    defaultConfig {
        applicationId = "com.goreecloud.appstore"
        minSdk = 26
        targetSdk = 36
        versionCode = 14
        versionName = "0.1.13-dev"
        manifestPlaceholders["appLabel"] = "GoreeCloud App Store"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val developmentBackendUrl = providers.gradleProperty("GOREECLOUD_APP_STORE_DEV_BACKEND_URL")
        .orElse("")
        .get()
    val developmentBackendToken = providers.gradleProperty("GOREECLOUD_APP_STORE_DEV_TOKEN")
        .orElse("")
        .get()
    fun quotedBuildConfig(value: String): String =
        "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".dev"
            manifestPlaceholders["appLabel"] = "GoreeCloud App Store Dev"
            signingConfig = signingConfigs.getByName("development")
            buildConfigField("String", "DEVELOPMENT_BACKEND_URL", quotedBuildConfig(developmentBackendUrl))
            buildConfigField("String", "DEVELOPMENT_BACKEND_TOKEN", quotedBuildConfig(developmentBackendToken))
        }
        getByName("release") {
            manifestPlaceholders["appLabel"] = "GoreeCloud App Store"
            buildConfigField("String", "DEVELOPMENT_BACKEND_URL", "\"\"")
            buildConfigField("String", "DEVELOPMENT_BACKEND_TOKEN", "\"\"")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.08.00"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")

    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
}
