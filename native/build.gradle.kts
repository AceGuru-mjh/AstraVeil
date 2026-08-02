import org.apache.tools.ant.taskdefs.condition.Os

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

// ── Rust build configuration ──

val rustRoot: File = providers.environmentVariable("ASTRA_RUST_ROOT")
    .map { File(it) }
    .getOrElse(File(rootDir, "rust"))

val targetAbis = listOf("arm64-v8a", "armeabi-v7a")

fun rustLibPath(abi: String): File {
    val triple = when (abi) {
        "arm64-v8a" -> "aarch64-linux-android"
        "armeabi-v7a" -> "armv7-linux-androideabi"
        "x86_64" -> "x86_64-linux-android"
        else -> abi
    }
    return File(rustRoot, "target/$triple/release/libastra_rust.a")
}

fun isCargoAvailable(): Boolean = try {
    val cmd = if (Os.isFamily(Os.FAMILY_WINDOWS)) "where" else "which"
    ProcessBuilder(cmd, "cargo").redirectErrorStream(true).start().waitFor() == 0
} catch (_: Exception) { false }

val checkRustLibs by tasks.registering {
    group = "astraveil"
    description = "Check if Rust static libs are built for all target ABIs"
    doLast {
        val missing = targetAbis.filter { !rustLibPath(it).exists() }
        if (missing.isEmpty()) {
            logger.lifecycle("✅ Rust static libs ready: ${targetAbis.joinToString()}")
        } else {
            logger.warn("⚠️  Missing Rust libs for: ${missing.joinToString()}")
            logger.warn("   Run './gradlew :native:cargoBuild' or 'cd rust && ./build-android.sh'")
            logger.warn("   Build will continue with C++ stub (ASTRA_HAVE_RUST=0).")
        }
    }
}

val cargoBuild by tasks.registering(Exec::class) {
    group = "astraveil"
    description = "Build Rust policy engine for all target ABIs via cargo-ndk"

    onlyIf {
        if (!rustRoot.exists()) {
            logger.warn("⚠️  Rust crate dir not found: $rustRoot — skipping cargoBuild")
            false
        } else if (!isCargoAvailable()) {
            logger.warn("⚠️  cargo not in PATH — skipping cargoBuild (will use stub)")
            false
        } else true
    }

    workingDir = rustRoot
    val abiArgs = targetAbis.flatMap { listOf("-t", it) }
    commandLine = if (Os.isFamily(Os.FAMILY_WINDOWS)) {
        listOf("cmd", "/c", "cargo", "ndk") + abiArgs + listOf("--platform", "26", "--", "build", "--release")
    } else {
        listOf("cargo", "ndk") + abiArgs + listOf("--platform", "26", "--", "build", "--release")
    }
    isIgnoreExitValue = true

    doLast {
        if (executionResult.get().exitValue != 0) {
            logger.warn("⚠️  cargo ndk build failed (exit=${executionResult.get().exitValue})")
            logger.warn("   Rust FFI will use C++ stub. Check Rust toolchain for full functionality.")
        } else {
            logger.lifecycle("✅ Rust policy engine built successfully")
        }
    }
}

val cargoClean by tasks.registering(Exec::class) {
    group = "astraveil"
    description = "Clean Rust build artifacts (cargo clean)"
    onlyIf { rustRoot.exists() && isCargoAvailable() }
    workingDir = rustRoot
    commandLine = listOf("cargo", "clean")
}

// ── Android config ──

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
                    "-DASTRA_RUST_ROOT=${rustRoot.absolutePath}/target",
                )
            }
        }
        ndk {
            abiFilters += targetAbis
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

// Auto-trigger cargo build before CMake
tasks.matching { it.name.contains("externalNativeBuild") }.configureEach {
    dependsOn(cargoBuild)
    dependsOn(checkRustLibs)
}

tasks.named("clean") { dependsOn(cargoClean) }

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(project(":core"))
}
