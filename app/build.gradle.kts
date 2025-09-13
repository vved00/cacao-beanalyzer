plugins {
    alias(libs.plugins.androidApplication)
}

android {
    namespace = "com.example.fermentation"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.fermentation"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isDebuggable = true // ← Allows installation without signing
            signingConfig = null // ← Disables signing
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
    buildFeatures {
        mlModelBinding = true
    }
}

configurations.all {
    resolutionStrategy {
        force("org.tensorflow:tensorflow-lite:2.14.0")
        force("org.tensorflow:tensorflow-lite-support:0.4.3")
        force("org.tensorflow:tensorflow-lite-metadata:0.4.3")
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)

    implementation(libs.tensorflow.lite)
    implementation(libs.tensorflow.lite.metadata)
    implementation(libs.tensorflow.lite.support)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}