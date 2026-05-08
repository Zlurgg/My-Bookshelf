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
import uk.co.zlurgg.mybookshelf.book.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.book.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.book.domain.repository.BookshelfRepository
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

    // Repositories
    single<BookshelfRepository> { BookshelfRepositoryImpl(get(), get()) }
    single<BookcaseRepository> { BookcaseRepositoryImpl(get(), get()) }
    single<BookRepository> { BookRepositoryImpl(get(), get()) }

    // Shared UseCases (used by both bookdetail and bookshelf)
    singleOf(::AddBookToShelfUseCaseImpl).bind<AddBookToShelfUseCase>()
    singleOf(::RemoveBookFromShelfUseCaseImpl).bind<RemoveBookFromShelfUseCase>()
    singleOf(::UpsertBookUseCaseImpl).bind<UpsertBookUseCase>()
}
