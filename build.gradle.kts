// Top-level build file where you can add configuration options common to all sub-projects/modules.
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.detekt) apply false
}

// ─────────────────────────────────────────────────────────────────────────────
// detekt — Kotlin static analysis
//
// Applied only to subprojects that already use the Kotlin Android plugin, so
// proto-only / C++-only modules are untouched. The configuration lives in
// config/detekt/detekt.yml. Run with:  ./gradlew detekt
// ─────────────────────────────────────────────────────────────────────────────
subprojects {
    pluginManager.withPlugin("org.jetbrains.kotlin.android") {
        apply(plugin = "io.gitlab.arturbosch.detekt")

        configure<DetektExtension> {
            buildUponDefaultConfig = true
            allRules = false
            autoCorrect = false
            config.setFrom(rootProject.files("config/detekt/detekt.yml"))
            parallel = true
        }

        dependencies {
            "detektPlugins"("io.gitlab.arturbosch.detekt:detekt-formatting:${libs.versions.detekt.get()}")
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// JaCoCo — coverage reporting (non-breaking, opt-in)
//
// Generates XML + HTML reports per module. Does NOT auto-run on test (no
// finalizedBy) so it never breaks the normal build. Invoke explicitly:
//   ./gradlew jacocoTestReport
// ─────────────────────────────────────────────────────────────────────────────
subprojects {
    pluginManager.withPlugin("org.jetbrains.kotlin.android") {
        apply(plugin = "jacoco")

        tasks.register<JacocoReport>("jacocoTestReport") {
            group = "verification"
            description = "Generates JaCoCo coverage report for this module."

            val testTask = tasks.findByName("testDebugUnitTest")
            if (testTask == null) {
                // Nothing to report — skip quietly.
                isEnabled = false
                return@register
            }
            dependsOn(testTask)

            reports {
                xml.required.set(true)
                html.required.set(true)
                csv.required.set(false)
            }

            val fileFilter = listOf(
                "**/R.class", "**/R$*.class",
                "**/BuildConfig.*", "**/Manifest*.*",
                "**/*Test*.*",
                "**/*_Impl*.*", "**/*Binding*.*",
                "**/ComposableSingletons*.*"
            )

            val kotlinClasses = layout.buildDirectory.dir("tmp/kotlin-classes/debug")
                .map { fileTree(it) { exclude(fileFilter) } }
            val javaClasses = layout.buildDirectory.dir("intermediates/javac/debug/classes")
                .map { fileTree(it) { exclude(fileFilter) } }

            classDirectories.setFrom(kotlinClasses, javaClasses)
            sourceDirectories.setFrom(files("src/main/kotlin", "src/main/java"))
            executionData.setFrom(fileTree(layout.buildDirectory) {
                include("jacoco/testDebugUnitTest.exec")
            })
        }
    }
}
