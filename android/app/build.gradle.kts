import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

// Auto-increment versionCode from file
val versionFile = file("version.properties")
if (!versionFile.exists()) {
    versionFile.writeText("VERSION_CODE=2\nVERSION_NAME=1.0.1\n")
}
val versionProps = Properties()
versionProps.load(FileInputStream(versionFile))
val autoVersionCode = versionProps.getProperty("VERSION_CODE", "1").toInt()
val autoVersionName = versionProps.getProperty("VERSION_NAME", "1.0.0")

// Increment on assembleRelease or bundleRelease
tasks.configureEach {
    if (name == "bundleRelease" || name == "assembleRelease") {
        doLast {
            val nextCode = autoVersionCode + 1
            val props = Properties()
            props.setProperty("VERSION_CODE", nextCode.toString())
            props.setProperty("VERSION_NAME", autoVersionName)
            props.store(FileOutputStream(versionFile), null)
            println("Auto-incremented versionCode to $nextCode")
        }
    }
}

android {
    namespace = "com.sasquatsh.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sasquatsh.app"
        minSdk = 26
        targetSdk = 35
        versionCode = autoVersionCode
        versionName = autoVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SUPABASE_URL", "\"https://yyfukoddeyiaxiufztdx.supabase.co\"")
        buildConfigField("String", "SUPABASE_FUNCTIONS_URL", "\"https://yyfukoddeyiaxiufztdx.supabase.co/functions/v1\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inl5ZnVrb2RkZXlpYXhpdWZ6dGR4Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzE5MTAxODIsImV4cCI6MjA4NzQ4NjE4Mn0.aujhFFlmiN_rswvYK4-yMrcuiCSa5osg-0i2aINvOYw\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"313358569933-i2rmbgu0gsprr1l29vj3bflq0pa8kf8m.apps.googleusercontent.com\"")
    }

    buildTypes {
        release {
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
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    // Navigation
    implementation(libs.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.moshi.kotlin)
    ksp(libs.moshi.codegen)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Image Loading
    implementation(libs.coil.compose)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.messaging.ktx)
    implementation(libs.play.services.auth)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services)
    implementation(libs.googleid)

    // Coroutines
    implementation(libs.coroutines.android)

    // DataStore
    implementation(libs.datastore.preferences)

    // Browser (Custom Tabs)
    implementation(libs.browser)

    // Google Play Billing
    implementation(libs.billing)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
