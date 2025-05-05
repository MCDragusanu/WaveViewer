plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    id("org.jetbrains.kotlin.plugin.serialization")
    id("kotlinx-serialization")
}

android {
    namespace = "com.example.waveviewer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.waveviewer"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    //Use for Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")

    //  Lifecycle Dependencies
    implementation ("androidx.lifecycle:lifecycle-runtime-compose-android:2.8.7")
    implementation ("androidx.lifecycle:lifecycle-runtime-compose:2.6.1")
    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")

    // Jetpack Compose
    implementation ("androidx.compose.material3:material3-android:1.3.1")
    implementation ("androidx.compose.runtime:runtime-android:1.7.7")
    implementation ("androidx.compose.foundation:foundation-layout:1.7.7")
    implementation("androidx.compose.ui:ui:1.4.3")
    implementation("androidx.compose.material:material:1.8.0-alpha04")
    implementation("androidx.navigation:navigation-compose:2.6.0")
    implementation("androidx.compose.material:material:1.8.0-alpha04")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}