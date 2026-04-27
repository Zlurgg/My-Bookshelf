package uk.co.zlurgg.mybookshelf.bookclub.domain.usecase

import kotlinx.coroutines.flow.first
import timber.log.Timber
import uk.co.zlurgg.mybookshelf.bookclub.domain.repository.BookClubRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Implementation of CreateBookClubUseCase.
 *
 * Delegates to BookClubRepository to handle the actual club creation,
 * Firestore operations, and local database updates.
 */
class CreateBookClubUseCaseImpl(
    private val bookClubRepository: BookClubRepository
) : CreateBookClubUseCase {

    companion object {
        private const val TAG = "CreateBookClub"
        const val MAX_BOOK_CLUBS = 5
    }

    override suspend operator fun invoke(shelfId: String): Result<String, DataError.Sync> {
        Timber.tag(TAG).d("Creating book club from shelf: %s", shelfId)

        // Check book club limit before creating
        val currentBookClubs = bookClubRepository.observeMyBookClubs().first()
        if (currentBookClubs.size >= MAX_BOOK_CLUBS) {
            Timber.tag(TAG).w("User has reached max book clubs limit: %d", MAX_BOOK_CLUBS)
            return Result.Error(DataError.Sync.MAX_BOOK_CLUBS_REACHED)
        }

        val result = bookClubRepository.createBookClub(shelfId)

        when (result) {
            is Result.Success -> {
                Timber.tag(TAG).d("Book club created successfully: %s", result.data)
            }
            is Result.Error -> {
                Timber.tag(TAG).e("Failed to create book club: %s", result.error)
            }
        }

        return result
    }
}
