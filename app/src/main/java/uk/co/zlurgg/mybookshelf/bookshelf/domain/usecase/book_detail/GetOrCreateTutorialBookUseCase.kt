package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.book_detail

import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * UseCase to get existing tutorial book or create it if it doesn't exist.
 * Returns the book ID.
 */
interface GetOrCreateTutorialBookUseCase {
    suspend fun execute(tutorialShelfId: String): Result<String, DataError.Local>
}
