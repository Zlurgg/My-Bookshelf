package uk.co.zlurgg.mybookshelf.book.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import uk.co.zlurgg.mybookshelf.book.data.network.FallbackRemoteBookDataSource
import uk.co.zlurgg.mybookshelf.book.data.network.GoogleBooksRemoteBookDataSource
import uk.co.zlurgg.mybookshelf.book.data.network.OpenLibraryRemoteBookDataSource
import uk.co.zlurgg.mybookshelf.book.data.network.RemoteBookDataSource
import uk.co.zlurgg.mybookshelf.book.data.network.api.GoogleBooksApiService
import uk.co.zlurgg.mybookshelf.book.data.network.api.GoogleBooksBookApi
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
import uk.co.zlurgg.mybookshelf.book.domain.usecase.SearchBooksUseCase
import uk.co.zlurgg.mybookshelf.book.domain.usecase.SearchBooksUseCaseImpl
import uk.co.zlurgg.mybookshelf.book.domain.usecase.SearchLibraryBooksUseCase
import uk.co.zlurgg.mybookshelf.book.domain.usecase.SearchLibraryBooksUseCaseImpl
import uk.co.zlurgg.mybookshelf.book.domain.usecase.UpsertBookUseCase
import uk.co.zlurgg.mybookshelf.book.domain.usecase.UpsertBookUseCaseImpl

val bookModule = module {
    // Network — both providers.
    // Google classes use explicit `single { … }` (not `singleOf`) because they have an
    // `apiKeyProvider: () -> String` test seam with a default — `singleOf` ignores
    // Kotlin defaults and would try to resolve Function0 from the DI graph.
    single<GoogleBooksBookApi> { GoogleBooksApiService(httpClient = get(), attestation = get()) }
    singleOf(::OpenLibraryApiService).bind<OpenLibraryBookApi>()

    // Data sources — both providers
    single { GoogleBooksRemoteBookDataSource(apiService = get(), systemLanguageProvider = get()) }
    singleOf(::OpenLibraryRemoteBookDataSource)

    // Fallback wrapper as the single RemoteBookDataSource
    single<RemoteBookDataSource> {
        FallbackRemoteBookDataSource(
            primary = get<GoogleBooksRemoteBookDataSource>(),
            fallback = get<OpenLibraryRemoteBookDataSource>()
        )
    }

    // Repositories
    single<BookshelfRepository> { BookshelfRepositoryImpl(get(), get()) }
    single<BookcaseRepository> { BookcaseRepositoryImpl(get(), get()) }
    single<BookRepository> { BookRepositoryImpl(get(), get()) }

    // Shared UseCases (used by bookdetail, bookshelf, library)
    singleOf(::AddBookToShelfUseCaseImpl).bind<AddBookToShelfUseCase>()
    singleOf(::RemoveBookFromShelfUseCaseImpl).bind<RemoveBookFromShelfUseCase>()
    singleOf(::SearchBooksUseCaseImpl).bind<SearchBooksUseCase>()
    singleOf(::SearchLibraryBooksUseCaseImpl).bind<SearchLibraryBooksUseCase>()
    singleOf(::UpsertBookUseCaseImpl).bind<UpsertBookUseCase>()
}
