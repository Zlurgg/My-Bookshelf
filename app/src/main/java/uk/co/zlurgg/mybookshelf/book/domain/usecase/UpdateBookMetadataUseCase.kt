package uk.co.zlurgg.mybookshelf.book.domain.usecase

import uk.co.zlurgg.mybookshelf.book.domain.model.ReadingStatus
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Use case for updating a book's personal metadata.
 *
 * Personal metadata includes reading status, personal rating, notes, and dates.
 * This data is NOT exported/shared - it stays local to the user's device.
 */
interface UpdateBookMetadataUseCase {
    suspend operator fun invoke(
        bookId: String,
        readingStatus: ReadingStatus? = null,
        personalRating: Float? = null,
        personalNotes: String? = null,
        purchaseDate: Long? = null
    ): Result<Unit, DataError>
}
