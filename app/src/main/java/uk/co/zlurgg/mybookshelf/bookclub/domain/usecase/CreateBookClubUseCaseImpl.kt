package uk.co.zlurgg.mybookshelf.bookclub.domain.usecase

import kotlinx.coroutines.flow.first
import timber.log.Timber
import uk.co.zlurgg.mybookshelf.bookclub.domain.model.BookClub
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
    }

    override suspend operator fun invoke(
        name: String,
        shelfStyle: String,
        sourceShelfId: String?,
    ): Result<String, DataError.Sync> {
        Timber.tag(TAG).d("Creating book club: %s (source: %s)", name, sourceShelfId)

        // Check book club limit before creating
        val currentBookClubs = bookClubRepository.observeMyBookClubs().first()
        if (currentBookClubs.size >= BookClub.MAX_BOOK_CLUBS) {
            Timber.tag(TAG).w("User has reached max book clubs limit: %d", BookClub.MAX_BOOK_CLUBS)
            return Result.Error(DataError.Sync.MAX_BOOK_CLUBS_REACHED)
        }

        val result = bookClubRepository.createBookClub(name, shelfStyle, sourceShelfId)

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
