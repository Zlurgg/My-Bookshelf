package uk.co.zlurgg.mybookshelf.di

import org.koin.dsl.module
import uk.co.zlurgg.mybookshelf.auth.di.authModule
import uk.co.zlurgg.mybookshelf.book.di.bookModule
import uk.co.zlurgg.mybookshelf.bookclub.di.bookClubModule
import uk.co.zlurgg.mybookshelf.bookcase.di.bookcaseModule
import uk.co.zlurgg.mybookshelf.bookshelf.di.bookshelfModule
import uk.co.zlurgg.mybookshelf.core.di.coreModule
import uk.co.zlurgg.mybookshelf.sharing.di.sharingModule
import uk.co.zlurgg.mybookshelf.sync.di.syncModule
import uk.co.zlurgg.mybookshelf.welcome.di.welcomeModule

/**
 * Root Koin module that aggregates all feature-scoped modules.
 *
 * Module hierarchy:
 * - coreModule: Infrastructure (database, network, preferences, core services)
 * - authModule: Authentication (Google Sign-In, auth state, user provider)
 * - syncModule: Cloud sync (Firestore sync engine, connectivity, migration)
 * - bookClubModule: Book club feature (16 use cases, handler)
 * - bookshelfModule: Main bookshelf feature (repositories, use cases, ViewModels)
 * - debugModule: Debug-only dependencies (only in debug builds via DebugModuleProvider)
 */
val appModule = module {
    includes(
        coreModule,
        authModule,
        syncModule,
        bookModule,
        bookClubModule,
        bookcaseModule,
        bookshelfModule,
        sharingModule,
        welcomeModule
    )
    DebugModuleProvider.getModules().forEach { includes(it) }
}
