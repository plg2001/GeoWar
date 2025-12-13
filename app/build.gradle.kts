plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.geowar"
    compileSdk = 35 // Mantengo 35 per ora, vedremo di risolvere le dipendenze

    defaultConfig {
        applicationId = "com.example.geowar"
        minSdk = 24
        targetSdk = 35 // Aggiorno targetSdk a 35
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Forziamo versioni compatibili con API 35 (Android 15)
    // Downgrade delle librerie che richiedono API 36 (Android 16 DP)
    
    implementation("androidx.core:core-ktx:1.15.0") // Downgrade da 1.17.0
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7") // Versione stabile
    implementation("androidx.activity:activity-compose:1.9.3") // Downgrade da 1.12.1
    
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    
    // Retrofit & Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    
    // ViewModel Compose
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.5.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)


    implementation("androidx.credentials:credentials:1.5.0-rc01") // Stabile compatibile con API 35
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0-rc01")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // Aggiunta libreria per decodificare il token JWT
    implementation("com.auth0.android:jwtdecode:2.0.2")

    // Google Maps & Location
    implementation("com.google.android.gms:play-services-maps:19.0.0")
    implementation("com.google.maps.android:maps-compose:4.4.1")
    implementation("com.google.android.gms:play-services-location:21.0.1")
}
