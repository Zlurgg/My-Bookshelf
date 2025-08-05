package uk.co.zlurgg.mybookshelf.app

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import uk.co.zlurgg.mybookshelf.di.appModule

class MyBookshelfApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MyBookshelfApplication)
            modules(appModule)
        }
    }
}