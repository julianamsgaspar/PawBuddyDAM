


plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "pt.ipt.dam2025.pawbuddy"
    compileSdk = 36

    defaultConfig {
        applicationId = "pt.ipt.dam2025.pawbuddy"
        minSdk = 28
        targetSdk = 36
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
    // allow the access to objects of the interface, from code,
    // in 'binding'
    buildFeatures {
        viewBinding=true
    }
}

dependencies {

    implementation (libs.glide.v4151);
    annotationProcessor (libs.compiler);

    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)


    implementation(libs.converter.gson.v250)
//For serialising JSONP add converter-scalars
    implementation(libs.converter.scalars)
//An Adapter for adapting RxJava 2.x types.
    implementation(libs.adapter.rxjava2)


    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    // CameraX core library using the camera2 implementation
    val cameraxVersion = "1.5.1"
    // The following line is optional, as the core library is included indirectly by camera-camera2
    implementation(libs.androidx.camera.core)
     implementation(libs.androidx.camera.camera2)

    // If you want to additionally use the CameraX Lifecycle library
    implementation(libs.androidx.camera.lifecycle)

    // If you want to additionally use the CameraX VideoCapture library
     implementation (libs.androidx.camera.video)
    // If you want to additionally use the CameraX View class
     implementation(libs.androidx.camera.view)

    // If you want to additionally add CameraX ML Kit Vision Integration
    implementation (libs.androidx.camera.mlkit.vision)
    // If you want to additionally use the CameraX Extensions library
    implementation(libs.androidx.camera.extensions)
    implementation("com.google.android.material:material:1.13.0")




    implementation (libs.glide)
    implementation(libs.retrofit)
    implementation(libs.converterGson)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

}