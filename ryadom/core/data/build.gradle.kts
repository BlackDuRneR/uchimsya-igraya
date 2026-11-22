plugins {
    id("ryadom.android.library")
    id("ryadom.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "app.ryadom.core.data"
}

dependencies {
    api(project(":core:model"))
    api(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.datastore.preferences)
    testImplementation(libs.kotlinx.coroutines.test)
}

android {
    testOptions.unitTests.all {
        it.systemProperty("robolectric.dependency.repo.url", "https://maven-central.storage-download.googleapis.com/maven2/")
    }
}

dependencies {
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
