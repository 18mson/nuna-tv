pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Diperlukan untuk library youtubedl-android
        maven {
            url = java.net.URI("https://jitpack.io")
            content {
                includeGroup("io.github.junkfood02.youtubedl-android")
            }
        }
    }
}

rootProject.name = "Nuna TV"
include(":app")
