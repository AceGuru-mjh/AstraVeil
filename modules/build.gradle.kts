// Copyright (c) AstraVeil Project. Licensed under the Apache License, Version 2.0.
//
// :modules — Astra Module runtime.
//
// This module is responsible for the full life-cycle of `.avm` packages:
// install / uninstall / enable / disable / start / stop, plus the sandboxing
// layer that constrains what a running module may do. It is built on top of
// :core (engine primitives), :providers (root abstraction) and :sdk (the
// contract third-party modules are written against).

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.astraveil.modules"
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

    implementation(project(":core"))
    implementation(project(":providers"))
    implementation(project(":sdk"))
}
