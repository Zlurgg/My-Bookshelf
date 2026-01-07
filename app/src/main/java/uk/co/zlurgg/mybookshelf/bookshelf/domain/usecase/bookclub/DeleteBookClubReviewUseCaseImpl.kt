package uk.co.zlurgg.mybookshelf.bookshelf.domain.usecase.bookclub

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.bookshelf.domain.repository.BookClubRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Implementation of DeleteBookClubReviewUseCase.
 */
class DeleteBookClubReviewUseCaseImpl(
    private val bookClubRepository: BookClubRepository
) : DeleteBookClubReviewUseCase {

    override suspend fun invoke(
        clubCode: String,
        bookId: String
    ): Result<Unit, DataError.Sync> {
        Timber.tag(TAG).d("Deleting review for book %s in club %s", bookId, clubCode)

        return when (val result = bookClubRepository.deleteBookReview(clubCode, bookId)) {
            is Result.Success -> {
                Timber.tag(TAG).d("Review deleted successfully")
                Result.Success(Unit)
            }
            is Result.Error -> {
                Timber.tag(TAG).e("Failed to delete review: %s", result.error)
                Result.Error(result.error)
            }
        }
    }

    companion object {
        private const val TAG = "DeleteClubReview"
    }
}
