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
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.network.api.OpenLibraryApiService
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.network.api.OpenLibraryBookApi
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.repository.BookcaseRepositoryImpl
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.repository.BookRepositoryImpl
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.repository.BookshelfRepositoryImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookSorter
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf.SearchBooksUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf.SearchBooksUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.data.service.AndroidBookshelfExportService
import uk.co.zlurgg.mybookshelf.bookshelf.data.service.AndroidShareService
import uk.co.zlurgg.mybookshelf.bookshelf.data.service.BookshelfImportValidatorImpl
import uk.co.zlurgg.mybookshelf.bookshelf.data.service.DatabaseBookshelfDataOrchestrator
import uk.co.zlurgg.mybookshelf.bookshelf.data.service.JsonBookshelfSerializer
import uk.co.zlurgg.mybookshelf.bookshelf.data.service.LocalShareTokenService
import uk.co.zlurgg.mybookshelf.bookshelf.data.mappers.BookshelfExportMapper
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookshelfDataOrchestrator
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookshelfImportValidator
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookshelfSerializer
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.export.CheckImportConflictUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.export.ExportBookshelfUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.export.ImportBookshelfUseCase
import uk.co.zlurgg.mybookshelf.core.data.service.AndroidSystemLanguageProvider
import uk.co.zlurgg.mybookshelf.core.data.service.SystemTimeProvider
import uk.co.zlurgg.mybookshelf.core.data.service.UuidIdGenerator
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookshelfRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookshelfExportService
import uk.co.zlurgg.mybookshelf.core.domain.service.IdGenerator
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.ShareTokenService
import uk.co.zlurgg.mybookshelf.core.domain.service.SystemLanguageProvider
import uk.co.zlurgg.mybookshelf.core.domain.service.TimeProvider
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.book_detail.BookDetailViewModel
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase.BookcaseViewModel
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelf.BookshelfViewModel
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
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.CreateShelfUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.CreateShelfUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.DeleteShelfUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.DeleteShelfUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.GetAllShelvesUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.GetAllShelvesUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.ReorderShelvesUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.ReorderShelvesUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.GetShelfByIdUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.GetShelfByIdUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.BookcaseUseCases
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.UpsertBookUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.UpsertBookUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.ToggleBookPurchaseUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.ToggleBookPurchaseUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf.ShareBookshelfUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf.ShareBookshelfUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.deeplink.DeepLinkViewModel
import uk.co.zlurgg.mybookshelf.core.data.network.HttpClientFactory
import uk.co.zlurgg.mybookshelf.core.data.image.ImageLoaderFactory

val appModule = module {
    single<HttpClientEngine> {
        Android.create()
    }
    single { HttpClientFactory.create(get(), enableLogging = BuildConfig.DEBUG) }
    single<ImageLoader> { ImageLoaderFactory.create(get<Context>()) }

    singleOf(::OpenLibraryApiService).bind<OpenLibraryBookApi>()
    singleOf(::KtorRemoteBookDataSource).bind<RemoteBookDataSource>()
    singleOf(::UuidIdGenerator).bind<IdGenerator>()
    singleOf(::SystemTimeProvider).bind<TimeProvider>()
    singleOf(::AndroidSystemLanguageProvider).bind<SystemLanguageProvider>()
    single<ShareTokenService> {
        LocalShareTokenService(
            tokenGenerator = get(),
            timeProvider = get()
        )
    }
    singleOf(::AndroidBookshelfExportService).bind<BookshelfExportService>()

    // Export/Import Services and Use Cases
    singleOf(::AndroidShareService)
    singleOf(::JsonBookshelfSerializer).bind<BookshelfSerializer>()
    singleOf(::BookshelfImportValidatorImpl).bind<BookshelfImportValidator>()
    singleOf(::DatabaseBookshelfDataOrchestrator).bind<BookshelfDataOrchestrator>()
    singleOf(::BookshelfExportMapper)
    singleOf(::ExportBookshelfUseCase)
    singleOf(::ImportBookshelfUseCase)
    singleOf(::CheckImportConflictUseCase)

    singleOf(::BookSorter)
    singleOf(::SearchBooksUseCaseImpl).bind<SearchBooksUseCase>()
    singleOf(::DeepLinkImportUseCaseImpl).bind<DeepLinkImportUseCase>()
    singleOf(::AddBookToShelfUseCaseImpl).bind<AddBookToShelfUseCase>()
    singleOf(::RemoveBookFromShelfUseCaseImpl).bind<RemoveBookFromShelfUseCase>()
    singleOf(::GetBookDetailsUseCaseImpl).bind<GetBookDetailsUseCase>()
    singleOf(::GetShelfBooksUseCaseImpl).bind<GetShelfBooksUseCase>()
    singleOf(::CreateShelfUseCaseImpl).bind<CreateShelfUseCase>()
    singleOf(::DeleteShelfUseCaseImpl).bind<DeleteShelfUseCase>()
    singleOf(::GetAllShelvesUseCaseImpl).bind<GetAllShelvesUseCase>()
    singleOf(::ReorderShelvesUseCaseImpl).bind<ReorderShelvesUseCase>()
    singleOf(::GetShelfByIdUseCaseImpl).bind<GetShelfByIdUseCase>()
    singleOf(::UpsertBookUseCaseImpl).bind<UpsertBookUseCase>()
    singleOf(::ToggleBookPurchaseUseCaseImpl).bind<ToggleBookPurchaseUseCase>()
    singleOf(::ShareBookshelfUseCaseImpl).bind<ShareBookshelfUseCase>()

    // UseCase Facades
    single { BookDetailUseCases(get(), get(), get(), get(), get()) }
    single { BookshelfUseCases(get(), get(), get(), get(), get(), get()) }
    single { BookcaseUseCases(get(), get(), get(), get(), get()) }

    single<DatabaseFactory> { DatabaseFactory(get()) }

    single {
        get<DatabaseFactory>().create()
            .build()
    }
    single { get<BookshelfDatabase>().bookshelfDao }

    viewModelOf(::DeepLinkViewModel)

    viewModel { (shelfId: String) ->
        BookshelfViewModel(
            bookshelfUseCases = get(),
            bookcaseUseCases = get(),
            shelfId = shelfId
        )
    }
    viewModel { BookcaseViewModel(get()) }
    viewModel { (bookId: String, shelfId: String) ->
        BookDetailViewModel(
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
