package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookshelfRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.service.BookColorGenerator
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Implementation of AddBookToShelfUseCase that orchestrates book persistence and shelf association.
 * Follows Clean Architecture by coordinating between domain repositories.
 * Generates spine color when book is first added to any shelf for optimal performance.
 */
class AddBookToShelfUseCaseImpl(
    private val bookRepository: BookRepository,
    private val bookshelfRepository: BookshelfRepository
) : AddBookToShelfUseCase {

    override suspend fun execute(book: Book, shelfId: String): Result<Unit, DataError.Local> {
        return try {
            // Check if book already exists to preserve personal metadata
            val existingBook = bookRepository.getBookById(book.id)

            val bookToUpsert = if (existingBook != null) {
                // Book exists - preserve ALL existing data including spine color
                book.copy(
                    spineColor = existingBook.spineColor,
                    readingStatus = existingBook.readingStatus,
                    personalRating = existingBook.personalRating,
                    personalNotes = existingBook.personalNotes,
                    dateAdded = existingBook.dateAdded,
                    purchaseDate = existingBook.purchaseDate,
                    purchased = existingBook.purchased
                )
            } else {
                // New book - generate spine color now (not during search)
                book.copy(spineColor = BookColorGenerator.generateSpineColor())
            }

            // Persist the book (with preserved metadata if it existed)
            bookRepository.upsertBook(bookToUpsert)

            // Then create the shelf association
            bookshelfRepository.addBookToShelf(shelfId, book.id)

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(
                ErrorMapper.mapExceptionToDataError(e) as? DataError.Local
                    ?: DataError.Local.UNKNOWN
            )
        }
    }
}