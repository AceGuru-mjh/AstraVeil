// Build configuration for the AstraVeil core engine module.
//
// This module exposes the foundational engine surface used by every other
// AstraVeil module: capability detection, permission brokering, the event
// bus, persistent config, structured logging, and security primitives.
//
// It is an Android library (not a feature module) and intentionally keeps
// its dependency surface small so it can be consumed by both the :app and
// :providers modules without pulling in UI libraries.

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.astraveil.core"

    compileSdk = 35

    defaultConfig {
        minSdk = 26
        // Instrumented tests live in :app; the core module ships only JVM tests.
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // AndroidX core utilities (Context, BuildCompat, etc.).
    implementation(libs.androidx.core.ktx)

    // Serialization of capability snapshots, config, permission sets.
    implementation(libs.kotlinx.serialization.json)

    // Coroutines used by CapabilityEngine.scan and ConfigManager.
    implementation(libs.kotlinx.coroutines.android)

    // JVM-only tests.
    testImplementation(libs.junit)
}
