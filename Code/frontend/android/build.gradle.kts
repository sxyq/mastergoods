buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("androidx.benchmark:benchmark-baseline-profile-gradle-plugin:1.3.4")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}

// Keep every Android module's build output in the repository-level temp area.
// This project is three levels below the repository root: Code/frontend/android.
val repositoryRoot = rootProject.projectDir.parentFile.parentFile.parentFile
val centralizedBuildRoot = repositoryRoot.resolve("tmp/build/gradle-output/android")

subprojects {
    val modulePath = path.removePrefix(":").replace(':', '/')
    layout.buildDirectory.set(centralizedBuildRoot.resolve(modulePath))
}

subprojects {
    configurations.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "androidx.lifecycle") {
                useVersion("2.8.7")
                because("Keep Android unit-test runtime classpaths on cached lifecycle artifacts.")
            }
            if (requested.group == "org.jetbrains.kotlinx" && requested.name.startsWith("kotlinx-coroutines")) {
                useVersion("1.9.0")
                because("Align coroutines with the version catalog and cached Android test artifacts.")
            }
        }
    }
}
