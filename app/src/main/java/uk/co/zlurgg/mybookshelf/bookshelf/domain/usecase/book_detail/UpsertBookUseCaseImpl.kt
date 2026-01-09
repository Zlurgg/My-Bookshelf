package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class UpsertBookUseCaseImpl(
    private val bookRepository: BookRepository
) : UpsertBookUseCase {

    companion object {
        private const val TAG = "UpsertBook"
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun execute(book: Book): Result<Unit, DataError.Local> {
        return try {
            // Check if book already exists to preserve personal metadata
            val existingBook = bookRepository.getBookById(book.id)

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

            bookRepository.upsertBook(bookToUpsert)
            Result.Success(Unit)
        } catch (e: Exception) {
            val error = ErrorMapper.mapExceptionToDataError(e) as? DataError.Local ?: DataError.Local.UNKNOWN
            Timber.tag(TAG).e(e, "Upsert book failed - Mapped to: %s", error)
            Result.Error(error)
        }
    }
}
