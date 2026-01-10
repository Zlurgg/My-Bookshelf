package uk.co.zlurgg.mybookshelf.bookshelf.di

import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.network.KtorRemoteBookDataSource
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.network.RemoteBookDataSource
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.network.api.OpenLibraryApiService
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.network.api.OpenLibraryBookApi
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.repository.BookRepositoryImpl
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.repository.BookcaseRepositoryImpl
import uk.co.zlurgg.mybookshelf.bookshelf.data.book.repository.BookshelfRepositoryImpl
import uk.co.zlurgg.mybookshelf.bookshelf.data.mappers.BookshelfExportMapper
import uk.co.zlurgg.mybookshelf.bookshelf.data.service.AndroidBookshelfExportService
import uk.co.zlurgg.mybookshelf.bookshelf.data.service.AndroidShareService
import uk.co.zlurgg.mybookshelf.bookshelf.data.service.BookshelfImportValidatorImpl
import uk.co.zlurgg.mybookshelf.bookshelf.data.service.DatabaseBookshelfDataOrchestrator
import uk.co.zlurgg.mybookshelf.bookshelf.data.service.JsonBookshelfSerializer
import uk.co.zlurgg.mybookshelf.bookshelf.data.service.UrlEncodedShareTokenService
import uk.co.zlurgg.mybookshelf.bookshelf.data.service.WelcomeService
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookshelfRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookshelfDataOrchestrator
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookshelfExportService
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookshelfImportValidator
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookshelfSerializer
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.ShareTokenService
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.AddBookToShelfUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.AddBookToShelfUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.BookDetailUseCases
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.GetBookDetailsUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.GetBookDetailsUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.RemoveBookFromShelfUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.RemoveBookFromShelfUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.ToggleBookPurchaseUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.ToggleBookPurchaseUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.UpdateBookMetadataUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.UpdateBookMetadataUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.UpsertBookUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail.UpsertBookUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.BookcaseUseCases
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.CreateShelfUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.CreateShelfUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.DeleteShelfUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.DeleteShelfUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.DuplicateShelfUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.DuplicateShelfUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.GetAllShelvesUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.GetAllShelvesUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.GetShelfByIdUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.GetShelfByIdUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.RenameShelfUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.RenameShelfUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.ReorderShelvesUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.ReorderShelvesUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.UpdateShelfStyleUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.UpdateShelfStyleUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf.BookshelfUseCases
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf.GetShelfBooksUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf.GetShelfBooksUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf.SearchBooksUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf.SearchBooksUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf.ShareBookshelfUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf.ShareBookshelfUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf.UpdateShelfTidyModeUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf.UpdateShelfTidyModeUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.deeplink.DeepLinkImportUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.deeplink.DeepLinkImportUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.export.CheckImportConflictUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.export.CheckImportConflictUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.export.ExportBookshelfUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.export.ExportBookshelfUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.export.ImportBookshelfUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.export.ImportBookshelfUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.tutorial.GetOrCreateTutorialBookUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.tutorial.GetOrCreateTutorialBookUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.tutorial.GetOrCreateTutorialShelfUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.tutorial.GetOrCreateTutorialShelfUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.tutorial.HandleTutorialAccessUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.tutorial.HandleTutorialAccessUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.welcome.InitializeWelcomeUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.welcome.InitializeWelcomeUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.welcome.MarkWelcomeShownUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.welcome.MarkWelcomeShownUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.welcome.ShouldShowWelcomeUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.welcome.ShouldShowWelcomeUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.book_detail.BookDetailViewModel
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase.BookcaseViewModel
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase.handlers.ShelfManagementHandler
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase.handlers.ShelfOperationsHandler
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelf.BookshelfViewModel
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.deeplink.DeepLinkViewModel
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.welcome.WelcomeViewModel

val bookshelfModule = module {
    // Network
    singleOf(::OpenLibraryApiService).bind<OpenLibraryBookApi>()
    singleOf(::KtorRemoteBookDataSource).bind<RemoteBookDataSource>()

    // Repositories
    singleOf(::BookshelfRepositoryImpl).bind<BookshelfRepository>()
    singleOf(::BookcaseRepositoryImpl).bind<BookcaseRepository>()
    singleOf(::BookRepositoryImpl).bind<BookRepository>()

    // Export/Import Services
    single<ShareTokenService> { UrlEncodedShareTokenService() }
    singleOf(::AndroidBookshelfExportService).bind<BookshelfExportService>()
    singleOf(::AndroidShareService)
    singleOf(::JsonBookshelfSerializer).bind<BookshelfSerializer>()
    singleOf(::BookshelfImportValidatorImpl).bind<BookshelfImportValidator>()
    singleOf(::DatabaseBookshelfDataOrchestrator).bind<BookshelfDataOrchestrator>()
    singleOf(::BookshelfExportMapper)

    // Welcome & Tutorial Services
    single { WelcomeService(get()) }

    // Tutorial UseCases
    singleOf(::GetOrCreateTutorialBookUseCaseImpl).bind<GetOrCreateTutorialBookUseCase>()
    singleOf(::GetOrCreateTutorialShelfUseCaseImpl).bind<GetOrCreateTutorialShelfUseCase>()
    singleOf(::HandleTutorialAccessUseCaseImpl).bind<HandleTutorialAccessUseCase>()

    // Welcome UseCases
    singleOf(::InitializeWelcomeUseCaseImpl).bind<InitializeWelcomeUseCase>()
    singleOf(::ShouldShowWelcomeUseCaseImpl).bind<ShouldShowWelcomeUseCase>()
    singleOf(::MarkWelcomeShownUseCaseImpl).bind<MarkWelcomeShownUseCase>()

    // Export/Import UseCases
    singleOf(::ExportBookshelfUseCaseImpl).bind<ExportBookshelfUseCase>()
    singleOf(::ImportBookshelfUseCaseImpl).bind<ImportBookshelfUseCase>()
    singleOf(::CheckImportConflictUseCaseImpl).bind<CheckImportConflictUseCase>()

    // DeepLink UseCase
    singleOf(::DeepLinkImportUseCaseImpl).bind<DeepLinkImportUseCase>()

    // Book Detail UseCases
    singleOf(::AddBookToShelfUseCaseImpl).bind<AddBookToShelfUseCase>()
    singleOf(::RemoveBookFromShelfUseCaseImpl).bind<RemoveBookFromShelfUseCase>()
    singleOf(::GetBookDetailsUseCaseImpl).bind<GetBookDetailsUseCase>()
    singleOf(::UpsertBookUseCaseImpl).bind<UpsertBookUseCase>()
    singleOf(::ToggleBookPurchaseUseCaseImpl).bind<ToggleBookPurchaseUseCase>()
    singleOf(::UpdateBookMetadataUseCaseImpl).bind<UpdateBookMetadataUseCase>()

    // Bookshelf UseCases
    singleOf(::SearchBooksUseCaseImpl).bind<SearchBooksUseCase>()
    singleOf(::GetShelfBooksUseCaseImpl).bind<GetShelfBooksUseCase>()
    singleOf(::ShareBookshelfUseCaseImpl).bind<ShareBookshelfUseCase>()
    singleOf(::UpdateShelfTidyModeUseCaseImpl).bind<UpdateShelfTidyModeUseCase>()

    // Bookcase UseCases
    singleOf(::CreateShelfUseCaseImpl).bind<CreateShelfUseCase>()
    singleOf(::DeleteShelfUseCaseImpl).bind<DeleteShelfUseCase>()
    singleOf(::GetAllShelvesUseCaseImpl).bind<GetAllShelvesUseCase>()
    singleOf(::ReorderShelvesUseCaseImpl).bind<ReorderShelvesUseCase>()
    singleOf(::GetShelfByIdUseCaseImpl).bind<GetShelfByIdUseCase>()
    singleOf(::RenameShelfUseCaseImpl).bind<RenameShelfUseCase>()
    singleOf(::UpdateShelfStyleUseCaseImpl).bind<UpdateShelfStyleUseCase>()
    singleOf(::DuplicateShelfUseCaseImpl).bind<DuplicateShelfUseCase>()

    // UseCase Facades
    single {
        BookDetailUseCases(
            addBookToShelf = get(),
            removeBookFromShelf = get(),
            getBookDetails = get(),
            upsertBook = get(),
            toggleBookPurchase = get(),
            updateBookMetadata = get()
        )
    }
    single {
        BookshelfUseCases(
            searchBooks = get(),
            getShelfBooks = get(),
            addBookToShelf = get(),
            removeBookFromShelf = get(),
            upsertBook = get(),
            shareBookshelf = get(),
            updateShelfTidyMode = get()
        )
    }
    single {
        BookcaseUseCases(
            getAllShelves = get(),
            createShelf = get(),
            deleteShelf = get(),
            reorderShelves = get(),
            getShelfById = get(),
            renameShelf = get(),
            updateShelfStyle = get(),
            duplicateShelf = get(),
            shareShelf = get()
        )
    }

    // Presentation Handlers
    single { ShelfOperationsHandler(get()) }
    single { ShelfManagementHandler(get(), get(), get()) }

    // ViewModels
    viewModelOf(::DeepLinkViewModel)
    viewModelOf(::WelcomeViewModel)

    viewModel { (shelfId: String) ->
        BookshelfViewModel(
            bookshelfUseCases = get(),
            bookcaseUseCases = get(),
            bookClubOperations = get(),
            shelfId = shelfId
        )
    }

    viewModel {
        BookcaseViewModel(
            shelfOperations = get(),
            shelfManagement = get(),
            bookcaseUseCases = get(),
            bookClubOperations = get(),
            checkForUpdateUseCase = get(),
            downloadUpdateUseCase = get(),
            dismissUpdateUseCase = get(),
            getCurrentVersionInfoUseCase = get(),
            checkSignInStatusUseCase = get(),
            signOutUseCase = get(),
            getCurrentUserIdUseCase = get()
        )
    }

    viewModel { (bookId: String, shelfId: String) ->
        BookDetailViewModel(
            bookDetailUseCases = get(),
            bookClubUseCases = get(),
            getCurrentUserIdUseCase = get(),
            bookId = bookId,
            shelfId = shelfId
        )
    }
}
