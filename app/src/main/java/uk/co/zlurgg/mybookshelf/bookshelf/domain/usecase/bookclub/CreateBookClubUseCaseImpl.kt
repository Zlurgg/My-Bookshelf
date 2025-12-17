package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookClubRepository
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

    override suspend fun execute(shelfId: String): Result<String, DataError.Sync> {
        Timber.tag(TAG).d("Creating book club from shelf: %s", shelfId)

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
