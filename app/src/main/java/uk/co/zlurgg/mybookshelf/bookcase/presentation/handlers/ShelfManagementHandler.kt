package uk.co.zlurgg.mybookshelf.bookcase.presentation.handlers

import uk.co.zlurgg.mybookshelf.book.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.book.domain.service.ClubOperations
import uk.co.zlurgg.mybookshelf.bookcase.domain.usecase.BookcaseUseCases
import uk.co.zlurgg.mybookshelf.welcome.domain.usecase.HandleTutorialAccessUseCase
import uk.co.zlurgg.mybookshelf.welcome.domain.usecase.TutorialAccessResult
import uk.co.zlurgg.mybookshelf.book.domain.util.BookshelfConstants
import uk.co.zlurgg.mybookshelf.book.domain.util.ShelfStyle
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

// Depends on welcome/ for tutorial shelf access (help icon → tutorial restore).
// This cross-feature dependency is intentional and injected via Koin.
class ShelfManagementHandler(
    private val bookcaseUseCases: BookcaseUseCases,
    private val handleTutorialAccess: HandleTutorialAccessUseCase,
    private val clubOperations: ClubOperations
) {
    suspend fun reorderShelf(
        shelf: Bookshelf,
        newPosition: Int,
        currentShelves: List<Bookshelf>
    ): Result<List<Bookshelf>, DataError> {
        return bookcaseUseCases.reorderShelves(shelf, newPosition, currentShelves)
    }

    suspend fun renameShelf(shelfId: String, newName: String): Result<Unit, DataError> {
        when (val validationResult = BookshelfConstants.validateShelfName(newName)) {
            is Result.Error -> return validationResult
            is Result.Success -> {}
        }

        // Check if this is a book club shelf - if so, use book club rename
        val shelfResult = bookcaseUseCases.getShelfById(shelfId)
        if (shelfResult is Result.Success) {
            val shelf = shelfResult.data
            if (shelf != null && shelf.isBookClub && !shelf.clubCode.isNullOrEmpty()) {
                return clubOperations.renameBookClub(shelf.clubCode, newName)
            }
        }

        return bookcaseUseCases.renameShelf(shelfId, newName)
    }

    suspend fun updateShelfStyle(shelfId: String, newStyle: ShelfStyle): Result<Unit, DataError> {
        return bookcaseUseCases.updateShelfStyle(shelfId, newStyle)
    }

    suspend fun accessTutorialShelf(): Result<TutorialAccessResult, DataError> {
        return handleTutorialAccess()
    }
}
