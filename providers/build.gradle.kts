// Copyright (c) AstraVeil Project. Licensed under the Apache License, Version 2.0.
//
// :providers — Root abstraction layer.
//
// This module defines the `RootProvider` interface and concrete implementations
// for every root backend AstraVeil can drive (Magisk, KernelSU, APatch, and the
// future AstraRoot). The rest of AstraVeil NEVER does `if (magisk) else if (ksu)`
// — it always goes through this abstraction so new backends can be added without
// touching call-sites.

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.astraveil.providers"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = false
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    // The providers layer is built on top of the AstraVeil core engine
    // (EventBus, Logger, etc.). It must not depend on :sdk, :modules or :app.
    implementation(project(":core"))

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
