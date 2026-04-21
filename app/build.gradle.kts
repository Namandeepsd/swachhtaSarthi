plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.swachhtasarthi"
    compileSdk = 36

    buildFeatures {
        buildConfig = true
    }

    val imageKitAuthEndpoint = ((project.findProperty("IMAGEKIT_AUTH_ENDPOINT") as String?) ?: "")
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
    val imageKitPrivateKey = ((project.findProperty("IMAGEKIT_PRIVATE_KEY") as String?) ?: "")
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")

    defaultConfig {
        applicationId = "com.example.swachhtasarthi"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "IMAGEKIT_AUTH_ENDPOINT", "\"$imageKitAuthEndpoint\"")
        buildConfigField("String", "IMAGEKIT_PRIVATE_KEY", "\"$imageKitPrivateKey\"")

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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // 🔥 Firebase (correct way using BOM)
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation(libs.firebase.firestore)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    implementation(libs.recyclerview)
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.github.imagekit-developer:imagekit-android:3.0.1")
    implementation(libs.firebase.database)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
