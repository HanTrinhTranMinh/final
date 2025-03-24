plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.quang.escan"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.quang.escan"
        minSdk = 33
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    
    buildFeatures {
        viewBinding = true
    }
    
    // Temporarily disable lint errors from failing the build
    lint {
        abortOnError = false
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    
    // Navigation Component
    implementation("androidx.navigation:navigation-fragment:2.7.7")
    implementation("androidx.navigation:navigation-ui:2.7.7")
    
    // CameraX
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
    
    // PDF handling
    implementation("com.itextpdf:itext7-core:7.2.3")
    
    // Image processing
    implementation("com.github.bumptech.glide:glide:4.16.0")
    
    // Firebase
    implementation(libs.firebase.auth)
    implementation("com.google.firebase:firebase-analytics:22.3.0")

    // ML Kit Text Recognition
    implementation("com.google.mlkit:text-recognition:16.0.1") // Latin
    implementation("com.google.mlkit:text-recognition-chinese:16.0.1") // Chinese
    implementation("com.google.mlkit:text-recognition-devanagari:16.0.1") // Devanagari
    implementation("com.google.mlkit:text-recognition-japanese:16.0.1") // Japanese
    implementation("com.google.mlkit:text-recognition-korean:16.0.1") // Korean

    // ML Kit for digital ink recognition (handwriting)
    implementation("com.google.mlkit:digital-ink-recognition:18.1.0")
    
    // ML Kit Barcode scanning
    implementation("com.google.mlkit:barcode-scanning:17.2.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    //thư viên translate
    implementation ("com.google.mlkit:translate:17.0.0")
}