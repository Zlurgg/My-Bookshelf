package uk.co.zlurgg.mybookshelf.app

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber
import uk.co.zlurgg.mybookshelf.BuildConfig
import uk.co.zlurgg.mybookshelf.di.DebugInitializer
import uk.co.zlurgg.mybookshelf.di.appModule
class MyBookshelfApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for logging (DEBUG builds only)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Initialize debug-only components (emulators, etc.)
        // In release builds, this is a no-op
        DebugInitializer.initialize()

        startKoin {
            androidContext(this@MyBookshelfApplication)
            modules(appModule)
        }
    }
}
