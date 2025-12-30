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
        
        // --- 16 KB PAGE SIZE COMPATIBILITY START ---
        // Opzione 1 (Consigliata per NDK moderni e APK più grandi ma più veloci): 
        // Forziamo le librerie native ad essere decompresse e allineate a 16KB.
        // Questo richiede che le librerie .so stesse siano compilate con allineamento ELF 16KB,
        // ma spesso per librerie di terze parti (come quelle Google) la soluzione più rapida lato consumer
        // se non si ha controllo sul codice sorgente è assicurarsi che non siano compresse nell'APK
        // e che zipalign faccia il suo dovere con l'allineamento corretto.
        
        // Tuttavia, il warning specifico "LOAD segments not aligned at 16 KB boundaries" 
        // indica che il file .so stesso deve essere ricompilato o patchato, oppure si deve usare 
        // l'opzione "useLegacyPackaging" che mantiene le librerie compresse nell'APK 
        // e le estrae all'installazione, aggirando il problema dell'allineamento mmap diretto.
        
        // Opzione scelta: useLegacyPackaging = false
        // Questo forza l'installazione delle librerie native sul disco invece di caricarle direttamente dall'APK,
        // risolvendo il problema di allineamento per librerie di terze parti non ancora aggiornate.
        // Nota: Aumenta lo spazio occupato su disco dall'app installata.
        // ---------------------------------------------
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
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

    // CameraX
    implementation("androidx.camera:camera-core:1.5.2")
    implementation("androidx.camera:camera-camera2:1.5.2")
    implementation("androidx.camera:camera-lifecycle:1.5.2")
    implementation("androidx.camera:camera-view:1.5.2")
    implementation("androidx.camera:camera-extensions:1.5.2")
}
