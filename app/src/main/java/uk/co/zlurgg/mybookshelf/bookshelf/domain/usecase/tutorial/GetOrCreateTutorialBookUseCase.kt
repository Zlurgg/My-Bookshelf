package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.tutorial

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * UseCase to get existing tutorial book or create it if it doesn't exist.
 * Returns the book ID.
 */
interface GetOrCreateTutorialBookUseCase {
    suspend operator fun invoke(tutorialShelfId: String): Result<String, DataError.Local>
}
