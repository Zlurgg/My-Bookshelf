package uk.co.zlurgg.mybookshelf.bookdetail.domain.usecase

import uk.co.zlurgg.mybookshelf.book.domain.model.ReadingStatus
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Use case for updating a book's personal metadata.
 *
 * Personal metadata includes reading status, personal rating, and notes.
 * This data is NOT exported/shared - it stays local to the user's device.
 *
 * Null parameters mean "leave this field alone." The implementation routes each
 * non-null field through a column-scoped UPDATE rather than a full-row upsert,
 * so a missing row is a silent no-op (previewed books cannot be promoted into
 * the library by an edit).
 */
interface UpdateBookMetadataUseCase {
    suspend operator fun invoke(
        bookId: String,
        readingStatus: ReadingStatus? = null,
        personalRating: Float? = null,
        personalNotes: String? = null,
    ): Result<Unit, DataError>
}
