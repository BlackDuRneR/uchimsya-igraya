pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        // Зеркало Maven Central от Google: стабильнее при ограничениях частоты запросов
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        // Зеркало Maven Central от Google: стабильнее при ограничениях частоты запросов
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        mavenCentral()
    }
}

rootProject.name = "Ryadom"

include(":app")
include(":core:model")
include(":core:l10n")
include(":core:designsystem")
include(":core:data")
include(":core:ui")
include(":feature:onboarding")
include(":feature:home")
include(":feature:schedule")
include(":feature:family")
include(":feature:history")
include(":feature:medication")
include(":feature:settings")
