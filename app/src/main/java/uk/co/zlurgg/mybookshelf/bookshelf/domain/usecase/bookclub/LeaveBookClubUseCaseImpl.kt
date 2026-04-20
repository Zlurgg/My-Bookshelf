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

        val clubCode = when (val codeResult = getClubCodeForShelf(shelfId)) {
            is Result.Success -> codeResult.data
            is Result.Error -> return codeResult
        }

        return when (val leaveResult = bookClubRepository.leaveBookClub(clubCode)) {
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

    private suspend fun getClubCodeForShelf(shelfId: String): Result<String, DataError.Sync> {
        val shelf = when (val getResult = bookcaseRepository.getShelfById(shelfId)) {
            is Result.Success -> getResult.data
            is Result.Error -> {
                Timber.tag(TAG).e("Failed to get shelf: %s", getResult.error)
                return Result.Error(DataError.Sync.DOCUMENT_NOT_FOUND)
            }
        }

        if (shelf == null) {
            Timber.tag(TAG).e("Shelf not found: %s", shelfId)
            return Result.Error(DataError.Sync.DOCUMENT_NOT_FOUND)
        }

        if (!shelf.isBookClub || shelf.clubCode == null) {
            Timber.tag(TAG).e("Shelf is not a book club: %s", shelfId)
            return Result.Error(DataError.Sync.CLUB_NOT_FOUND)
        }

        return Result.Success(shelf.clubCode)
    }

    companion object {
        private const val TAG = "LeaveBookClub"
    }
}
