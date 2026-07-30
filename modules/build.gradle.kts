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

    testOptions {
        unitTests.isReturnDefaultValues = true
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
    // NativeBridge (nativeInvokeModuleEntry / JNI entry probing) lives in
    // :native and is referenced by ModuleRuntime.tryInvokeEntry at compile
    // time. Without this dependency, com.astraveil.nativelib.NativeBridge is
    // unresolved from the :modules compilation classpath.
    implementation(project(":native"))
    // AstraSdkConstants (MODULE_API_LEVEL, SUPPORTED_PERMISSIONS) lives in :sdk
    // and is referenced by ModuleValidator at compile time.
    implementation(project(":sdk"))

    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("io.mockk:mockk:1.13.13")
}
