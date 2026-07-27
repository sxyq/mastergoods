plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

val pinnedHost = providers.gradleProperty("ZHIHUIJI_PINNED_HOST")
    .orElse("sxyq27.online")
    .get()
val pinnedSha256Pins = providers.gradleProperty("ZHIHUIJI_CERT_PINS")
    .orElse("")
    .get()
val certPinningEnabled = pinnedSha256Pins.isNotBlank()

android {
    namespace = "com.zhihuiji.core.network"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "NETWORK_LOGGING_ENABLED", "true")
            buildConfigField("boolean", "ALLOW_CLEARTEXT_BASE_URL", "false")
            buildConfigField("boolean", "ENFORCE_TRUSTED_BASE_URL", "false")
            buildConfigField("boolean", "CERT_PINNING_ENABLED", "false")
            buildConfigField("String", "PINNED_HOST", "\"$pinnedHost\"")
            buildConfigField("String", "PINNED_SHA256_PINS", "\"\"")
        }
        release {
            buildConfigField("boolean", "NETWORK_LOGGING_ENABLED", "false")
            buildConfigField("boolean", "ALLOW_CLEARTEXT_BASE_URL", "false")
            buildConfigField("boolean", "ENFORCE_TRUSTED_BASE_URL", "true")
            buildConfigField("boolean", "CERT_PINNING_ENABLED", certPinningEnabled.toString())
            buildConfigField("String", "PINNED_HOST", "\"$pinnedHost\"")
            buildConfigField("String", "PINNED_SHA256_PINS", "\"$pinnedSha256Pins\"")
        }
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:datastore"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.datastore.preferences)

    testImplementation(libs.junit)
}
