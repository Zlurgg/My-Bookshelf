package uk.co.zlurgg.mybookshelf.di

import android.content.Context
import coil3.ImageLoader
import uk.co.zlurgg.mybookshelf.BuildConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.android.Android
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.BookshelfDatabase
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.database.DatabaseFactory
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.network.KtorRemoteBookDataSource
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.network.RemoteBookDataSource
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.repository.BookcaseRepositoryImpl
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.repository.BookRepositoryImpl
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.repository.BookshelfRepositoryImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookSorter
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf.SearchBooksUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf.SearchBooksUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.data.service.AndroidBookshelfExportService
import uk.co.zlurgg.mybookshelf.bookshelf.data.service.AndroidSystemLanguageProvider
import uk.co.zlurgg.mybookshelf.bookshelf.data.service.LocalShareTokenService
import uk.co.zlurgg.mybookshelf.bookshelf.data.service.SystemTimeProvider
import uk.co.zlurgg.mybookshelf.bookshelf.data.service.UuidBookshelfIdGenerator
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookshelfRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookshelfExportService
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookshelfIdGenerator
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.ShareTokenService
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.SystemLanguageProvider
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.TimeProvider
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.book_detail.BookDetailViewModel
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase.BookcaseViewModel
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelf.BookshelfViewModel
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.shared.SharedMyBookshelfViewModel
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.deeplink.DeepLinkImportUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.deeplink.DeepLinkImportUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.AddBookToShelfUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.AddBookToShelfUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.RemoveBookFromShelfUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.RemoveBookFromShelfUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.GetBookDetailsUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.GetBookDetailsUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.BookDetailUseCases
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf.GetShelfBooksUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf.GetShelfBooksUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf.BookshelfUseCases
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.deeplink.DeepLinkViewModel
import uk.co.zlurgg.mybookshelf.core.data.HttpClientFactory
import uk.co.zlurgg.mybookshelf.core.data.ImageLoaderFactory

val appModule = module {
    single<HttpClientEngine> {
        Android.create()
    }
    single { HttpClientFactory.create(get(), enableLogging = BuildConfig.DEBUG) }
    single<ImageLoader> { ImageLoaderFactory.create(get<Context>()) }

    singleOf(::KtorRemoteBookDataSource).bind<RemoteBookDataSource>()
    singleOf(::UuidBookshelfIdGenerator).bind<BookshelfIdGenerator>()
    singleOf(::SystemTimeProvider).bind<TimeProvider>()
    singleOf(::AndroidSystemLanguageProvider).bind<SystemLanguageProvider>()
    single<ShareTokenService> {
        LocalShareTokenService(
            tokenGenerator = get(),
            timeProvider = get()
        )
    }
    singleOf(::AndroidBookshelfExportService).bind<BookshelfExportService>()
    singleOf(::BookSorter)
    singleOf(::SearchBooksUseCaseImpl).bind<SearchBooksUseCase>()
    singleOf(::DeepLinkImportUseCaseImpl).bind<DeepLinkImportUseCase>()
    singleOf(::AddBookToShelfUseCaseImpl).bind<AddBookToShelfUseCase>()
    singleOf(::RemoveBookFromShelfUseCaseImpl).bind<RemoveBookFromShelfUseCase>()
    singleOf(::GetBookDetailsUseCaseImpl).bind<GetBookDetailsUseCase>()
    singleOf(::GetShelfBooksUseCaseImpl).bind<GetShelfBooksUseCase>()

    // UseCase Facades
    single { BookDetailUseCases(get(), get(), get()) }
    single { BookshelfUseCases(get(), get(), get(), get()) }

    single<DatabaseFactory> { DatabaseFactory(get()) }

    single {
        get<DatabaseFactory>().create()
            .build()
    }
    single { get<BookshelfDatabase>().bookshelfDao }

    // Shared shelves VM
    viewModelOf(::SharedMyBookshelfViewModel)
    viewModelOf(::DeepLinkViewModel)

    viewModel { (shelfId: String) ->
        BookshelfViewModel(
            bookRepository = get(),
            bookshelfExportService = get(),
            bookshelfUseCases = get(),
            shelfId = shelfId
        )
    }
    viewModel { BookcaseViewModel(get(), get(), get()) }
    viewModel { (bookId: String, shelfId: String) ->
        BookDetailViewModel(
            bookRepository = get(),
            bookDetailUseCases = get(),
            bookId = bookId,
            shelfId = shelfId
        )
    }

    // Domain layer repositories
    singleOf(::BookshelfRepositoryImpl).bind<BookshelfRepository>()
    singleOf(::BookcaseRepositoryImpl).bind<BookcaseRepository>()
    singleOf(::BookRepositoryImpl).bind<BookRepository>()
}
