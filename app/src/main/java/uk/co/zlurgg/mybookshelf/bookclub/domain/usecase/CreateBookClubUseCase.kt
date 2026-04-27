package uk.co.zlurgg.mybookshelf.bookclub.domain.usecase

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * UseCase for creating a Book Club from an existing shelf.
 *
 * This creates a new collaborative book club in Firestore and uploads
 * all books from the source shelf. Returns the generated club code
 * which can be shared with other users.
 */
interface CreateBookClubUseCase {
    /**
     * Creates a book club from the given shelf.
     *
     * @param shelfId The ID of the shelf to create a book club from
     * @return The generated club code on success
     */
    suspend operator fun invoke(shelfId: String): Result<String, DataError.Sync>
}
