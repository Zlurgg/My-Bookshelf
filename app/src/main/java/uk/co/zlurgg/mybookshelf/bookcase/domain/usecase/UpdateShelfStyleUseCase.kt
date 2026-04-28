package uk.co.zlurgg.mybookshelf.bookcase.domain.usecase

import uk.co.zlurgg.mybookshelf.book.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Use case for updating a bookshelf's style.
 * Updates the shelf material/style in the repository.
 */
interface UpdateShelfStyleUseCase {
    /**
     * Updates a bookshelf's style.
     *
     * @param shelfId ID of the shelf to update
     * @param newStyle New style for the shelf
     * @return Result.Success if updated, Result.Error if shelf not found
     */
    suspend operator fun invoke(shelfId: String, newStyle: ShelfStyle): Result<Unit, DataError.Local>
}
