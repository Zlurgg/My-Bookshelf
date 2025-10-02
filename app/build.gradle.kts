import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// Load keystore properties
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "uk.co.zlurgg.mybookshelf"
    compileSdk = 36

    defaultConfig {
        applicationId = "uk.co.zlurgg.mybookshelf"
        minSdk = 28
        targetSdk = 36
        versionCode = 3
        versionName = "1.0.3-alpha"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "OPEN_LIBRARY_BASE_URL", "\"https://openlibrary.org\"")
            buildConfigField("String", "GOOGLE_BOOKS_BASE_URL", "\"https://www.googleapis.com/books/v1\"")
            buildConfigField("long", "HTTP_TIMEOUT_MILLIS", "20000L")
            buildConfigField("String", "API_VERSION", "\"v1\"")
            buildConfigField("String", "SHARE_BASE_URL", "\"https://zlurgg.github.io/My-Bookshelf/share\"")
            buildConfigField("String", "PRIMARY_BOOK_API", "\"OPEN_LIBRARY\"")
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "OPEN_LIBRARY_BASE_URL", "\"https://openlibrary.org\"")
            buildConfigField("String", "GOOGLE_BOOKS_BASE_URL", "\"https://www.googleapis.com/books/v1\"")
            buildConfigField("long", "HTTP_TIMEOUT_MILLIS", "20000L")
            buildConfigField("String", "API_VERSION", "\"v1\"")
            buildConfigField("String", "SHARE_BASE_URL", "\"https://zlurgg.github.io/My-Bookshelf/share\"")
            buildConfigField("String", "PRIMARY_BOOK_API", "\"OPEN_LIBRARY\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.incremental", "true")
    }
    sourceSets["main"].assets.srcDir("schemas")

}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.ktor3)

    implementation(libs.bundles.ktor)
    implementation(libs.bundles.koin)
    implementation(libs.material3)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.arch.core.testing)
    
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)


}