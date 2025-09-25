package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase

import kotlinx.coroutines.flow.Flow
import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookcase

/**
 * UseCase for getting the complete bookcase with all shelves and their book counts.
 * Provides reactive updates when shelves or books change.
 * Returns the bookcase with populated book counts for display purposes.
 */
interface GetAllShelvesUseCase {
    suspend fun execute(): Flow<Bookcase>
}