package uk.co.zlurgg.mybookshelf.bookshelf.di

import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookdetail.AddBookToShelfUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookdetail.AddBookToShelfUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookdetail.BookDetailUseCases
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookdetail.GetBookDetailsUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookdetail.GetBookDetailsUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookdetail.RemoveBookFromShelfUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookdetail.RemoveBookFromShelfUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookdetail.ToggleBookPurchaseUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookdetail.ToggleBookPurchaseUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookdetail.UpdateBookMetadataUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookdetail.UpdateBookMetadataUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookdetail.UpsertBookUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookdetail.UpsertBookUseCaseImpl
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
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookdetail.BookDetailViewModel
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase.BookcaseViewModel
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase.handlers.ShelfManagementHandler
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase.handlers.ShelfOperationsHandler
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelf.BookshelfViewModel

val bookshelfModule = module {
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
            authUseCases = get()
        )
    }

    viewModel { (bookId: String, shelfId: String) ->
        BookDetailViewModel(
            bookDetailUseCases = get(),
            bookReviewProvider = get(),
            authUseCases = get(),
            bookId = bookId,
            shelfId = shelfId
        )
    }
}
