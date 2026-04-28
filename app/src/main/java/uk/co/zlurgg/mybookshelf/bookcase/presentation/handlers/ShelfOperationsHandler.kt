package uk.co.zlurgg.mybookshelf.bookcase.presentation.handlers

import uk.co.zlurgg.mybookshelf.book.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookcase.domain.usecase.BookcaseUseCases
import uk.co.zlurgg.mybookshelf.book.domain.util.BookshelfConstants
import uk.co.zlurgg.mybookshelf.book.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class ShelfOperationsHandler(
    private val bookcaseUseCases: BookcaseUseCases
) {
    companion object {
        const val MAX_PERSONAL_SHELVES = 20
    }

    suspend fun createShelf(
        name: String,
        style: ShelfStyle,
        existingShelves: List<Bookshelf>
    ): Result<Bookshelf, DataError> {
        // Check personal shelf limit (exclude book clubs and tutorial shelf)
        val personalShelfCount = existingShelves.count {
            !it.isBookClub && it.name != BookshelfConstants.TUTORIAL_SHELF_NAME
        }
        if (personalShelfCount >= MAX_PERSONAL_SHELVES) {
            return Result.Error(DataError.Local.MAX_SHELVES_REACHED)
        }

        when (val validationResult = BookshelfConstants.validateShelfName(name)) {
            is Result.Error -> return validationResult
            is Result.Success -> {}
        }

        return bookcaseUseCases.createShelf(name, style, existingShelves)
    }

    suspend fun deleteShelf(shelfId: String): Result<Unit, DataError> {
        return bookcaseUseCases.deleteShelf(shelfId)
    }

    suspend fun restoreShelf(shelf: Bookshelf): Result<Unit, DataError> {
        return bookcaseUseCases.deleteShelf.restore(shelf)
    }

    suspend fun duplicateShelf(shelfId: String): Result<Bookshelf, DataError> {
        return bookcaseUseCases.duplicateShelf(shelfId)
    }
}
