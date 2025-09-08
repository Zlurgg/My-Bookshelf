# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep line numbers for better crash reports
-keepattributes SourceFile,LineNumberTable

# Rename source file attribute to reduce APK size
-renamesourcefileattribute SourceFile

# ====== Kotlin and Coroutines ======
-dontwarn kotlinx.**
-keep class kotlinx.coroutines.** { *; }
-keep interface kotlinx.coroutines.** { *; }

# ====== Jetpack Compose ======
-keep class androidx.compose.** { *; }
-keep interface androidx.compose.** { *; }
-keep class androidx.activity.** { *; }

# ====== Room Database ======
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keep @androidx.room.Database class *
-keep class uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.** { *; }

# ====== Koin Dependency Injection ======
-keep class org.koin.** { *; }
-keep interface org.koin.** { *; }
-keep class uk.co.zlurgg.mybookshelf.di.** { *; }

# ====== Ktor HTTP Client ======
-keep class io.ktor.** { *; }
-keep interface io.ktor.** { *; }
-keepclassmembers class io.ktor.** { *; }
-keep class io.ktor.client.engine.android.** { *; }

# ====== Kotlinx Serialization ======
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep DTOs and domain models
-keep class uk.co.zlurgg.mybookshelf.bookshelf.data.book.dto.** { *; }
-keep class uk.co.zlurgg.mybookshelf.bookshelf.domain.** { *; }
-keep class uk.co.zlurgg.mybookshelf.core.domain.** { *; }

# Keep serializable classes
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

# ====== Navigation Component ======
-keep class androidx.navigation.** { *; }
-keep interface androidx.navigation.** { *; }

# ====== Coil Image Loading ======
-keep class coil.** { *; }
-keep interface coil.** { *; }

# ====== ViewModels ======
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class uk.co.zlurgg.mybookshelf.bookshelf.presentation.** { *; }

# ====== Application Class ======
-keep class uk.co.zlurgg.mybookshelf.MyBookshelfApp { *; }

# ====== Enum Classes ======
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ====== Parcelable Classes ======
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# ====== Remove Logging in Release ======
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# ====== Suppress Warnings ======
-dontwarn org.slf4j.**
-dontwarn javax.annotation.**