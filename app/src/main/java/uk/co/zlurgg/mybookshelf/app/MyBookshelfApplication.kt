package uk.co.zlurgg.mybookshelf.app

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber
import uk.co.zlurgg.mybookshelf.BuildConfig
import uk.co.zlurgg.mybookshelf.core.data.firebase.FirebaseEmulatorConfig
import uk.co.zlurgg.mybookshelf.di.appModule

class MyBookshelfApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for logging (DEBUG builds only)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Configure Firebase emulators BEFORE Koin (debug builds only)
        // Start emulators with: firebase emulators:start
        FirebaseEmulatorConfig.configureEmulators()

        startKoin {
            androidContext(this@MyBookshelfApplication)
            modules(appModule)
        }
    }
}
