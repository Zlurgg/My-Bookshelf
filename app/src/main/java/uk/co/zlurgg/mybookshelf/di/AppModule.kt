package uk.co.zlurgg.mybookshelf.di

import org.koin.dsl.module
import uk.co.zlurgg.mybookshelf.auth.di.authModule
import uk.co.zlurgg.mybookshelf.bookshelf.di.bookClubModule
import uk.co.zlurgg.mybookshelf.bookshelf.di.bookshelfModule
import uk.co.zlurgg.mybookshelf.core.di.coreModule
import uk.co.zlurgg.mybookshelf.sync.di.syncModule
import uk.co.zlurgg.mybookshelf.update.di.updateModule

/**
 * Root Koin module that aggregates all feature-scoped modules.
 *
 * Module hierarchy:
 * - coreModule: Infrastructure (database, network, preferences, core services)
 * - authModule: Authentication (Google Sign-In, auth state, user provider)
 * - syncModule: Cloud sync (Firestore sync engine, connectivity, migration)
 * - updateModule: In-app updates (GitHub release checking, APK download)
 * - bookClubModule: Book club feature (16 use cases, handler)
 * - bookshelfModule: Main bookshelf feature (repositories, use cases, ViewModels)
 */
val appModule = module {
    includes(
        coreModule,
        authModule,
        syncModule,
        updateModule,
        bookClubModule,
        bookshelfModule
    )
}
