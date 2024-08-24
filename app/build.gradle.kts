plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.nomnal.onlyTuner"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nomnal.onlyTuner"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = ".5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        setProperty("archivesBaseName", "${applicationId}-${versionName}-${versionCode}")
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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(files("libs/TarsosDSP-latest.jar"))
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}