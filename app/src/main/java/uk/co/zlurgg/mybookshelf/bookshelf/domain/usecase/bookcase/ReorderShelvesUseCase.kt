package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.core.domain.DataError
import uk.co.zlurgg.mybookshelf.core.domain.Result

/**
 * UseCase for reordering bookshelves by moving one shelf to a new position.
 * Handles position recalculation and batch updates for affected shelves.
 */
interface ReorderShelvesUseCase {
    suspend fun execute(
        shelfToMove: Bookshelf,
        newPosition: Int,
        currentShelves: List<Bookshelf>
    ): Result<List<Bookshelf>, DataError.Local>
}