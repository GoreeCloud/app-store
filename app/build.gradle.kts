plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

fun buildConfigString(value: String): String =
    "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

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
        versionCode = 15
        versionName = "0.1.14-dev"
        manifestPlaceholders["appLabel"] = "GoreeCloud App Store"

        buildConfigField("String", "DEVELOPMENT_DELIVERY_BASE_URL", buildConfigString(""))
        buildConfigField("String", "DEVELOPMENT_DELIVERY_TOKEN", buildConfigString(""))
        buildConfigField("String", "DEVELOPMENT_DELIVERY_TLS_CERT_SHA256", buildConfigString(""))

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".dev"
            manifestPlaceholders["appLabel"] = "GoreeCloud App Store Dev"
            signingConfig = signingConfigs.getByName("development")

            val deliveryBaseUrl = providers.gradleProperty("goreecloudDevDeliveryBaseUrl")
                .orElse(providers.environmentVariable("GOREECLOUD_DEV_DELIVERY_BASE_URL"))
                .orNull
                .orEmpty()
            val deliveryToken = providers.gradleProperty("goreecloudDevDeliveryToken")
                .orElse(providers.environmentVariable("GOREECLOUD_DEV_DELIVERY_TOKEN"))
                .orNull
                .orEmpty()
            val deliveryTlsPin = providers.gradleProperty("goreecloudDevDeliveryTlsCertSha256")
                .orElse(providers.environmentVariable("GOREECLOUD_DEV_DELIVERY_TLS_CERT_SHA256"))
                .orNull
                .orEmpty()
            buildConfigField("String", "DEVELOPMENT_DELIVERY_BASE_URL", buildConfigString(deliveryBaseUrl))
            buildConfigField("String", "DEVELOPMENT_DELIVERY_TOKEN", buildConfigString(deliveryToken))
            buildConfigField("String", "DEVELOPMENT_DELIVERY_TLS_CERT_SHA256", buildConfigString(deliveryTlsPin))
        }
        getByName("release") {
            manifestPlaceholders["appLabel"] = "GoreeCloud App Store"
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

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")

    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
}
