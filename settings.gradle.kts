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
        // JetBrains Compose repository for Compose Multiplatform (Web)
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    // Allow required repositories from projects/plugins (needed for Kotlin/JS Node distributions)
    repositories {
        google()
        mavenCentral()
        // JetBrains Compose repository for Compose Multiplatform dependencies
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

rootProject.name = "bountu"
include(":app")
include(":web")
 
 