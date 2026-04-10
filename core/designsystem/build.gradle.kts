val applicationIdValue: String by rootProject.extra
val compileSdkValue: Int by rootProject.extra
val minSdkValue: Int by rootProject.extra
val compatibilityValue: JavaVersion by rootProject.extra
val jvmToolchainValue: Int by rootProject.extra

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose.compiler)
}

kotlin {
    jvmToolchain(jvmToolchainValue)
}

android {
    namespace = "$applicationIdValue.core.designsystem"
    compileSdk = compileSdkValue

    defaultConfig {
        minSdk = minSdkValue
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = compatibilityValue
        targetCompatibility = compatibilityValue
    }
}

dependencies {
    implementation(project(":core:model"))

    implementation(libs.androidx.core.ktx)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.androidx.compose)

    testImplementation(libs.junit4)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.bundles.androidx.compose.debug)
}
