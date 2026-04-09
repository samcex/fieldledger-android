import org.jetbrains.kotlin.gradle.dsl.JvmTarget

fun buildConfigValue(raw: String?): String {
    val escaped = raw.orEmpty()
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
    return "\"$escaped\""
}

val billingBackendUrl = providers.gradleProperty("FIELDLEDGER_BILLING_BACKEND_URL")
    .orElse(providers.environmentVariable("FIELDLEDGER_BILLING_BACKEND_URL"))

val releaseStoreFilePath = providers.gradleProperty("FIELDLEDGER_RELEASE_STORE_FILE")
    .orElse(providers.environmentVariable("FIELDLEDGER_RELEASE_STORE_FILE"))
val releaseStorePassword = providers.gradleProperty("FIELDLEDGER_RELEASE_STORE_PASSWORD")
    .orElse(providers.environmentVariable("FIELDLEDGER_RELEASE_STORE_PASSWORD"))
val releaseKeyAlias = providers.gradleProperty("FIELDLEDGER_RELEASE_KEY_ALIAS")
    .orElse(providers.environmentVariable("FIELDLEDGER_RELEASE_KEY_ALIAS"))
val releaseKeyPassword = providers.gradleProperty("FIELDLEDGER_RELEASE_KEY_PASSWORD")
    .orElse(providers.environmentVariable("FIELDLEDGER_RELEASE_KEY_PASSWORD"))

val hasReleaseSigning = !releaseStoreFilePath.orNull.isNullOrBlank() &&
    !releaseStorePassword.orNull.isNullOrBlank() &&
    !releaseKeyAlias.orNull.isNullOrBlank() &&
    !releaseKeyPassword.orNull.isNullOrBlank()

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
}

android {
    namespace = "com.indie.shiftledger"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.indie.shiftledger"
        minSdk = 24
        targetSdk = 36
        versionCode = 11
        versionName = "0.3.8"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            if (hasReleaseSigning) {
                storeFile = file(requireNotNull(releaseStoreFilePath.orNull))
                storePassword = requireNotNull(releaseStorePassword.orNull)
                keyAlias = requireNotNull(releaseKeyAlias.orNull)
                keyPassword = requireNotNull(releaseKeyPassword.orNull)
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField(
                "String",
                "BILLING_BACKEND_URL",
                buildConfigValue(billingBackendUrl.orNull),
            )
        }

        release {
            isMinifyEnabled = false
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            buildConfigField(
                "String",
                "BILLING_BACKEND_URL",
                buildConfigValue(billingBackendUrl.orNull),
            )
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

kapt {
    correctErrorTypes = true
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.play.billing.ktx)

    kapt(libs.androidx.room.compiler)

    testImplementation(libs.junit)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
