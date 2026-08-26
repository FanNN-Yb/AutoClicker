import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.mikepenz.aboutlibraries.plugin")
}

// 签名配置从根目录 keystore.properties 读取（不入库），缺失�?release 使用 debug 签名
val keystoreProperties = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.example.autoclicker"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.autoclicker"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
        }
    }

    signingConfigs {
        if (keystoreProperties.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = if (keystoreProperties.isNotEmpty()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
        compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin {
        jvmToolchain(21)
        compilerOptions {
            freeCompilerArgs.addAll("-Xskip-prerelease-check")
        }
    }

    buildFeatures {
        viewBinding = true
        compose = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2025.05.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.navigation:navigation-compose:2.9.0")
    debugImplementation("androidx.compose.ui:ui-tooling")

        // Backdrop (Liquid Glass effects)
    implementation("io.github.kyant0:backdrop:2.0.0-rc01")

    // RubberBand - iOS 式橡皮筋回弹
    implementation("com.github.FanNN-Yb:Rubber-Band:v1")

    // MMKV �?fast key-value storage (replaces SharedPreferences)
    implementation("com.tencent:mmkv:2.0.2")

    // App Startup �?optimized component initialization
    implementation("androidx.startup:startup-runtime:1.2.0")

    // Shapes - continuous rounded corner shapes (Capsule)
    implementation("io.github.kyant0:shapes:1.2.0")

    // Profile Installer �?pre-compiles hot code paths for faster cold start
    implementation("androidx.profileinstaller:profileinstaller:1.4.0")

    // Immutable collections �?Compose skips recomposition for @Immutable lists
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.4.0")

    // AboutLibraries �?开源库致谢
    implementation("com.mikepenz:aboutlibraries-compose-m3:11.6.2")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}
