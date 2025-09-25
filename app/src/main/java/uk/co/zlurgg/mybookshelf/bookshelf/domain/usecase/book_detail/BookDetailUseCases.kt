package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail

/**
 * Facade aggregating all book detail related UseCases.
 * Simplifies ViewModel constructor dependencies and provides clean separation of concerns.
 */
class BookDetailUseCases(
    val getBookDetails: GetBookDetailsUseCase,
    val addBookToShelf: AddBookToShelfUseCase,
    val removeBookFromShelf: RemoveBookFromShelfUseCase
)