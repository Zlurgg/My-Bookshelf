package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Clears all local data belonging to a specific user.
 * Used during sign-out to prevent data leakage between accounts.
 */
interface ClearUserDataUseCase {
    suspend operator fun invoke(userId: String): Result<Int, DataError.Local>
}
