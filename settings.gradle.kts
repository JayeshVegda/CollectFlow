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
        // The flatDir("app/libs") repository was removed: it pointed at two local .aar files
        // (core-release.aar, ktx-release.aar) that no dependency in this project declares, so it
        // was dead configuration that could silently satisfy a future dependency from a stale
        // local binary instead of from a repository.
    }
}

rootProject.name = "CollectFlow"
include(":app")
