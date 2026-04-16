package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookdetail

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

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
