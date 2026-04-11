plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.0"  // ADD THIS LINE
}

android {
    namespace = "com.bearinmind.launcher314"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bearinmind.launcher314"
        minSdk = 24
        targetSdk = 35
        versionCode = 12
        versionName = "0.0.9-beta"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("C:/Users/icedc/Downloads/ANDROID MASTER FOLDER/ANDROID STUFF AND APPS/my-key.jks")
            storePassword = "56712345"
            keyAlias = "key0"
            keyPassword = "56712345"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
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
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {


    implementation("androidx.compose.material:material-icons-extended:1.5.4")
    implementation("androidx.exifinterface:exifinterface:1.3.7")



    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.ui:ui:1.5.4")
    implementation("androidx.compose.ui:ui-tooling-preview:1.5.4")
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // Calendar library
    implementation("com.kizitonwose.calendar:compose:2.4.0")

    // Coil for async image loading (app icons)
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Accompanist for drawable painter (wallpaper display)
    implementation("com.google.accompanist:accompanist-drawablepainter:0.32.0")

    // Date handling
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // JSON Serialization for data persistence - ADD THESE TWO LINES
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")


    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.5.4")
    debugImplementation("androidx.compose.ui:ui-tooling:1.5.4")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.5.4")
}

// Disable ArtProfile tasks (baseline.prof causes reproducibility issues for F-Droid)
tasks.whenTaskAdded {
    if (name.contains("ArtProfile")) {
        enabled = false
    }
}