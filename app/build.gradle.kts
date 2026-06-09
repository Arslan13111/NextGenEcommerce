import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
    kotlin("plugin.serialization")
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

fun configValue(name: String, defaultValue: String = ""): String =
    localProperties.getProperty(name)
        ?: providers.environmentVariable(name).orNull
        ?: defaultValue

fun quotedConfig(name: String, defaultValue: String = ""): String =
    "\"${configValue(name, defaultValue).replace("\\", "\\\\").replace("\"", "\\\"")}\""

android {
    namespace = "com.example.nextgenecommerce"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.nextgenecommerce"
        minSdk = 26  // Required for ARCore
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        manifestPlaceholders["snapCameraKitApiToken"] = configValue("SNAP_CAMERA_KIT_API_TOKEN")
        manifestPlaceholders["usesCleartextTraffic"] = false

        buildConfigField("String", "SUPABASE_URL", quotedConfig("SUPABASE_URL"))
        buildConfigField("String", "SUPABASE_ANON_KEY", quotedConfig("SUPABASE_ANON_KEY"))
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", quotedConfig("GOOGLE_WEB_CLIENT_ID"))
        buildConfigField("String", "BACKEND_BASE_URL", quotedConfig("BACKEND_BASE_URL", "https://api.example.invalid/"))
        buildConfigField("String", "TRYONA_BASE_URL", quotedConfig("TRYONA_BASE_URL", "https://api.tryona.com/"))
        buildConfigField("String", "TRYONA_API_KEY", quotedConfig("TRYONA_API_KEY"))
        buildConfigField("String", "RAPID_API_KEY", quotedConfig("RAPID_API_KEY"))
        buildConfigField("String", "RAPID_API_HOST", quotedConfig("RAPID_API_HOST", "try-on-diffusion.p.rapidapi.com"))
        buildConfigField("String", "SAFEPAY_ENV", quotedConfig("SAFEPAY_ENV", "sandbox"))
        buildConfigField("String", "SAFEPAY_PUBLIC_KEY", quotedConfig("SAFEPAY_PUBLIC_KEY"))
        buildConfigField("String", "SAFEPAY_API_BASE_URL", quotedConfig("SAFEPAY_API_BASE_URL", "https://sandbox.api.getsafepay.com/"))
        buildConfigField("String", "SAFEPAY_CHECKOUT_BASE_URL", quotedConfig("SAFEPAY_CHECKOUT_BASE_URL", "https://sandbox.api.getsafepay.com/checkout/pay"))
        buildConfigField("boolean", "ENABLE_HTTP_LOGGING", "false")

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            manifestPlaceholders["usesCleartextTraffic"] = true
            buildConfigField("String", "BACKEND_BASE_URL", quotedConfig("DEBUG_BACKEND_BASE_URL", "http://10.0.2.2:3000/"))
            buildConfigField("boolean", "ENABLE_HTTP_LOGGING", "true")
        }

        release {
            manifestPlaceholders["usesCleartextTraffic"] = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
        )
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

configurations.all {
    exclude(group = "com.android.support", module = "support-compat")
    exclude(group = "com.android.support", module = "support-v4")
    exclude(group = "com.android.support", module = "support-annotations")
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Jetpack Compose
    val composeBom = platform("androidx.compose:compose-bom:2023.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation("androidx.navigation:navigation-compose:2.7.6")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Compose Debug Tools
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Supabase
    val supabaseVersion = "2.0.4"
    implementation("io.github.jan-tennert.supabase:postgrest-kt:$supabaseVersion")
    implementation("io.github.jan-tennert.supabase:gotrue-kt:$supabaseVersion")
    implementation("io.github.jan-tennert.supabase:storage-kt:$supabaseVersion")
    implementation("io.github.jan-tennert.supabase:realtime-kt:$supabaseVersion")

    // Ktor Client for Supabase
    val ktorVersion = "2.3.7"
    implementation("io.ktor:ktor-client-android:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-utils:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")

    // Kotlin Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // Google Play Services (for Google Sign-In with Supabase)
    implementation("com.google.android.gms:play-services-auth:20.7.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")

    // Room Database
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Hilt Dependency Injection
    implementation("com.google.dagger:hilt-android:2.48")
    ksp("com.google.dagger:hilt-android-compiler:2.48")

    // Retrofit & OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Coil for Image Loading
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Glide for Image Loading (for Views)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    ksp("com.github.bumptech.glide:compiler:4.16.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Accompanist (System UI Controller for status bar theming)
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.32.0")
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")

    // Gson
    implementation("com.google.code.gson:gson:2.10.1")

    // Paging
    implementation("androidx.paging:paging-runtime-ktx:3.2.1")
    implementation("androidx.paging:paging-compose:3.2.1")

    // CameraX
    val cameraxVersion = "1.3.1"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
    implementation("androidx.camera:camera-extensions:$cameraxVersion")

    // Snap Camera Kit for Live AR Try-On
    val cameraKitVersion = "1.46.0"
    implementation("com.snap.camerakit:camerakit:$cameraKitVersion")
    implementation("com.snap.camerakit:support-camerax:$cameraKitVersion")

    // Image Processing
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // Lottie for animations
    implementation("com.airbnb.android:lottie-compose:6.3.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
