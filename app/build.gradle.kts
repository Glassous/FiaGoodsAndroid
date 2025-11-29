import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val envLocal = Properties().apply {
    val f = rootProject.file(".env.local")
    if (f.exists()) {
        load(f.inputStream())
    }
}
val SUPABASE_URL: String = envLocal.getProperty("SUPABASE_URL", "")
val SUPABASE_ANON_KEY: String = envLocal.getProperty("SUPABASE_ANON_KEY", "")
val OSS_ENDPOINT: String = envLocal.getProperty("OSS_ENDPOINT", "")
val OSS_BUCKET: String = envLocal.getProperty("OSS_BUCKET", "")
val OSS_ACCESS_KEY_ID: String = envLocal.getProperty("OSS_ACCESS_KEY_ID", "")
val OSS_ACCESS_KEY_SECRET: String = envLocal.getProperty("OSS_ACCESS_KEY_SECRET", "")
val OSS_PUBLIC_BASE_URL: String = envLocal.getProperty("OSS_PUBLIC_BASE_URL", "")
val APP_VERSION_JSON_URL: String = envLocal.getProperty("APP_VERSION_JSON_URL", "")
val APP_DOWNLOAD_BASE_URL: String = envLocal.getProperty("APP_DOWNLOAD_BASE_URL", "")

android {
    namespace = "com.glassous.fiagoods"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.glassous.fiagoods"
        minSdk = 33
        targetSdk = 36
        versionCode = 4
        versionName = "1.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SUPABASE_URL", "\"$SUPABASE_URL\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$SUPABASE_ANON_KEY\"")

        buildConfigField("String", "OSS_ENDPOINT", "\"$OSS_ENDPOINT\"")
        buildConfigField("String", "OSS_BUCKET", "\"$OSS_BUCKET\"")
        buildConfigField("String", "OSS_ACCESS_KEY_ID", "\"$OSS_ACCESS_KEY_ID\"")
        buildConfigField("String", "OSS_ACCESS_KEY_SECRET", "\"$OSS_ACCESS_KEY_SECRET\"")
        buildConfigField("String", "OSS_PUBLIC_BASE_URL", "\"$OSS_PUBLIC_BASE_URL\"")
        buildConfigField("String", "APP_VERSION_JSON_URL", "\"$APP_VERSION_JSON_URL\"")
        buildConfigField("String", "APP_DOWNLOAD_BASE_URL", "\"$APP_DOWNLOAD_BASE_URL\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
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
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.okhttp)
    implementation(libs.gson)
    implementation(libs.coroutines.android)
    implementation(libs.coil.compose)
    implementation(libs.androidx.security.crypto)

    implementation(libs.aliyun.oss)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}