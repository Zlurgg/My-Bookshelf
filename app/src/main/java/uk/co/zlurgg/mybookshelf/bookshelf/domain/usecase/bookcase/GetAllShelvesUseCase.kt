package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase

import kotlinx.coroutines.flow.Flow
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf

/**
 * UseCase for getting all bookshelves with their book counts.
 * Provides reactive updates when shelves or books change.
 * Returns shelves with populated book counts for display purposes.
 */
interface GetAllShelvesUseCase {
    suspend fun execute(): Flow<List<Bookshelf>>
}