plugins {
    id("ryadom.android.library")
    id("ryadom.android.compose")
}

android {
    namespace = "app.ryadom.core.designsystem"
}

dependencies {
    implementation(libs.androidx.core.ktx)
}
