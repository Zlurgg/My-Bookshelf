package uk.co.zlurgg.mybookshelf.bookdetail.di

import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module
import uk.co.zlurgg.mybookshelf.bookdetail.domain.usecase.BookDetailUseCases
import uk.co.zlurgg.mybookshelf.bookdetail.domain.usecase.GetBookDescriptionUseCase
import uk.co.zlurgg.mybookshelf.bookdetail.domain.usecase.GetBookDescriptionUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookdetail.domain.usecase.GetBookDetailsUseCase
import uk.co.zlurgg.mybookshelf.bookdetail.domain.usecase.GetBookDetailsUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookdetail.domain.usecase.ToggleBookPurchaseUseCase
import uk.co.zlurgg.mybookshelf.bookdetail.domain.usecase.ToggleBookPurchaseUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookdetail.domain.usecase.UpdateBookDescriptionUseCase
import uk.co.zlurgg.mybookshelf.bookdetail.domain.usecase.UpdateBookDescriptionUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookdetail.domain.usecase.UpdateBookMetadataUseCase
import uk.co.zlurgg.mybookshelf.bookdetail.domain.usecase.UpdateBookMetadataUseCaseImpl
import uk.co.zlurgg.mybookshelf.bookdetail.presentation.BookDetailViewModel

val bookDetailModule = module {
    // Book Detail UseCases
    singleOf(::GetBookDetailsUseCaseImpl).bind<GetBookDetailsUseCase>()
    singleOf(::GetBookDescriptionUseCaseImpl).bind<GetBookDescriptionUseCase>()
    singleOf(::UpdateBookDescriptionUseCaseImpl).bind<UpdateBookDescriptionUseCase>()
    singleOf(::ToggleBookPurchaseUseCaseImpl).bind<ToggleBookPurchaseUseCase>()
    singleOf(::UpdateBookMetadataUseCaseImpl).bind<UpdateBookMetadataUseCase>()

    // UseCase Facade
    single {
        BookDetailUseCases(
            getBookDetails = get(),
            getBookDescription = get(),
            updateBookDescription = get(),
            addBookToShelf = get(),
            removeBookFromShelf = get(),
            upsertBook = get(),
            toggleBookPurchase = get(),
            updateBookMetadata = get()
        )
    }

    // ViewModels
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
