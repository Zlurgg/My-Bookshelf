package uk.co.zlurgg.mybookshelf.app

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import uk.co.zlurgg.mybookshelf.core.logging.LoggingInitializer
import uk.co.zlurgg.mybookshelf.di.DebugInitializer
import uk.co.zlurgg.mybookshelf.di.appModule
class MyBookshelfApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Per-variant Timber tree: DebugTree in debug, CrashlyticsTree in release.
        LoggingInitializer.initialize()

        // Initialize debug-only components (emulators, etc.)
        // In release builds, this is a no-op
        DebugInitializer.initialize()

        startKoin {
            androidContext(this@MyBookshelfApplication)
            modules(appModule)
        }
    }
}
