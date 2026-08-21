plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

apply(plugin = "androidx.baselineprofile")

val debugSigningSha256 = "8910A440E3C0D107E2B250D559DEF2AB7F02B5A1911556CA8154D9C9B4C77FD8"
val releaseSigningSha256 = providers.gradleProperty("ZHIHUIJI_RELEASE_SIGNING_SHA256")
    .orElse(debugSigningSha256)
    .get()

android {
    namespace = "com.zhihuiji.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.zhihuiji.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            buildConfigField("String", "APP_SIGNING_SHA256", "\"$debugSigningSha256\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            buildConfigField("String", "APP_SIGNING_SHA256", "\"$releaseSigningSha256\"")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:network"))
    implementation(project(":core:datastore"))
    implementation(project(":core:database"))

    implementation(project(":data:auth"))
    implementation(project(":data:product"))
    implementation(project(":data:customer"))
    implementation(project(":data:supplier"))
    implementation(project(":data:order"))
    implementation(project(":data:finance"))
    implementation(project(":data:report"))
    implementation(project(":data:agent"))
    implementation(project(":data:sync"))

    implementation(project(":feature:auth"))
    implementation(project(":feature:dashboard"))
    implementation(project(":feature:products"))
    implementation(project(":feature:customers"))
    implementation(project(":feature:suppliers"))
    implementation(project(":feature:sales"))
    implementation(project(":feature:purchases"))
    implementation(project(":feature:payments"))
    implementation(project(":feature:finance"))
    implementation(project(":feature:reports"))
    implementation(project(":feature:agent"))
    implementation(project(":feature:settings"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.profileinstaller)

    debugImplementation(libs.compose.ui.tooling)

    add("baselineProfile", project(":benchmark"))
    testImplementation(libs.junit)
}

configure<androidx.baselineprofile.gradle.consumer.BaselineProfileConsumerExtension> {
    automaticGenerationDuringBuild = false
    dexLayoutOptimization = true
}
