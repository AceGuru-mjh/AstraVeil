pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AstraVeil"

// Android application module - the AstraUI control center
include(":app")

// Core engine module - Capability / Permission / Event / Config / Logger / Security
include(":core")

// Root provider abstraction layer - Magisk / KernelSU / APatch / AstraRoot
include(":providers")

// Public SDK module for third-party module developers
include(":sdk")

// Astra Module runtime system (.avm packages)
include(":modules")

// Native C++ JNI bridge module
include(":native")

// Rust security component (built via cargo and linked as static lib)
// include(":rust") // Rust is built out-of-band via Cargo, linked in :native
