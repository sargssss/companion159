plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    id("org.jetbrains.kotlin.kapt")

    //alias(libs.plugins.kotlin.serialization) // Now uses the catalog and inherits the Kotlin version
    //noinspection NewerVersionAvailable
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21" //DON`T CHANGE VERSION

    id("com.google.devtools.ksp") version "2.2.10-2.0.2"

    id("com.google.dagger.hilt.android") version "2.57.1" apply false // Match your app/build.gradle.kts version
    //id("com.google.devtools.ksp")
}

android {
    namespace = "com.lifelover.companion159"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.lifelover.companion159"
        minSdk = 24
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
        //    sourceCompatibility = JavaVersion.VERSION_1_8
        //    targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // default
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    // end default

    // USER DEPENDENCIES

    // Compose Nav
    implementation("androidx.navigation:navigation-compose:2.8.0")
    // ViewModel for Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.11.0") // Latest stable Retrofit 2.x
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0") // Latest stable OkHttp 4.x
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")

    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Для синхронізації
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // hilt
    implementation("com.google.dagger:hilt-android:2.57.1")
    ksp("com.google.dagger:hilt-android-compiler:2.57.1")


    // default testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    //end  default testing
}