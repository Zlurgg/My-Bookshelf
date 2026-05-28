package uk.co.zlurgg.mybookshelf.bookdetail.domain.usecase

import uk.co.zlurgg.mybookshelf.book.domain.model.ReadingStatus
import uk.co.zlurgg.mybookshelf.book.domain.repository.BookRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Implementation of UpdateBookMetadataUseCase.
 *
 * Validates input and delegates to a single column-scoped repository call so
 * that an edit never resurrects a previewed book or clobbers a parallel write
 * to another column. The repository wraps the DAO call in ErrorMapper; this
 * use case is pure delegation and does not re-wrap.
 */
class UpdateBookMetadataUseCaseImpl(
    private val bookRepository: BookRepository,
) : UpdateBookMetadataUseCase {

    companion object {
        private const val MAX_RATING = 5.0f
        private const val MAX_NOTES_LENGTH = 5000
    }

    override suspend operator fun invoke(
        bookId: String,
        readingStatus: ReadingStatus?,
        personalRating: Float?,
        personalNotes: String?,
    ): Result<Unit, DataError> {
        if (personalRating != null && personalRating !in 0f..MAX_RATING) {
            return Result.Error(DataError.Validation.INVALID_FORMAT)
        }
        if (personalNotes != null && personalNotes.length > MAX_NOTES_LENGTH) {
            return Result.Error(DataError.Validation.TOO_LONG)
        }
        return bookRepository.updatePersonalMetadata(
            bookId = bookId,
            readingStatus = readingStatus?.name,
            personalRating = personalRating,
            personalNotes = personalNotes,
        )
    }
}
