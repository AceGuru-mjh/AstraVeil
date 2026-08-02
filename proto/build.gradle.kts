import com.google.protobuf.gradle.id

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.protobuf)
}

android {
    namespace = "com.astraveil.proto"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
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

// ─────────────────────────────────────────────────────────────────────────────
// Protobuf 配置
// ─────────────────────────────────────────────────────────────────────────────

protobuf {
    protoc {
        artifact = libs.protobuf.protoc.get().toString()
    }

    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                id("java") {
                    option("lite")
                }
                id("kotlin") {
                    option("lite")
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 依赖
// ─────────────────────────────────────────────────────────────────────────────

dependencies {
    api(libs.protobuf.javalite)
    api(libs.protobuf.kotlin.lite)

    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
}

// ─────────────────────────────────────────────────────────────────────────────
// 确保生成的代码被正确识别为源目录
// ─────────────────────────────────────────────────────────────────────────────

androidComponents {
    onVariants { variant ->
        val variantName = variant.name.replaceFirstChar { it.uppercase() }
        // KSP task may not exist in this module; use afterEvaluate to safely
        // wire the dependency only if the task is present.
        project.afterEvaluate {
            tasks.findByName("ksp${variantName}Kotlin")?.dependsOn("generate${variantName}Proto")
        }
    }
}
