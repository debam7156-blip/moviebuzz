import java.util.Properties

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
        versionCode = 2
        versionName = "1.1.0"

        // The TMDB key is injected at build time from local.properties / CI secrets.
        // It can also be entered at runtime in Settings (stored in DataStore).
        // Keys are never committed to the repository.
        val localProps = Properties().apply {
            val f = rootProject.file("local.properties")
            if (f.exists()) f.inputStream().use { load(it) }
        }
        val tmdbKey = localProps.getProperty("MOVIEBUZZ_TMDB_API_KEY")
            ?: System.getenv("MOVIEBUZZ_TMDB_API_KEY") ?: ""

        buildConfigField("String", "TMDB_API_KEY", "\"$tmdbKey\"")
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
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
        )
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
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.espresso.core)
}
