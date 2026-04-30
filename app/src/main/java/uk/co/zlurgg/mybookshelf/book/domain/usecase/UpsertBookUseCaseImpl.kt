package uk.co.zlurgg.mybookshelf.book.domain.usecase

import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * WARNING: Does not trigger sync. This is intentional — UpsertBook is a building-block
 * used by parent use cases (AddBookToShelf, etc.) that handle sync themselves.
 * If you call this directly for a user-facing mutation, you must trigger sync separately.
 */
class UpsertBookUseCaseImpl(
    private val bookRepository: BookRepository
) : UpsertBookUseCase {

    override suspend operator fun invoke(book: Book): Result<Unit, DataError.Local> {
        // Check if book already exists to preserve personal metadata
        val existingBook = when (val getResult = bookRepository.getBookById(book.id)) {
            is Result.Success -> getResult.data
            is Result.Error -> return getResult
        }

        val bookToUpsert = if (existingBook != null) {
            // Book exists - preserve personal metadata, update everything else from new data
            book.copy(
                readingStatus = existingBook.readingStatus,
                personalRating = existingBook.personalRating,
                personalNotes = existingBook.personalNotes,
                dateAdded = existingBook.dateAdded,
                purchaseDate = existingBook.purchaseDate,
                purchased = existingBook.purchased
            )
        } else {
            // New book - use as-is
            book
        }

        return bookRepository.upsertBook(bookToUpsert)
    }
}
