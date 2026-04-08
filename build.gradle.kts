val applicationIdValue by extra("com.kmwh.cryptoorderbook")
val compileSdkValue by extra(36)
val targetSdkValue by extra(36)
val minSdkValue by extra(26)
val compatibilityValue by extra(JavaVersion.VERSION_21)
val jvmToolchainValue by extra(21)

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose.compiler) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kapt) apply false
}

