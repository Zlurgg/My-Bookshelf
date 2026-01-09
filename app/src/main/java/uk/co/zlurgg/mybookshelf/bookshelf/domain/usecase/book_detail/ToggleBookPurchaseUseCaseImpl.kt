package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Book
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.error.ErrorMapper
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class ToggleBookPurchaseUseCaseImpl(
    private val bookRepository: BookRepository
) : ToggleBookPurchaseUseCase {

    override suspend fun execute(book: Book, purchased: Boolean): Result<Book, DataError.Local> {
        return try {
            // Check if book already exists to preserve personal metadata
            val existingBook = bookRepository.getBookById(book.id)

            val updatedBook = if (existingBook != null) {
                // Book exists - preserve personal metadata, update purchased status and other API data
                book.copy(
                    purchased = purchased,
                    readingStatus = existingBook.readingStatus,
                    personalRating = existingBook.personalRating,
                    personalNotes = existingBook.personalNotes,
                    dateAdded = existingBook.dateAdded,
                    purchaseDate = existingBook.purchaseDate
                )
            } else {
                // New book - use as-is with purchased status
                book.copy(purchased = purchased)
            }

            bookRepository.upsertBook(updatedBook)
            Result.Success(updatedBook)
        } catch (e: Exception) {
            Result.Error(ErrorMapper.mapExceptionToDataError(e) as? DataError.Local ?: DataError.Local.UNKNOWN)
        }
    }
}
