package uk.co.zlurgg.mybookshelf.book.di

import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
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
import uk.co.zlurgg.mybookshelf.book.domain.usecase.bookdetail.AddBookToShelfUseCase
import uk.co.zlurgg.mybookshelf.book.domain.usecase.bookdetail.AddBookToShelfUseCaseImpl
import uk.co.zlurgg.mybookshelf.book.domain.usecase.bookdetail.BookDetailUseCases
import uk.co.zlurgg.mybookshelf.book.domain.usecase.bookdetail.GetBookDetailsUseCase
import uk.co.zlurgg.mybookshelf.book.domain.usecase.bookdetail.GetBookDetailsUseCaseImpl
import uk.co.zlurgg.mybookshelf.book.domain.usecase.bookdetail.RemoveBookFromShelfUseCase
import uk.co.zlurgg.mybookshelf.book.domain.usecase.bookdetail.RemoveBookFromShelfUseCaseImpl
import uk.co.zlurgg.mybookshelf.book.domain.usecase.bookdetail.ToggleBookPurchaseUseCase
import uk.co.zlurgg.mybookshelf.book.domain.usecase.bookdetail.ToggleBookPurchaseUseCaseImpl
import uk.co.zlurgg.mybookshelf.book.domain.usecase.bookdetail.UpdateBookMetadataUseCase
import uk.co.zlurgg.mybookshelf.book.domain.usecase.bookdetail.UpdateBookMetadataUseCaseImpl
import uk.co.zlurgg.mybookshelf.book.domain.usecase.bookdetail.UpsertBookUseCase
import uk.co.zlurgg.mybookshelf.book.domain.usecase.bookdetail.UpsertBookUseCaseImpl
import uk.co.zlurgg.mybookshelf.book.presentation.bookdetail.BookDetailViewModel

val bookModule = module {
    // Network
    singleOf(::OpenLibraryApiService).bind<OpenLibraryBookApi>()
    singleOf(::KtorRemoteBookDataSource).bind<RemoteBookDataSource>()

    // Repositories
    singleOf(::BookshelfRepositoryImpl).bind<BookshelfRepository>()
    singleOf(::BookcaseRepositoryImpl).bind<BookcaseRepository>()
    singleOf(::BookRepositoryImpl).bind<BookRepository>()

    // Book Detail UseCases
    singleOf(::AddBookToShelfUseCaseImpl).bind<AddBookToShelfUseCase>()
    singleOf(::RemoveBookFromShelfUseCaseImpl).bind<RemoveBookFromShelfUseCase>()
    singleOf(::GetBookDetailsUseCaseImpl).bind<GetBookDetailsUseCase>()
    singleOf(::UpsertBookUseCaseImpl).bind<UpsertBookUseCase>()
    singleOf(::ToggleBookPurchaseUseCaseImpl).bind<ToggleBookPurchaseUseCase>()
    singleOf(::UpdateBookMetadataUseCaseImpl).bind<UpdateBookMetadataUseCase>()

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
