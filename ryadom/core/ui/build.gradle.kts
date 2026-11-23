plugins {
    id("ryadom.android.library")
    id("ryadom.android.compose")
}

android {
    namespace = "app.ryadom.core.ui"
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:l10n"))
    implementation(libs.androidx.core.ktx)
}
