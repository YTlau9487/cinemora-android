plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.cinemora.movieorder"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.cinemora.movieorder"
        minSdk = 26
        targetSdk = 34
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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.swiperefreshlayout)

    // Firebase BoM
    implementation(platform(libs.firebase.bom))
    // Firebase dependencies
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    
    // Glide for image loading
    implementation(libs.glide)

    // Gson for Local Cart
    implementation(libs.gson)

    // Android YouTube Player
    implementation("com.pierfrancescosoffritti.androidyoutubeplayer:core:12.1.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}