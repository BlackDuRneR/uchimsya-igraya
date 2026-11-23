plugins {
    id("ryadom.jvm.library")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    testImplementation(libs.kotlinx.coroutines.test)
}

dependencies {
    api(libs.kotlinx.serialization.json)
}
