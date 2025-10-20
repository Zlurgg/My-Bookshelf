package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookshelf

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * UseCase for updating the tidy mode preference of a specific bookshelf.
 * This persists the user's choice between tidy and messy view modes.
 */
interface UpdateShelfTidyModeUseCase {
    suspend fun execute(shelfId: String, isTidyMode: Boolean): Result<Unit, DataError>
}
