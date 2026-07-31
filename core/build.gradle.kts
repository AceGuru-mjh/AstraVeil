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

    compileSdk = providers.gradleProperty("astraveil.compileSdk").orNull?.toIntOrNull() ?: 35

    defaultConfig {
        minSdk = providers.gradleProperty("astraveil.minSdk").orNull?.toIntOrNull() ?: 26
        // Instrumented tests live in :app; the core module ships only JVM tests.
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        // Single source of truth: the version is read from gradle.properties
        // and exposed to Kotlin via BuildConfig so [com.astraveil.core.version.Version]
        // can derive NAME/CODE without hardcoding (which caused the earlier
        // 1.2.1 / 1.0.0 / 0.1.0-alpha drift).
        buildConfigField(
            "String",
            "ASTRAVEIL_VERSION",
            "\"${providers.gradleProperty("astraveil.version").orNull ?: "0.0.0"}\""
        )
        buildConfigField(
            "int",
            "ASTRAVEIL_VERSION_CODE",
            providers.gradleProperty("astraveil.versionCode").orNull ?: "1"
        )
    }

    buildFeatures {
        buildConfig = true
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

    testOptions {
        unitTests.isReturnDefaultValues = true
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
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.0.21")
}
