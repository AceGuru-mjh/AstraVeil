plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.astraveil.xposed"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.astraveil.xposed"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }
    buildTypes {
        release { isMinifyEnabled = false }
    }
}

dependencies {
    compileOnly("de.robv.android.xposed:api:82")
}
