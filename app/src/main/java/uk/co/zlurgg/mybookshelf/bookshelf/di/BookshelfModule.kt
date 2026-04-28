package uk.co.zlurgg.mybookshelf.bookshelf.di

import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module
import uk.co.zlurgg.mybookshelf.bookcase.domain.usecase.GetShelfByIdUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf.BookshelfUseCases
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf.GetShelfBooksUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf.GetShelfBooksUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf.SearchBooksUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf.SearchBooksUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf.ShareBookshelfUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf.ShareBookshelfUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf.UpdateShelfTidyModeUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf.UpdateShelfTidyModeUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookshelf.BookshelfViewModel

val bookshelfModule = module {
    // Bookshelf UseCases
    singleOf(::SearchBooksUseCaseImpl).bind<SearchBooksUseCase>()
    singleOf(::GetShelfBooksUseCaseImpl).bind<GetShelfBooksUseCase>()
    singleOf(::ShareBookshelfUseCaseImpl).bind<ShareBookshelfUseCase>()
    singleOf(::UpdateShelfTidyModeUseCaseImpl).bind<UpdateShelfTidyModeUseCase>()

    // UseCase Facade
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

    // ViewModels
    viewModel { (shelfId: String) ->
        BookshelfViewModel(
            bookshelfUseCases = get(),
            getShelfById = get(),
            bookClubOperations = get(),
            shelfId = shelfId
        )
    }
}
