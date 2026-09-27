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
        versionCode = 4
        versionName = "0.1.3-dev"
        manifestPlaceholders["appLabel"] = "GoreeCloud App Store"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".dev"
            manifestPlaceholders["appLabel"] = "GoreeCloud App Store Dev"
            signingConfig = signingConfigs.getByName("development")
        }
        getByName("release") {
            manifestPlaceholders["appLabel"] = "GoreeCloud App Store"
        }
    }

    buildFeatures {
        compose = true
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

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
}
