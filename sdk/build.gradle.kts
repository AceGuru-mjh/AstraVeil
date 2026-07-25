// Copyright (c) AstraVeil Project. Licensed under the Apache License, Version 2.0.
//
// :sdk — Public stable surface for third-party .avm module developers.
//
// Everything in this module is part of the *contract* between AstraVeil and
// third-party modules. Breaking changes here require a [AstraClient.sdkApiLevel]
// bump and a migration guide. Internal modules (:providers, :modules) are free
// to refactor at will; :sdk is not.

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.astraveil.sdk"
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

    // :sdk is built on the AstraVeil core engine (capability / permission).
    implementation(project(":core"))

    // The SDK facade returns `ProviderExecResult` and (internally) delegates
    // `execute(...)` to the active [RootProvider]. To avoid re-defining those
    // types, :sdk depends on :providers; the symbols remain part of the public
    // API surface and are covered by the SDK stability contract.
    implementation(project(":providers"))
}
