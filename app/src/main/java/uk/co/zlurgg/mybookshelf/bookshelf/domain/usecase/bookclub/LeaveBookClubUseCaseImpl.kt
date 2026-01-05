package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookClubRepository
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookcaseRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Implementation of LeaveBookClubUseCase.
 *
 * Coordinates the leave flow by:
 * 1. Getting the shelf to find the club code
 * 2. Delegating the actual leave operation to the repository
 */
class LeaveBookClubUseCaseImpl(
    private val bookcaseRepository: BookcaseRepository,
    private val bookClubRepository: BookClubRepository
) : LeaveBookClubUseCase {

    override suspend fun invoke(shelfId: String): Result<Unit, DataError.Sync> {
        Timber.tag(TAG).d("Attempting to leave book club for shelf: %s", shelfId)

        // 1. Get the shelf to find the club code
        val shelf = bookcaseRepository.getShelfById(shelfId)
        if (shelf == null) {
            Timber.tag(TAG).e("Shelf not found: %s", shelfId)
            return Result.Error(DataError.Sync.DOCUMENT_NOT_FOUND)
        }

        if (!shelf.isBookClub) {
            Timber.tag(TAG).e("Shelf is not a book club: %s", shelfId)
            return Result.Error(DataError.Sync.CLUB_NOT_FOUND)
        }

        val clubCode = shelf.clubCode
        if (clubCode == null) {
            Timber.tag(TAG).e("Book club shelf has no club code: %s", shelfId)
            return Result.Error(DataError.Sync.CLUB_NOT_FOUND)
        }

        // 2. Delegate to repository
        val leaveResult = bookClubRepository.leaveBookClub(clubCode)

        return when (leaveResult) {
            is Result.Success -> {
                Timber.tag(TAG).d("Successfully left book club: %s", clubCode)
                Result.Success(Unit)
            }
            is Result.Error -> {
                Timber.tag(TAG).e("Failed to leave club: %s", leaveResult.error)
                Result.Error(leaveResult.error)
            }
        }
    }

    companion object {
        private const val TAG = "LeaveBookClub"
    }
}
