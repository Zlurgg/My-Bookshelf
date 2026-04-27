package uk.co.zlurgg.mybookshelf.bookclub.domain.usecase

import timber.log.Timber
import uk.co.zlurgg.mybookshelf.bookclub.domain.model.BookClubReview
import uk.co.zlurgg.mybookshelf.bookclub.domain.repository.BookClubRepository
import uk.co.zlurgg.mybookshelf.core.domain.error.DataError
import uk.co.zlurgg.mybookshelf.core.domain.result.Result

/**
 * Implementation of GetBookClubReviewsUseCase.
 */
class GetBookClubReviewsUseCaseImpl(
    private val bookClubRepository: BookClubRepository
) : GetBookClubReviewsUseCase {

    override suspend fun invoke(
        clubCode: String,
        bookId: String
    ): Result<List<BookClubReview>, DataError.Sync> {
        Timber.tag(TAG).d("Getting reviews for book %s in club %s", bookId, clubCode)

        return when (val result = bookClubRepository.getBookReviews(clubCode, bookId)) {
            is Result.Success -> {
                Timber.tag(TAG).d("Found %d reviews", result.data.size)
                Result.Success(result.data)
            }
            is Result.Error -> {
                Timber.tag(TAG).e("Failed to get reviews: %s", result.error)
                Result.Error(result.error)
            }
        }
    }

    companion object {
        private const val TAG = "GetClubReviews"
    }
}
