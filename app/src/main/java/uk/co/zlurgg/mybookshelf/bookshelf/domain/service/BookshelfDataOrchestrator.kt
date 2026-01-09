package uk.co.zlurgg.mybookshelf.bookshelf.domain.service

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Domain service interface for orchestrating data operations across repositories.
 * Coordinates complex workflows involving multiple data sources.
 */
interface BookshelfDataOrchestrator {
    suspend fun loadShelfForExport(shelfId: String): Result<Bookshelf, DataError.Local>
    suspend fun importShelfToDatabase(shelf: Bookshelf): Result<Unit, DataError.Local>
}
