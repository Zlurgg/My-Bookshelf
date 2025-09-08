# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep line numbers for better crash reports
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*

# ====== Application Classes ======
-keep class uk.co.zlurgg.mybookshelf.bookshelf.presentation.MyBookShelfAppKt { *; }
-keep class uk.co.zlurgg.mybookshelf.MainActivity { *; }

# ====== Domain Models (needed for serialization/Room) ======
-keep class uk.co.zlurgg.mybookshelf.bookshelf.domain.Book { *; }
-keep class uk.co.zlurgg.mybookshelf.bookshelf.domain.Bookshelf { *; }
-keep class uk.co.zlurgg.mybookshelf.bookshelf.domain.Bookcase { *; }
-keep class uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle { *; }

# ====== Room Database ======
-keep class uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.BookshelfDatabase { *; }
-keep class uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.BookEntity { *; }
-keep class uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.BookshelfEntity { *; }
-keep class uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.BookshelfBookCrossRef { *; }
-keep class uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.BookshelfDao { *; }

# ====== Network DTOs (for Kotlinx Serialization) ======
-keep class uk.co.zlurgg.mybookshelf.bookshelf.data.book.dto.** { 
    <fields>;
    <init>();
}
-keepclassmembers class uk.co.zlurgg.mybookshelf.bookshelf.data.book.dto.** {
    *** Companion;
}

# ====== ViewModels ======
-keep class uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase.BookcaseViewModel { <init>(...); }
-keep class uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelf.BookshelfViewModel { <init>(...); }
-keep class uk.co.zlurgg.mybookshelf.bookshelf.presentation.book_detail.BookDetailViewModel { <init>(...); }
-keep class uk.co.zlurgg.mybookshelf.bookshelf.presentation.shared.SharedMyBookshelfViewModel { <init>(...); }

# ====== UI State Classes ======
-keep class uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase.BookcaseState { *; }
-keep class uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelf.BookshelfState { *; }
-keep class uk.co.zlurgg.mybookshelf.bookshelf.presentation.book_detail.BookDetailState { *; }

# ====== Koin DI Module ======
-keep class uk.co.zlurgg.mybookshelf.di.AppModuleKt { *; }

# ====== Kotlin Coroutines ======
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keep class kotlinx.coroutines.android.AndroidExceptionPreHandler { *; }
-keep class kotlinx.coroutines.android.AndroidDispatcherFactory { *; }

# ====== Jetpack Compose (only what's needed) ======
-dontwarn androidx.compose.material.**
-keepclasseswithmembers class androidx.compose.ui.** { *; }

# ====== Ktor Client ======
-keep class io.ktor.client.engine.android.** { *; }
-keepclassmembers class io.ktor.** {
    volatile <fields>;
}

# ====== Kotlinx Serialization ======
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# Keep Serializers
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep fields with @SerialName annotation
-keepclassmembers class uk.co.zlurgg.mybookshelf.** {
    @kotlinx.serialization.SerialName <fields>;
}

# ====== Coil Image Loading ======
-keep class coil.network.HttpException { *; }
-keep class coil.decode.DataSource { *; }

# ====== Enum Classes ======
-keepclassmembers enum uk.co.zlurgg.mybookshelf.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ====== Android Components ======
-keep class * extends androidx.lifecycle.ViewModel {
    <init>();
}
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *

# ====== Remove Logging in Release ======
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}

# ====== R8 Optimizations ======
-repackageclasses 'o'
-allowaccessmodification
-dontpreverify
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5

# ====== Remove Unused Code More Aggressively ======
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void checkNotNull(java.lang.Object);
    public static void checkNotNull(java.lang.Object, java.lang.String);
    public static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
    public static void checkNotNullParameter(java.lang.Object, java.lang.String);
}

# ====== Suppress Specific Warnings ======
-dontwarn org.slf4j.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn kotlin.reflect.jvm.internal.**