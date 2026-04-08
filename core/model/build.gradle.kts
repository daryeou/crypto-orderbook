val applicationIdValue: String by rootProject.extra
val compileSdkValue: Int by rootProject.extra
val minSdkValue: Int by rootProject.extra
val compatibilityValue: JavaVersion by rootProject.extra
val jvmToolchainValue: Int by rootProject.extra

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

kotlin {
    jvmToolchain(jvmToolchainValue)
}

android {
    namespace = "$applicationIdValue.core.model"
    compileSdk = compileSdkValue

    defaultConfig {
        minSdk = minSdkValue
    }

    compileOptions {
        sourceCompatibility = compatibilityValue
        targetCompatibility = compatibilityValue
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
}

