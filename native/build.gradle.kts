plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.astraveil.nativelib"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++20"
                arguments += listOf(
                    "-DANDROID_STL=c++_shared",
                    "-DANDROID_ARM_NEON=ON",
                )
                // Point CMake at the Rust cross-compile output root.
                // CMakeLists.txt resolves the per-ABI staticlib from
                // ANDROID_ABI under this root (e.g. arm64-v8a →
                // aarch64-linux-android/release/libastra_rust.a). When the
                // artifact is absent, CMake falls back to ASTRA_HAVE_RUST=0
                // (FFI stubbed) so builds without the Rust toolchain still
                // succeed. To produce a build with the Rust policy engine:
                //   cargo build --release --target aarch64-linux-android
                //   cargo build --release --target armv7-linux-androideabi
                arguments += "-DASTRA_RUST_ROOT=${rootDir}/rust/target"
            }
        }
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    externalNativeBuild {
        cmake {
            path = file("CMakeLists.txt")
            version = "3.22.1"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(project(":core"))
}
