package uk.co.zlurgg.mybookshelf.bookdetail.domain.usecase

import uk.co.zlurgg.mybookshelf.book.domain.usecase.AddBookToShelfUseCase
import uk.co.zlurgg.mybookshelf.book.domain.usecase.RemoveBookFromShelfUseCase
import uk.co.zlurgg.mybookshelf.book.domain.usecase.UpsertBookUseCase

/**
 * Facade aggregating all book detail related UseCases.
 * Simplifies ViewModel constructor dependencies and provides clean separation of concerns.
 */
class BookDetailUseCases(
    val getBookDetails: GetBookDetailsUseCase,
    val addBookToShelf: AddBookToShelfUseCase,
    val removeBookFromShelf: RemoveBookFromShelfUseCase,
    val upsertBook: UpsertBookUseCase,
    val toggleBookPurchase: ToggleBookPurchaseUseCase,
    val updateBookMetadata: UpdateBookMetadataUseCase
)
