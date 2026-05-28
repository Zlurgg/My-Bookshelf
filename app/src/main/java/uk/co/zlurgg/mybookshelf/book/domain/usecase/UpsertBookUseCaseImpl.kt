package uk.co.zlurgg.mybookshelf.book.domain.usecase

import uk.co.zlurgg.mybookshelf.book.domain.model.Book
import uk.co.zlurgg.mybookshelf.book.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result
import uk.co.zlurgg.mybookshelf.core.domain.service.TimeProvider

class UpsertBookUseCaseImpl(
    private val bookRepository: BookRepository,
    private val timeProvider: TimeProvider,
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
            // New book - default dateAdded at insert time. The column-scoped
            // update use cases no longer copy + re-insert the row, so this is
            // the only place left that can set it.
            book.copy(dateAdded = book.dateAdded ?: timeProvider.currentTimeMillis())
        }

        return bookRepository.upsertBook(bookToUpsert)
    }
}
