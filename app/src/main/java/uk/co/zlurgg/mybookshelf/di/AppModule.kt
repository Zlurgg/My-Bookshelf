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
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf.SearchBooksUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf.SearchBooksUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.data.service.AndroidBookshelfExportService
import uk.co.zlurgg.mybookshelf.bookshelf.data.service.AndroidShareService
import uk.co.zlurgg.mybookshelf.bookshelf.data.service.BookshelfImportValidatorImpl
import uk.co.zlurgg.mybookshelf.bookshelf.data.service.DatabaseBookshelfDataOrchestrator
import uk.co.zlurgg.mybookshelf.bookshelf.data.service.JsonBookshelfSerializer
import uk.co.zlurgg.mybookshelf.bookshelf.data.service.UrlEncodedShareTokenService
import uk.co.zlurgg.mybookshelf.bookshelf.data.mappers.BookshelfExportMapper
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookshelfDataOrchestrator
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookshelfImportValidator
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookshelfSerializer
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.export.CheckImportConflictUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.export.CheckImportConflictUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.export.ExportBookshelfUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.export.ExportBookshelfUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.export.ImportBookshelfUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.export.ImportBookshelfUseCaseImpl
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
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase.handlers.ShelfManagementHandler
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase.handlers.ShelfOperationsHandler
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
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.RenameShelfUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.RenameShelfUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.UpdateShelfStyleUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.UpdateShelfStyleUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.DuplicateShelfUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.DuplicateShelfUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.tutorial.GetOrCreateTutorialShelfUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.tutorial.GetOrCreateTutorialShelfUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.tutorial.GetOrCreateTutorialBookUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.tutorial.GetOrCreateTutorialBookUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.tutorial.HandleTutorialAccessUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.tutorial.HandleTutorialAccessUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.welcome.InitializeWelcomeUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.welcome.InitializeWelcomeUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.BookcaseUseCases
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.UpsertBookUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.UpsertBookUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.ToggleBookPurchaseUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.ToggleBookPurchaseUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.UpdateBookMetadataUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.UpdateBookMetadataUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf.ShareBookshelfUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf.ShareBookshelfUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf.UpdateShelfTidyModeUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf.UpdateShelfTidyModeUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.deeplink.DeepLinkViewModel
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.welcome.WelcomeViewModel
import uk.co.zlurgg.mybookshelf.core.data.network.HttpClientFactory
import uk.co.zlurgg.mybookshelf.core.data.image.ImageLoaderFactory
import uk.co.zlurgg.mybookshelf.core.data.preferences.WelcomePreferencesImpl
import uk.co.zlurgg.mybookshelf.core.domain.preferences.WelcomePreferences
import uk.co.zlurgg.mybookshelf.update.data.remote.api.GitHubApiService
import uk.co.zlurgg.mybookshelf.update.data.repository.UpdateRepositoryImpl
import uk.co.zlurgg.mybookshelf.update.data.service.ApkDownloadService
import uk.co.zlurgg.mybookshelf.update.domain.model.UpdateConfig
import uk.co.zlurgg.mybookshelf.update.domain.repository.UpdatePreferencesRepository
import uk.co.zlurgg.mybookshelf.update.domain.repository.UpdateRepository
import uk.co.zlurgg.mybookshelf.update.domain.usecases.CheckForUpdateUseCase
import uk.co.zlurgg.mybookshelf.update.domain.usecases.CheckForUpdateUseCaseImpl
import uk.co.zlurgg.mybookshelf.update.domain.usecases.DismissUpdateUseCase
import uk.co.zlurgg.mybookshelf.update.domain.usecases.DismissUpdateUseCaseImpl
import uk.co.zlurgg.mybookshelf.update.domain.usecases.DownloadUpdateUseCase
import uk.co.zlurgg.mybookshelf.update.domain.usecases.DownloadUpdateUseCaseImpl
import uk.co.zlurgg.mybookshelf.update.domain.usecases.GetCurrentVersionInfoUseCase
import uk.co.zlurgg.mybookshelf.update.domain.usecases.GetCurrentVersionInfoUseCaseImpl

private const val GITHUB_OWNER = "Zlurgg"
private const val GITHUB_REPO = "My-Bookshelf"
private const val APP_NAME = "my-bookshelf"

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
    single { WelcomePreferencesImpl(get()) }
    single<WelcomePreferences> { get<WelcomePreferencesImpl>() }
    single<UpdatePreferencesRepository> { get<WelcomePreferencesImpl>() }
    single<ShareTokenService> {
        UrlEncodedShareTokenService()
    }
    singleOf(::AndroidBookshelfExportService).bind<BookshelfExportService>()

    // Welcome & Tutorial
    single { uk.co.zlurgg.mybookshelf.bookshelf.data.service.WelcomeService(get()) }
    singleOf(::GetOrCreateTutorialBookUseCaseImpl).bind<GetOrCreateTutorialBookUseCase>()
    singleOf(::GetOrCreateTutorialShelfUseCaseImpl).bind<GetOrCreateTutorialShelfUseCase>()
    singleOf(::HandleTutorialAccessUseCaseImpl).bind<HandleTutorialAccessUseCase>()
    singleOf(::InitializeWelcomeUseCaseImpl).bind<InitializeWelcomeUseCase>()

    // Export/Import Services and Use Cases
    singleOf(::AndroidShareService)
    singleOf(::JsonBookshelfSerializer).bind<BookshelfSerializer>()
    singleOf(::BookshelfImportValidatorImpl).bind<BookshelfImportValidator>()
    singleOf(::DatabaseBookshelfDataOrchestrator).bind<BookshelfDataOrchestrator>()
    singleOf(::BookshelfExportMapper)
    singleOf(::ExportBookshelfUseCaseImpl).bind<ExportBookshelfUseCase>()
    singleOf(::ImportBookshelfUseCaseImpl).bind<ImportBookshelfUseCase>()
    singleOf(::CheckImportConflictUseCaseImpl).bind<CheckImportConflictUseCase>()

    // In-App Update Feature
    single {
        UpdateConfig(
            gitHubOwner = GITHUB_OWNER,
            gitHubRepo = GITHUB_REPO,
            appName = APP_NAME
        )
    }
    single { GitHubApiService(httpClient = get()) }
    single { ApkDownloadService(context = get<Context>(), downloadTitle = get<UpdateConfig>().downloadTitle) }
    singleOf(::UpdateRepositoryImpl).bind<UpdateRepository>()
    single<CheckForUpdateUseCase> { CheckForUpdateUseCaseImpl(get(), get(), BuildConfig.VERSION_NAME) }
    single<DismissUpdateUseCase> { DismissUpdateUseCaseImpl(get()) }
    single<DownloadUpdateUseCase> { DownloadUpdateUseCaseImpl(get(), get()) }
    single<GetCurrentVersionInfoUseCase> { GetCurrentVersionInfoUseCaseImpl(get(), BuildConfig.VERSION_NAME) }

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
    singleOf(::RenameShelfUseCaseImpl).bind<RenameShelfUseCase>()
    singleOf(::UpdateShelfStyleUseCaseImpl).bind<UpdateShelfStyleUseCase>()
    singleOf(::DuplicateShelfUseCaseImpl).bind<DuplicateShelfUseCase>()
    singleOf(::UpsertBookUseCaseImpl).bind<UpsertBookUseCase>()
    singleOf(::ToggleBookPurchaseUseCaseImpl).bind<ToggleBookPurchaseUseCase>()
    singleOf(::UpdateBookMetadataUseCaseImpl).bind<UpdateBookMetadataUseCase>()
    singleOf(::ShareBookshelfUseCaseImpl).bind<ShareBookshelfUseCase>()
    singleOf(::UpdateShelfTidyModeUseCaseImpl).bind<UpdateShelfTidyModeUseCase>()

    // UseCase Facades
    single { BookDetailUseCases(get(), get(), get(), get(), get(), get()) }
    single { BookshelfUseCases(get(), get(), get(), get(), get(), get(), get()) }
    single { BookcaseUseCases(get(), get(), get(), get(), get(), get(), get(), get(), get()) }

    // Presentation Handlers
    single { ShelfOperationsHandler(get()) }
    single { ShelfManagementHandler(get(), get()) }

    single<DatabaseFactory> { DatabaseFactory(get()) }

    single {
        get<DatabaseFactory>().create()
            .build()
    }
    single { get<BookshelfDatabase>().bookshelfDao }

    viewModelOf(::DeepLinkViewModel)
    viewModelOf(::WelcomeViewModel)

    viewModel { (shelfId: String) ->
        BookshelfViewModel(
            bookshelfUseCases = get(),
            bookcaseUseCases = get(),
            shelfId = shelfId
        )
    }
    viewModel {
        BookcaseViewModel(
            shelfOperations = get(),
            shelfManagement = get(),
            bookcaseUseCases = get(),
            checkForUpdateUseCase = get(),
            downloadUpdateUseCase = get(),
            dismissUpdateUseCase = get(),
            getCurrentVersionInfoUseCase = get()
        )
    }
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
