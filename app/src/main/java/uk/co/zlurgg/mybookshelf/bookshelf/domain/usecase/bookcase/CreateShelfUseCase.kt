package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase

import uk.co.zlurgg.mybookshelf.book.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.book.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * UseCase for creating a new bookshelf with the given name and style.
 * Handles position calculation and shelf creation logic.
 */
interface CreateShelfUseCase {
    suspend operator fun invoke(
        name: String,
        style: ShelfStyle,
        existingShelves: List<Bookshelf>
    ): Result<Bookshelf, DataError.Local>
}
