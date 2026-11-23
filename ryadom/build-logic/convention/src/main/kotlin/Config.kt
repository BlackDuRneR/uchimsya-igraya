import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/** Общие параметры сборки «Рядом». */
object RyadomConfig {
    const val COMPILE_SDK = 36
    const val MIN_SDK = 26
    const val TARGET_SDK = 36
    val JAVA = JavaVersion.VERSION_17
}

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun Project.configureAndroid(ext: CommonExtension<*, *, *, *, *, *>) {
    ext.apply {
        compileSdk = RyadomConfig.COMPILE_SDK
        defaultConfig.minSdk = RyadomConfig.MIN_SDK
        compileOptions {
            sourceCompatibility = RyadomConfig.JAVA
            targetCompatibility = RyadomConfig.JAVA
            isCoreLibraryDesugaringEnabled = false
        }
        testOptions.unitTests.isIncludeAndroidResources = true
        lint {
            abortOnError = true
            warningsAsErrors = false
            disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion")
        }
    }
    extensions.configure(KotlinAndroidProjectExtension::class.java) {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            freeCompilerArgs.addAll(
                "-opt-in=kotlin.RequiresOptIn",
                "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
                "-opt-in=androidx.compose.foundation.layout.ExperimentalLayoutApi",
            )
        }
    }
}

internal fun Project.configureJvm() {
    extensions.configure(KotlinJvmProjectExtension::class.java) {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }
    extensions.configure(org.gradle.api.plugins.JavaPluginExtension::class.java) {
        sourceCompatibility = RyadomConfig.JAVA
        targetCompatibility = RyadomConfig.JAVA
    }
}
