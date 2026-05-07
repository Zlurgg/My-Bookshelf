package uk.co.zlurgg.mybookshelf.bookclub.domain.usecase

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * UseCase for creating a Book Club, either directly or from an existing shelf.
 *
 * When sourceShelfId is provided, books are copied from the source shelf.
 * When null, an empty club is created directly.
 * Returns the generated club code which can be shared with other users.
 */
interface CreateBookClubUseCase {
    suspend operator fun invoke(
        name: String,
        shelfStyle: String,
        sourceShelfId: String? = null,
    ): Result<String, DataError.Sync>
}
