package uk.co.zlurgg.mybookshelf.bookshelf.presentation.bookcase.handlers

import uk.co.zlurgg.mybookshelf.bookshelf.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookClubRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookcase.BookcaseUseCases
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.tutorial.HandleTutorialAccessUseCase
import uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.tutorial.TutorialAccessResult
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.BookshelfConstants
import uk.co.zlurgg.mybookshelf.bookshelf.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

class ShelfManagementHandler(
    private val bookcaseUseCases: BookcaseUseCases,
    private val handleTutorialAccess: HandleTutorialAccessUseCase,
    private val bookClubRepository: BookClubRepository
) {
    suspend fun reorderShelf(
        shelf: Bookshelf,
        newPosition: Int,
        currentShelves: List<Bookshelf>
    ): Result<List<Bookshelf>, DataError> {
        return bookcaseUseCases.reorderShelves.execute(shelf, newPosition, currentShelves)
    }

    suspend fun renameShelf(shelfId: String, newName: String): Result<Unit, DataError> {
        when (val validationResult = BookshelfConstants.validateShelfName(newName)) {
            is Result.Error -> return validationResult
            is Result.Success -> {}
        }

        // Check if this is a book club shelf - if so, use book club rename
        val shelfResult = bookcaseUseCases.getShelfById.execute(shelfId)
        if (shelfResult is Result.Success) {
            val shelf = shelfResult.data
            if (shelf != null && shelf.isBookClub && !shelf.clubCode.isNullOrEmpty()) {
                return bookClubRepository.renameBookClub(shelf.clubCode, newName)
            }
        }

        return bookcaseUseCases.renameShelf.execute(shelfId, newName)
    }

    suspend fun updateShelfStyle(shelfId: String, newStyle: ShelfStyle): Result<Unit, DataError> {
        return bookcaseUseCases.updateShelfStyle.execute(shelfId, newStyle)
    }

    suspend fun accessTutorialShelf(): Result<TutorialAccessResult, DataError> {
        return handleTutorialAccess.execute()
    }
}
