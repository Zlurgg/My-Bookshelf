package uk.co.zlurgg.mybookshelf.book.domain.usecase

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.auth.domain.service.CurrentUserProvider
import uk.co.zlurgg.mybookshelf.book.domain.model.Bookshelf
import uk.co.zlurgg.mybookshelf.book.domain.service.ClubOperations
import uk.co.zlurgg.mybookshelf.book.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.book.domain.repository.BookshelfRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Implementation of RemoveBookFromShelfUseCase that handles removing book-shelf associations.
 * The book remains in the database; only the relationship to the shelf is removed.
 * Also syncs to Firestore if the shelf is a book club.
 */
class RemoveBookFromShelfUseCaseImpl(
    private val bookshelfRepository: BookshelfRepository,
    private val bookcaseRepository: BookcaseRepository,
    private val clubOperations: ClubOperations,
    private val currentUserProvider: CurrentUserProvider,
) : RemoveBookFromShelfUseCase {

    override suspend operator fun invoke(bookId: String, shelfId: String): Result<Unit, DataError.Local> {
        // Check if this is a book club shelf BEFORE removing
        val shelf = when (val getResult = bookcaseRepository.getShelfById(shelfId)) {
            is Result.Success -> getResult.data
            is Result.Error -> return getResult
        }

        // Permission check for club shelves
        if (shelf != null && shelf.isBookClub) {
            val permissionError = checkClubPermission(shelf, bookId, shelfId)
            if (permissionError != null) return permissionError
        }

        // Remove the book-shelf association
        when (val removeResult = bookshelfRepository.removeBookFromShelf(shelfId, bookId)) {
            is Result.Success -> { /* continue */ }
            is Result.Error -> return removeResult
        }

        // If this is a book club shelf, also remove from Firestore club collection
        shelf?.takeIf { it.isBookClub }?.clubCode?.takeIf { it.isNotEmpty() }?.let { code ->
            Timber.tag(TAG).d("Removing book %s from book club %s", bookId, code)
            val syncResult = clubOperations.removeBookFromClub(code, bookId)
            if (syncResult is Result.Error) {
                Timber.tag(TAG).w("Failed to remove book from club: %s", syncResult.error)
                // Don't fail the whole operation - local remove succeeded
            }
        }

        return Result.Success(Unit)
    }

    private suspend fun checkClubPermission(
        shelf: Bookshelf,
        bookId: String,
        shelfId: String,
    ): Result.Error<DataError.Local>? {
        val currentUserId = currentUserProvider.getCurrentUserId()
        if (currentUserId == null) {
            Timber.tag(TAG).w(
                "Unauthenticated user attempted to remove book %s from club shelf %s",
                bookId,
                shelfId
            )
            return Result.Error(DataError.Local.PERMISSION_DENIED)
        }
        if (shelf.clubCreatorId == currentUserId) return null

        val addedBy = when (val result = bookshelfRepository.getAddedByUserId(shelfId, bookId)) {
            is Result.Success -> result.data
            is Result.Error -> null
        }
        if (addedBy == currentUserId) return null

        Timber.tag(TAG).w(
            "Non-owner/non-adder attempted to remove book %s from club shelf %s",
            bookId,
            shelfId
        )
        return Result.Error(DataError.Local.PERMISSION_DENIED)
    }

    companion object {
        private const val TAG = "RemoveBookFromShelf"
    }
}
