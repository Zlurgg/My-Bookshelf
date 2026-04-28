package uk.co.zlurgg.mybookshelf.bookcase.domain.usecase

import uk.co.zlurgg.mybookshelf.book.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * UseCase for reordering bookshelves by moving one shelf to a new position.
 * Handles position recalculation and batch updates for affected shelves.
 */
interface ReorderShelvesUseCase {
    suspend operator fun invoke(
        shelfToMove: Bookshelf,
        newPosition: Int,
        currentShelves: List<Bookshelf>
    ): Result<List<Bookshelf>, DataError.Local>
}
