// [impl->dsn~watch-data-layer~1]
// The application for a watch with Wear OS: it shows what the phone application sends and has no file of its own.

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.github.koppor.koppter.wear"
    compileSdk = 37

    defaultConfig {
        // That of the phone application: the Data Layer connects applications with the same id and the same signature only.
        applicationId = "io.github.koppor.koppter"
        // Wear OS 3, with which the Galaxy Watch 4 came.
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = providers.environmentVariable("KOPPTER_VERSION").getOrElse("0")
    }

    // The key of the phone application, set up as in :android (decision 0010).
    // [impl->adr~signing-key-as-secret~1]
    val keystoreFile = providers.environmentVariable("KOPPTER_KEYSTORE_FILE").orNull
    if (keystoreFile != null) {
        val password = providers.environmentVariable("KOPPTER_KEYSTORE_PASSWORD").get()
        signingConfigs {
            create("published") {
                storeFile = file(keystoreFile)
                storeType = "PKCS12"
                storePassword = password
                keyAlias = "koppter"
                keyPassword = password
            }
        }
        buildTypes {
            getByName("release") {
                signingConfig = signingConfigs.getByName("published")
            }
        }
    }

    // ponytail: not shrunk, as :android: the 23 MB are mostly code of Compose and Play services that nothing calls.
    // R8 (`isMinifyEnabled`) if the size matters, and then tried on a watch.
    buildFeatures {
        compose = true
    }

    sourceSets["main"].res.srcDir("../android/src/icon/res")
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.wear.compose.material3)
    implementation(libs.wear.compose.foundation)
    implementation(libs.play.services.wearable)
    // [impl->dsn~watch-tile~1]
    implementation(libs.wear.tiles)
    implementation(libs.wear.protolayout.material3)
    // Turns the answer of the Data Layer into the future that a tile service returns.
    implementation(libs.androidx.concurrent.futures)
}
