package uk.co.zlurgg.mybookshelf.book.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import uk.co.zlurgg.mybookshelf.book.data.network.KtorRemoteBookDataSource
import uk.co.zlurgg.mybookshelf.book.data.network.RemoteBookDataSource
import uk.co.zlurgg.mybookshelf.book.data.network.api.OpenLibraryApiService
import uk.co.zlurgg.mybookshelf.book.data.network.api.OpenLibraryBookApi
import uk.co.zlurgg.mybookshelf.book.data.repository.BookRepositoryImpl
import uk.co.zlurgg.mybookshelf.book.data.repository.BookcaseRepositoryImpl
import uk.co.zlurgg.mybookshelf.book.data.repository.BookshelfRepositoryImpl
import uk.co.zlurgg.mybookshelf.book.domain.usecase.AddBookToShelfUseCase
import uk.co.zlurgg.mybookshelf.book.domain.usecase.AddBookToShelfUseCaseImpl
import uk.co.zlurgg.mybookshelf.book.domain.usecase.RemoveBookFromShelfUseCase
import uk.co.zlurgg.mybookshelf.book.domain.usecase.RemoveBookFromShelfUseCaseImpl
import uk.co.zlurgg.mybookshelf.book.domain.usecase.UpsertBookUseCase
import uk.co.zlurgg.mybookshelf.book.domain.usecase.UpsertBookUseCaseImpl

val bookModule = module {
    // Network
    singleOf(::OpenLibraryApiService).bind<OpenLibraryBookApi>()
    singleOf(::KtorRemoteBookDataSource).bind<RemoteBookDataSource>()

    // Repositories — concrete types only, no interface binding.
    // SyncModule wires decorators that bind the interfaces.
    single { BookshelfRepositoryImpl(get(), get()) }
    single { BookcaseRepositoryImpl(get(), get(), get()) }
    single { BookRepositoryImpl(get(), get(), get(), get()) }

    // Shared UseCases (used by both bookdetail and bookshelf)
    singleOf(::AddBookToShelfUseCaseImpl).bind<AddBookToShelfUseCase>()
    singleOf(::RemoveBookFromShelfUseCaseImpl).bind<RemoveBookFromShelfUseCase>()
    singleOf(::UpsertBookUseCaseImpl).bind<UpsertBookUseCase>()
}
