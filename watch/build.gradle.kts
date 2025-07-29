plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.kapt")

}

android {
    namespace = "com.example.watch"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.remindwatch"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

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
    // Librerías principales de Wear OS
    implementation(libs.androidx.wear) // Componentes básicos para apps Wear OS
    implementation(libs.androidx.constraintlayout) // Soporte para ConstraintLayout en Wear

    // Google Play Services para comunicación entre dispositivos (Wearable API)
    implementation(libs.play.services.wearable)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.wear.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)


    // Room para persistencia de datos local (base de datos SQLite)
    implementation("androidx.room:room-runtime:2.6.1") // Runtime de Room
    kapt("androidx.room:room-compiler:2.6.1") // Procesador de anotaciones para Room
    implementation("androidx.room:room-ktx:2.6.1") // Extensiones Kotlin para Room

    // RecyclerView para listas
    implementation("androidx.recyclerview:recyclerview:1.3.2") // Listas tradicionales

    // Koin para inyección de dependencias
    implementation("io.insert-koin:koin-android:3.5.3")

    // Coroutines para asincronía
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Core KTX para soporte de ComponentActivity y utilidades esenciales
    implementation("androidx.core:core-ktx:1.12.0")
}