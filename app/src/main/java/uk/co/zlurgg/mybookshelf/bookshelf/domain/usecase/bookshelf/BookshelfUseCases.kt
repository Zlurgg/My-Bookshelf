package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf

import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookdetail.AddBookToShelfUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookdetail.RemoveBookFromShelfUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookdetail.UpsertBookUseCase

/**
 * Facade aggregating all bookshelf related UseCases.
 * Simplifies ViewModel constructor dependencies and provides clean separation of concerns.
 */
class BookshelfUseCases(
    val searchBooks: SearchBooksUseCase,
    val getShelfBooks: GetShelfBooksUseCase,
    val addBookToShelf: AddBookToShelfUseCase,
    val removeBookFromShelf: RemoveBookFromShelfUseCase,
    val upsertBook: UpsertBookUseCase,
    val shareBookshelf: ShareBookshelfUseCase,
    val updateShelfTidyMode: UpdateShelfTidyModeUseCase
)
