package uk.co.zlurgg.mybookshelf.core.di

import android.content.Context
import coil3.ImageLoader
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.android.Android
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import uk.co.zlurgg.mybookshelf.BuildConfig
import uk.co.zlurgg.mybookshelf.core.data.database.DatabaseFactory
import uk.co.zlurgg.mybookshelf.core.data.database.MyBookshelfRoomDatabase
import uk.co.zlurgg.mybookshelf.core.data.image.ImageLoaderFactory
import uk.co.zlurgg.mybookshelf.core.data.network.HttpClientFactory
import uk.co.zlurgg.mybookshelf.core.data.preferences.WelcomePreferencesImpl
import uk.co.zlurgg.mybookshelf.core.data.service.AndroidSystemLanguageProvider
import uk.co.zlurgg.mybookshelf.core.data.service.SystemTimeProvider
import uk.co.zlurgg.mybookshelf.core.data.service.UuidIdGenerator
import uk.co.zlurgg.mybookshelf.core.domain.preferences.WelcomePreferences
import uk.co.zlurgg.mybookshelf.core.domain.service.IdGenerator
import uk.co.zlurgg.mybookshelf.core.domain.service.SystemLanguageProvider
import uk.co.zlurgg.mybookshelf.core.domain.service.TimeProvider
import uk.co.zlurgg.mybookshelf.update.domain.repository.UpdatePreferencesRepository

val coreModule =
    module {
        // HTTP & Network
        single<HttpClientEngine> { Android.create() }
        single { HttpClientFactory.create(get(), enableLogging = BuildConfig.DEBUG) }
        single<ImageLoader> { ImageLoaderFactory.create(get<Context>()) }

        // Core Services
        singleOf(::UuidIdGenerator).bind<IdGenerator>()
        singleOf(::SystemTimeProvider).bind<TimeProvider>()
        singleOf(::AndroidSystemLanguageProvider).bind<SystemLanguageProvider>()

        // Database
        single<DatabaseFactory> { DatabaseFactory(get()) }
        single { get<DatabaseFactory>().create() }
        single { get<MyBookshelfRoomDatabase>().bookshelfDao }
        single { get<MyBookshelfRoomDatabase>().syncDao }
        single { get<MyBookshelfRoomDatabase>().bookClubDao }

        // Preferences (shared between welcome and update features)
        single { WelcomePreferencesImpl(get()) }
        single<WelcomePreferences> { get<WelcomePreferencesImpl>() }
        single<UpdatePreferencesRepository> { get<WelcomePreferencesImpl>() }
    }
