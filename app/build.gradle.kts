plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.example.remindwatch"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.remindwatch"
        minSdk = 23
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
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    //Despendencias de diseno
    implementation(libs.androidx.cardview)
    implementation ("androidx.recyclerview:recyclerview:1.3.2")
    implementation ("com.airbnb.android:lottie:6.3.0")
    implementation(libs.androidx.recyclerview)
    implementation(libs.firebase.inappmessaging)
    // Google Play Services para comunicación entre dispositivos (Wearable API)
    implementation(libs.play.services.wearable)
    implementation(libs.androidx.swiperefreshlayout)

    // JUnit para pruebas unitarias
    testImplementation(libs.junit)
    // AndroidX JUnit para pruebas instrumentadas
    androidTestImplementation(libs.androidx.junit)
    // Espresso para pruebas UI
    androidTestImplementation(libs.androidx.espresso.core)

    // Room para persistencia de datos local (base de datos SQLite)
    implementation("androidx.room:room-ktx:2.6.1") // Extensiones Kotlin para Room
    kapt("androidx.room:room-compiler:2.6.1") // Procesador de anotaciones para Room
    implementation("androidx.room:room-runtime:2.6.1") // Runtime de Room

    // RecyclerView para listas tradicionales
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Koin para inyección de dependencias
    implementation("io.insert-koin:koin-android:3.5.3")

    // Coroutines para asincronía
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

}