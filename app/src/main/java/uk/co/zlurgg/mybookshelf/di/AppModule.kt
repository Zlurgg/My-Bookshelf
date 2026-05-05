package uk.co.zlurgg.mybookshelf.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import uk.co.zlurgg.mybookshelf.app.presentation.theme.ThemeViewModel
import uk.co.zlurgg.mybookshelf.account.di.accountModule
import uk.co.zlurgg.mybookshelf.auth.di.authModule
import uk.co.zlurgg.mybookshelf.book.di.bookModule
import uk.co.zlurgg.mybookshelf.bookclub.di.bookClubModule
import uk.co.zlurgg.mybookshelf.bookdetail.di.bookDetailModule
import uk.co.zlurgg.mybookshelf.bookcase.di.bookcaseModule
import uk.co.zlurgg.mybookshelf.bookshelf.di.bookshelfModule
import uk.co.zlurgg.mybookshelf.core.di.coreModule
import uk.co.zlurgg.mybookshelf.sync.di.syncModule
import uk.co.zlurgg.mybookshelf.welcome.di.welcomeModule

/**
 * Root Koin module that aggregates all feature-scoped modules.
 *
 * Module hierarchy:
 * - coreModule: Infrastructure (database, network, preferences, core services)
 * - authModule: Authentication (Google Sign-In, auth state, user provider)
 * - syncModule: Cloud sync (Firestore sync engine, connectivity, migration)
 * - bookModule: Shared book domain (models, repos, network, shared use cases)
 * - bookDetailModule: Book detail screen (view/edit book metadata)
 * - bookClubModule: Book club feature (use cases, handler)
 * - bookcaseModule: Home screen (shelf list, create/delete/rename shelves)
 * - bookshelfModule: Shelf screen (viewing books, searching, adding)
 * - welcomeModule: Tutorial, onboarding
 * - debugModule: Debug-only dependencies (only in debug builds via DebugModuleProvider)
 */
val appModule = module {
    includes(
        coreModule,
        accountModule,
        authModule,
        syncModule,
        bookModule,
        bookClubModule,
        bookDetailModule,
        bookcaseModule,
        bookshelfModule,
        welcomeModule
    )
    DebugModuleProvider.getModules().forEach { includes(it) }

    viewModelOf(::ThemeViewModel)
}
