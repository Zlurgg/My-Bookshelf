package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase

import uk.co.zlurgg.mybookshelf.book.domain.usecase.AddBookToShelfUseCase
import uk.co.zlurgg.mybookshelf.book.domain.usecase.RemoveBookFromShelfUseCase
import uk.co.zlurgg.mybookshelf.book.domain.usecase.SearchBooksUseCase
import uk.co.zlurgg.mybookshelf.book.domain.usecase.UpsertBookUseCase

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
    val updateShelfTidyMode: UpdateShelfTidyModeUseCase
)
