dependencyResolutionManagement {
    repositories {
        google()
        // Зеркало Maven Central от Google: стабильнее при ограничениях частоты запросов
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") { from(files("../gradle/libs.versions.toml")) }
    }
}
rootProject.name = "build-logic"
include(":convention")
