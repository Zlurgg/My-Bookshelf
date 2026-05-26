import java.util.Properties
import java.io.FileInputStream
import java.io.ByteArrayOutputStream

/**
 * Attempts to detect the host machine's LAN IP so a physical device on the same
 * Wi-Fi can reach the Firebase emulators. Returns null if detection fails or
 * yields a loopback/link-local address — caller falls back to the emulator host.
 */
fun detectLanIp(): String? {
    val os = System.getProperty("os.name").lowercase()
    val command = when {
        os.contains("mac") -> listOf("ipconfig", "getifaddr", "en0")
        os.contains("linux") -> listOf("sh", "-c", "hostname -I | awk '{print \$1}'")
        else -> return null
    }
    return runCatching {
        val process = ProcessBuilder(command).redirectErrorStream(true).start()
        val output = ByteArrayOutputStream().use { buf ->
            process.inputStream.copyTo(buf)
            buf.toString(Charsets.UTF_8).trim()
        }
        process.waitFor()
        output.takeIf {
            it.matches(Regex("""\d+\.\d+\.\d+\.\d+""")) &&
                !it.startsWith("127.") &&
                !it.startsWith("169.254.")
        }
    }.getOrNull()
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.detekt)
}

// Load keystore properties
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

// Load local properties for debug config
val localPropertiesFile = rootProject.file("local.properties")
val localProperties = Properties()
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

android {
    namespace = "uk.co.zlurgg.mybookshelf"
    compileSdk = 36

    defaultConfig {
        applicationId = "uk.co.zlurgg.mybookshelf"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

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
            // Firebase emulator hosts — two values so a single debug APK works on both
            // the Android Emulator (which routes localhost via 10.0.2.2) and a physical
            // device on the same LAN (which needs the dev machine's LAN IP).
            // FirebaseEmulatorConfig picks between them at runtime via Build.FINGERPRINT.
            //
            // Override either in local.properties:
            //   firebase.emulator.host.emulator=10.0.2.2
            //   firebase.emulator.host.device=192.168.1.x
            //
            // If firebase.emulator.host.device is unset, Gradle attempts to auto-detect
            // the LAN IP via `ipconfig getifaddr en0` (macOS) / `hostname -I` (linux).
            val emulatorHost = localProperties.getProperty("firebase.emulator.host.emulator")
                ?: localProperties.getProperty("firebase.emulator.host", "10.0.2.2")
            val deviceHost = localProperties.getProperty("firebase.emulator.host.device")
                ?: detectLanIp()
                ?: emulatorHost
            buildConfigField("String", "FIREBASE_EMULATOR_HOST", "\"$emulatorHost\"")
            buildConfigField("String", "FIREBASE_EMULATOR_DEVICE_HOST", "\"$deviceHost\"")
            buildConfigField("String", "OPEN_LIBRARY_BASE_URL", "\"https://openlibrary.org\"")
            buildConfigField("long", "HTTP_TIMEOUT_MILLIS", "20000L")
            buildConfigField("String", "SITE_BASE_URL", "\"https://zlurgg.github.io/My-Bookshelf\"")
            val googleBooksApiKey = localProperties.getProperty("GOOGLE_BOOKS_API_KEY", "")
            buildConfigField("String", "GOOGLE_BOOKS_API_KEY", "\"$googleBooksApiKey\"")
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
            buildConfigField("long", "HTTP_TIMEOUT_MILLIS", "20000L")
            buildConfigField("String", "SITE_BASE_URL", "\"https://zlurgg.github.io/My-Bookshelf\"")
            val googleBooksApiKey = localProperties.getProperty("GOOGLE_BOOKS_API_KEY", "")
            buildConfigField("String", "GOOGLE_BOOKS_API_KEY", "\"$googleBooksApiKey\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    sourceSets["main"].assets.srcDir("schemas")

}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
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
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.ktor3)

    implementation(libs.bundles.ktor)
    implementation(libs.bundles.koin)
    implementation(libs.material3)
    implementation(libs.androidx.splashscreen)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.datastore.preferences)
    implementation(libs.timber)
    implementation(libs.play.review.ktx)
    implementation(libs.qrose)

    // Firebase & Google Auth
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.ktor.client.mock)
    
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.room.testing)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Detekt formatting rules (ktlint wrapper)
    detektPlugins(libs.detekt.formatting)
}

detekt {
    config.setFrom("$projectDir/detekt.yml")
    buildUponDefaultConfig = true
    allRules = false
    source.setFrom(
        files(
            "src/main/java",
            "src/main/kotlin",
            "src/test/java",
            "src/test/kotlin",
        )
    )
}