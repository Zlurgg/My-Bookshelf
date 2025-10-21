package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase.handlers

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.BookcaseUseCases
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.BookshelfConstants
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class ShelfOperationsHandler(
    private val bookcaseUseCases: BookcaseUseCases
) {
    suspend fun createShelf(
        name: String,
        style: ShelfStyle,
        existingShelves: List<Bookshelf>
    ): Result<Bookshelf, DataError> {
        when (val validationResult = BookshelfConstants.validateShelfName(name)) {
            is Result.Error -> return validationResult
            is Result.Success -> {}
        }

        return bookcaseUseCases.createShelf.execute(name, style, existingShelves)
    }

    suspend fun deleteShelf(shelfId: String): Result<Unit, DataError> {
        return bookcaseUseCases.deleteShelf.execute(shelfId)
    }

    suspend fun restoreShelf(shelf: Bookshelf): Result<Unit, DataError> {
        return bookcaseUseCases.deleteShelf.restore(shelf)
    }

    suspend fun shareShelf(shelfId: String): Result<Unit, DataError> {
        return bookcaseUseCases.shareShelf.execute(shelfId)
    }

    suspend fun duplicateShelf(shelfId: String): Result<Bookshelf, DataError> {
        return bookcaseUseCases.duplicateShelf.execute(shelfId)
    }
}
