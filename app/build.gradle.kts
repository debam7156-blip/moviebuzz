plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.moviebuzz.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.moviebuzz.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        // API keys are injected at build time from local.properties / environment.
        // They are never committed to the repository.
        val localProps = Properties().apply {
            val f = rootProject.file("local.properties")
            if (f.exists()) f.inputStream().use { load(it) }
        }
        val tmdbKey = (localProps.getProperty("MOVIEBUZZ_TMDB_API_KEY")
            ?: System.getenv("MOVIEBUZZ_TMDB_API_KEY") ?: "")
        val aiEndpoint = (localProps.getProperty("MOVIEBUZZ_AI_ENDPOINT")
            ?: System.getenv("MOVIEBUZZ_AI_ENDPOINT") ?: "")
        val syncUrl = (localProps.getProperty("MOVIEBUZZ_SYNC_URL")
            ?: System.getenv("MOVIEBUZZ_SYNC_URL") ?: "")
        val syncToken = (localProps.getProperty("MOVIEBUZZ_SYNC_TOKEN")
            ?: System.getenv("MOVIEBUZZ_SYNC_TOKEN") ?: "")

        buildConfigField("String", "TMDB_API_KEY", "\"$tmdbKey\"")
        buildConfigField("String", "AI_ENDPOINT", "\"$aiEndpoint\"")
        buildConfigField("String", "SYNC_URL", "\"$syncUrl\"")
        buildConfigField("String", "SYNC_TOKEN", "\"$syncToken\"")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.compose.ui.tooling.preview)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.datastore.preferences)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)
    implementation(libs.work.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.espresso.core)
}
